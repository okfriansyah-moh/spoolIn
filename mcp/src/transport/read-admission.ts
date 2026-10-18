import { createHmac, randomBytes } from 'node:crypto';

export class ReadLimited extends Error {
  constructor(readonly retryAfterMs: number) {
    super('READ admission throttled');
  }
}
export class ReadUnavailable extends Error {
  constructor() {
    super('READ admission unavailable');
  }
}
type Binding = { window: number; hits: number; expires: number };
function integer(
  value: string | undefined,
  fallback: number,
  maximum: number,
): number {
  if (value === undefined) return fallback;
  if (!/^\d+$/.test(value)) throw new Error('Invalid MCP READ configuration');
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 1 || parsed > maximum)
    throw new Error('Invalid MCP READ configuration');
  return parsed;
}

/** Per-process protection only; Core independently owns durable tool admission. */
export class ReadAdmission {
  private readonly maximum: number;
  private readonly windowMs: number;
  private readonly capacity: number;
  private readonly key = randomBytes(32);
  private readonly bindings = new Map<string, Binding>();
  private previousTime = 0;
  constructor(
    environment: NodeJS.ProcessEnv = process.env,
    private readonly clock: () => number = Date.now,
  ) {
    this.maximum = integer(
      environment.RATE_LIMIT_READ_MAX_REQUESTS,
      120,
      2147483647,
    );
    this.windowMs =
      integer(environment.RATE_LIMIT_READ_WINDOW_SECONDS, 60, 86400) * 1000;
    this.capacity = integer(
      environment.MCP_RATE_LIMIT_MAX_BINDINGS,
      1024,
      100000,
    );
  }
  admit(verifiedDigest: Buffer): void {
    const now = this.clock();
    if (
      !Number.isSafeInteger(now) ||
      now < this.previousTime ||
      verifiedDigest.length !== 32
    )
      throw new ReadUnavailable();
    this.previousTime = now;
    const identity = createHmac('sha256', this.key)
      .update('mcp-read-v1\0')
      .update(verifiedDigest)
      .digest('hex');
    const window = Math.floor(now / this.windowMs) * this.windowMs;
    const binding = this.binding(identity, window, now);
    if (binding.window !== window) {
      binding.window = window;
      binding.hits = 0;
      binding.expires = window + 2 * this.windowMs;
    }
    if (binding.hits >= this.maximum)
      throw new ReadLimited(Math.max(1, window + this.windowMs - now));
    binding.hits += 1;
  }
  private binding(identity: string, window: number, now: number): Binding {
    const existing = this.bindings.get(identity);
    if (existing && existing.expires > now) return existing;
    this.purge(now);
    if (this.bindings.size >= this.capacity) {
      let next = now + 2 * this.windowMs;
      for (const retained of this.bindings.values())
        next = Math.min(next, retained.expires);
      throw new ReadLimited(Math.max(1, next - now));
    }
    const binding = { window, hits: 0, expires: window + 2 * this.windowMs };
    this.bindings.set(identity, binding);
    return binding;
  }
  private purge(now: number): void {
    for (const [identity, binding] of this.bindings)
      if (binding.expires <= now) this.bindings.delete(identity);
  }
}
