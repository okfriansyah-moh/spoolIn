import { createHmac, randomUUID, timingSafeEqual } from 'node:crypto';
import {
  closeSync,
  constants,
  fstatSync,
  openSync,
  readFileSync,
} from 'node:fs';
import { isAbsolute } from 'node:path';
import { inspect } from 'node:util';

export type UiBinding = Readonly<{
  connector: string;
  client: 'CHATGPT' | 'CLAUDE';
  session: string;
  proposal: string;
  revision: number;
  hash: string;
  method:
    | '/spoolin.v1.McpGatewayService/ApproveMovie'
    | '/spoolin.v1.McpGatewayService/RejectMovie';
}>;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const KEY_ID = /^[A-Za-z0-9_-]{1,64}$/;
const FIELDS = [
  'client',
  'connector',
  'hash',
  'method',
  'proposal',
  'revision',
  'session',
];
const METHODS = new Set([
  '/spoolin.v1.McpGatewayService/ApproveMovie',
  '/spoolin.v1.McpGatewayService/RejectMovie',
]);

function protectedKey(path: string): Buffer {
  if (!isAbsolute(path)) throw new Error('Protected UI assertion key required');
  const descriptor = openSync(path, constants.O_RDONLY | constants.O_NOFOLLOW);
  try {
    const metadata = fstatSync(descriptor);
    if (
      !metadata.isFile() ||
      metadata.size !== 32 ||
      (metadata.mode & 0o777) !== 0o600
    ) {
      throw new Error('Protected32-byte UI assertion key required');
    }
    const key = readFileSync(descriptor);
    if (key.length !== 32) {
      key.fill(0);
      throw new Error('Invalid UI assertion key');
    }
    return key;
  } finally {
    closeSync(descriptor);
  }
}

function validate(binding: UiBinding): void {
  if (!binding || Object.keys(binding).sort().join(',') !== FIELDS.join(','))
    throw new Error('Invalid private UI binding');
  validateIdentity(binding);
  validateContent(binding);
}

function validateIdentity(binding: UiBinding): void {
  if (
    ![binding.connector, binding.session, binding.proposal].every(
      (value) => typeof value === 'string' && UUID.test(value),
    ) ||
    !['CHATGPT', 'CLAUDE'].includes(binding.client) ||
    !METHODS.has(binding.method)
  )
    throw new Error('Invalid private UI binding');
}

function validateContent(binding: UiBinding): void {
  if (
    !Number.isSafeInteger(binding.revision) ||
    binding.revision < 1 ||
    binding.revision > 2147483647 ||
    typeof binding.hash !== 'string'
  )
    throw new Error('Invalid private UI binding');
  const hash = Buffer.from(binding.hash, 'base64');
  if (hash.length !== 32 || hash.toString('base64') !== binding.hash)
    throw new Error('Invalid private UI binding');
}

/** Private RPC metadata only. The caller must first authenticate the supported host and its live bound session. */
export class UiAssertionSigner {
  private key: Buffer;
  private closed = false;
  private readonly seen = new Set<string>();

  constructor(
    private keyId: string,
    keyFile: string,
  ) {
    if (!KEY_ID.test(keyId))
      throw new Error('Invalid UI assertion configuration');
    this.key = protectedKey(keyFile);
    this.seen.add(keyId);
  }

  static fromEnvironment(
    environment: NodeJS.ProcessEnv = process.env,
  ): UiAssertionSigner | undefined {
    const enabled = environment.MCP_PUBLICATION_ENABLED;
    if (enabled === undefined || enabled === 'false') return undefined;
    if (
      enabled !== 'true' ||
      !environment.MCP_UI_ASSERTION_KEY_ID ||
      !environment.MCP_UI_ASSERTION_KEY_FILE
    ) {
      throw new Error('Invalid private UI assertion configuration');
    }
    return new UiAssertionSigner(
      environment.MCP_UI_ASSERTION_KEY_ID,
      environment.MCP_UI_ASSERTION_KEY_FILE,
    );
  }

  sign(binding: UiBinding, nowSeconds = Math.floor(Date.now() / 1000)): string {
    if (
      this.closed ||
      !Number.isSafeInteger(nowSeconds) ||
      nowSeconds < 0 ||
      nowSeconds > Number.MAX_SAFE_INTEGER - 30
    ) {
      throw new Error('UI assertion signer unavailable');
    }
    validate(binding);
    const claims = {
      audience: 'core',
      connector: binding.connector,
      client: binding.client,
      session: binding.session,
      proposal: binding.proposal,
      revision: binding.revision,
      hash: binding.hash,
      method: binding.method,
      invocation: randomUUID(),
      expiry: nowSeconds + 30,
    };
    const payload = Buffer.from(
      JSON.stringify(claims, Object.keys(claims).sort()),
    ).toString('base64url');
    const input = `${this.keyId}.${payload}`;
    const result = `${input}.${createHmac('sha256', this.key).update(input).digest('base64url')}`;
    if (Buffer.byteLength(result) > 4096)
      throw new Error('UI assertion exceeds private metadata limit');
    return result;
  }

  /** Install only the new write key. Core alone retains the previous key for its absolute60s read window. */
  rotate(keyId: string, keyFile: string): void {
    if (
      this.closed ||
      !KEY_ID.test(keyId) ||
      this.seen.has(keyId) ||
      this.seen.size >= 10000
    ) {
      throw new Error('Invalid UI assertion rotation');
    }
    const next = protectedKey(keyFile);
    if (timingSafeEqual(this.key, next)) {
      next.fill(0);
      throw new Error('UI assertion rotation requires fresh material');
    }
    this.key.fill(0);
    this.key = next;
    this.keyId = keyId;
    this.seen.add(keyId);
  }

  close(): void {
    this.closed = true;
    this.key.fill(0);
  }
  toJSON(): string {
    return 'UiAssertionSigner(redacted)';
  }
  [inspect.custom](): string {
    return this.toJSON();
  }
}
