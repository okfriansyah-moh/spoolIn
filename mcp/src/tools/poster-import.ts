import { lookup } from 'node:dns/promises';
import { request } from 'node:https';
import { BlockList, isIP } from 'node:net';
import { GatewayFailure } from '../operations/gateway-failure.js';
import { activeCancellation } from '../operations/tracing.js';

export const MAX_POSTER_INPUT_BYTES = 5 * 1024 * 1024;
export const MAX_POSTER_REQUEST_BYTES = Math.ceil(MAX_POSTER_INPUT_BYTES / 3) * 4 + 128 * 1024;
const denied = new BlockList();
for (const [address, bits] of [
  ['0.0.0.0', 8], ['10.0.0.0', 8], ['100.64.0.0', 10],
  ['127.0.0.0', 8], ['169.254.0.0', 16], ['172.16.0.0', 12],
  ['192.0.0.0', 24], ['192.0.2.0', 24], ['192.88.99.0', 24],
  ['192.168.0.0', 16], ['198.18.0.0', 15], ['198.51.100.0', 24],
  ['203.0.113.0', 24], ['224.0.0.0', 4], ['240.0.0.0', 4],
] as const) denied.addSubnet(address, bits, 'ipv4');
const globalV6 = new BlockList();
globalV6.addSubnet('2000::', 3, 'ipv6');
for (const [address, bits] of [
  ['2001::', 23], ['2001:db8::', 32], ['2002::', 16], ['3fff::', 20],
] as const) denied.addSubnet(address, bits, 'ipv6');
let downloads = 0;

function allowed(address: string): boolean {
  const family = isIP(address);
  if (family === 4) return !denied.check(address, 'ipv4');
  return family === 6 && globalV6.check(address, 'ipv6') && !denied.check(address, 'ipv6');
}
function imageType(bytes: Buffer): string {
  if (bytes.subarray(0, 3).equals(Buffer.from([0xff, 0xd8, 0xff]))) return 'image/jpeg';
  if (bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) return 'image/png';
  if (bytes.subarray(0, 4).toString() === 'RIFF' && bytes.subarray(8, 12).toString() === 'WEBP') return 'image/webp';
  throw new GatewayFailure('FIELD_INVALID');
}
export function decodePoster(content: string, maximum: number): Buffer {
  if (content.length > Math.ceil(maximum / 3) * 4) throw new GatewayFailure('RESOURCE_EXHAUSTED');
  if (!/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(content))
    throw new GatewayFailure('FIELD_INVALID');
  const bytes = Buffer.from(content, 'base64');
  if (!bytes.length || bytes.toString('base64') !== content) throw new GatewayFailure('FIELD_INVALID');
  if (bytes.length > maximum) throw new GatewayFailure('RESOURCE_EXHAUSTED');
  return bytes;
}
function checkedUrl(raw: string): URL {
  let url: URL;
  try { url = new URL(raw); } catch { throw new GatewayFailure('FIELD_INVALID'); }
  if (url.protocol !== 'https:' || url.username || url.password || (url.port && url.port !== '443'))
    throw new GatewayFailure('FIELD_INVALID');
  url.hash = '';
  return url;
}
async function resolvePublic(url: URL, signal: AbortSignal) {
  const hostname = url.hostname.replace(/^\[|\]$/g, '');
  if (isIP(hostname)) {
    if (!allowed(hostname)) throw new GatewayFailure('FORBIDDEN');
    return { address: hostname, family: isIP(hostname) };
  }
  let abort: (() => void) | undefined;
  try {
    const addresses = await Promise.race([
      lookup(hostname, { all: true, verbatim: true }),
      new Promise<never>((_, reject) => {
        abort = () => reject(new GatewayFailure('DEADLINE_EXCEEDED'));
        signal.addEventListener('abort', abort, { once: true });
        if (signal.aborted) abort();
      }),
    ]);
    if (!addresses.length || addresses.some(({ address }) => !allowed(address)))
      throw new GatewayFailure('FORBIDDEN');
    return addresses[0]!;
  } finally {
    if (abort) signal.removeEventListener('abort', abort);
  }
}
async function download(url: URL, maximum: number, signal: AbortSignal): Promise<Buffer> {
  for (let redirects = 0; redirects <= 3; redirects += 1) {
    signal.throwIfAborted();
    const pinned = await resolvePublic(url, signal);
    const reply = await new Promise<import('node:http').IncomingMessage>((resolve, reject) => {
      const req = request(url, {
        method: 'GET', signal, agent: false, autoSelectFamily: false, maxHeaderSize: 16 * 1024,
        headers: { accept: 'image/jpeg,image/png,image/webp', 'accept-encoding': 'identity' },
        lookup: (_hostname, _options, callback) => callback(null, pinned.address, pinned.family),
      }, resolve);
      req.once('error', reject);
      req.end();
    });
    if ([301, 302, 303, 307, 308].includes(reply.statusCode ?? 0)) {
      reply.destroy();
      if (!reply.headers.location || redirects === 3) throw new GatewayFailure('FIELD_INVALID');
      url = checkedUrl(new URL(reply.headers.location, url).href);
      continue;
    }
    const length = Number(reply.headers['content-length'] ?? 0);
    if (reply.statusCode !== 200 || length > maximum || (reply.headers['content-encoding'] && reply.headers['content-encoding'] !== 'identity')) {
      reply.destroy();
      throw new GatewayFailure(length > maximum ? 'RESOURCE_EXHAUSTED' : 'FIELD_INVALID');
    }
    const chunks: Buffer[] = [];
    let size = 0;
    for await (const chunk of reply) {
      const bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      size += bytes.length;
      if (size > maximum) { reply.destroy(); throw new GatewayFailure('RESOURCE_EXHAUSTED'); }
      chunks.push(bytes);
    }
    return Buffer.concat(chunks);
  }
  throw new GatewayFailure('FIELD_INVALID');
}
export async function posterInput(args: Record<string, unknown>, maximum: number) {
  const limit = Math.min(maximum, MAX_POSTER_INPUT_BYTES);
  let bytes: Buffer;
  if (typeof args.content === 'string') {
    bytes = decodePoster(args.content, limit);
  } else {
    if (downloads >= 4) throw new GatewayFailure('RESOURCE_EXHAUSTED');
    downloads += 1;
    try {
      const timeout = AbortSignal.timeout(15_000);
      const cancellation = activeCancellation();
      const signal = cancellation ? AbortSignal.any([timeout, cancellation]) : timeout;
      bytes = await download(checkedUrl(String(args.url)), limit, signal);
    } finally { downloads -= 1; }
  }
  const mediaType = imageType(bytes);
  if (args.mediaType && args.mediaType !== mediaType) throw new GatewayFailure('FIELD_INVALID');
  return { bytes, mediaType, sourceUrl: String(args.url ?? args.sourceUrl ?? '') };
}
