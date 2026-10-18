import { createHmac, randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';

export type UiBinding = Readonly<{
  connector: string;
  client: 'CHATGPT' | 'CLAUDE';
  session: string;
  proposal: string;
  revision: number;
  hash: string;
  method: '/spoolin.v1.McpGatewayService/ApproveMovie' | '/spoolin.v1.McpGatewayService/RejectMovie';
}>;

/** Private RPC metadata only. Callers must first verify a supported host's authenticated app channel. */
export class UiAssertionSigner {
  private readonly key: Buffer;
  constructor(private readonly keyId: string, keyFile: string) {
    this.key = readFileSync(keyFile);
    if (this.key.length !== 32 || !/^[A-Za-z0-9_-]{1,64}$/.test(keyId)) throw new Error('Invalid UI assertion configuration');
  }
  sign(binding: UiBinding, nowSeconds = Math.floor(Date.now() / 1000)): string {
    if (!binding.connector || !binding.session || !binding.proposal || !binding.hash || binding.revision < 1) {
      throw new Error('Incomplete private UI binding');
    }
    const claims = { audience: 'core', ...binding, invocation: randomUUID(), expiry: nowSeconds + 30 };
    const payload = Buffer.from(JSON.stringify(claims, Object.keys(claims).sort())).toString('base64url');
    const input = `${this.keyId}.${payload}`;
    return `${input}.${createHmac('sha256', this.key).update(input).digest('base64url')}`;
  }
  close(): void { this.key.fill(0); }
}
