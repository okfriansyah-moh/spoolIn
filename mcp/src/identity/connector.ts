import { createHash } from 'node:crypto';

export function connectorTokenDigest(rawToken: string): Buffer {
  return createHash('sha256').update(rawToken, 'utf8').digest();
}
