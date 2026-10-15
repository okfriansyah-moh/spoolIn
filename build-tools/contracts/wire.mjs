/** Scalar rules shared by the contract verification boundary. */
import { Buffer } from 'node:buffer';
export function longString(value) {
  if (typeof value !== 'string' || !/^-?[0-9]+$/.test(value)) {
    throw new TypeError('Long requires a decimal string');
  }
  const number = BigInt(value);
  if (number < -9223372036854775808n || number > 9223372036854775807n) {
    throw new RangeError('Long outside signed64 range');
  }
  return value;
}

export function int32(value) {
  if (!Number.isInteger(value) || value < -2147483648 || value > 2147483647) {
    throw new RangeError('Int outside signed32 range');
  }
  return value;
}

export function uuid(value) {
  if (
    typeof value !== 'string' ||
    !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(
      value,
    )
  ) {
    throw new TypeError('UUID requires lowercase hyphenated encoding');
  }
  return value;
}

export function unicode(value) {
  if (typeof value !== 'string' || !value.isWellFormed()) {
    throw new TypeError('Invalid Unicode text');
  }
  return value;
}

export function ordinaryText(value) {
  unicode(value);
  if (value.includes('\u0000')) throw new TypeError('NUL in ordinary text');
  return value.replace(/^\p{White_Space}+|\p{White_Space}+$/gu, '');
}

export function dateTime(value) {
  if (
    typeof value !== 'string' ||
    !/^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d{1,6})?Z$/.test(value)
  ) {
    throw new TypeError(
      'DateTime requires UTC with at most 6 fractional digits',
    );
  }
  const instant = new Date(value);
  if (
    !Number.isFinite(instant.valueOf()) ||
    instant.toISOString().slice(0, 19) !== value.slice(0, 19) ||
    value.startsWith('0000')
  ) {
    throw new TypeError('Invalid UTC instant');
  }
  return value;
}

export function truncateTimestamp({ seconds, nanos }) {
  longString(seconds);
  const instant = BigInt(seconds);
  if (instant < -62135596800n || instant > 253402300799n) {
    throw new RangeError('Invalid timestamp seconds');
  }
  if (!Number.isInteger(nanos) || nanos < 0 || nanos > 999999999) {
    throw new RangeError('Invalid timestamp nanos');
  }
  return { seconds, nanos: Math.floor(nanos / 1000) * 1000 };
}

export function base64(value) {
  if (
    typeof value !== 'string' ||
    !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(
      value,
    ) ||
    Buffer.from(value, 'base64').toString('base64') !== value
  ) {
    throw new TypeError('Invalid padded standard Base64');
  }
  return value;
}

export function approvalChallenge(value) {
  if (
    typeof value !== 'string' ||
    !/^[A-Za-z0-9_-]{43}$/.test(value) ||
    Buffer.from(value, 'base64url').length !== 32 ||
    Buffer.from(value, 'base64url').toString('base64url') !== value
  ) {
    throw new TypeError('Invalid approval challenge encoding');
  }
  return value;
}

function jsonPrimitive(value) {
  return (
    value === null ||
    typeof value === 'boolean' ||
    (typeof value === 'number' && Number.isFinite(value))
  );
}

function jsonValue(value, seen = new Set()) {
  if (jsonPrimitive(value)) return;
  if (typeof value === 'string') {
    unicode(value);
    return;
  }
  if (typeof value !== 'object') throw new TypeError('Unsupported JSON value');
  if (seen.has(value)) throw new TypeError('Circular JSON value');
  seen.add(value);
  if (Array.isArray(value)) {
    for (const item of value) jsonValue(item, seen);
  } else jsonObject(value, seen);
  seen.delete(value);
}

function jsonObject(value, seen) {
  if (![Object.prototype, null].includes(Object.getPrototypeOf(value)))
    throw new TypeError('Unsupported JSON object');
  for (const [key, item] of Object.entries(value)) {
    unicode(key);
    jsonValue(item, seen);
  }
}

export function envelope(value, limit = 131072) {
  jsonValue(value);
  const encoded = JSON.stringify(value);
  if (
    typeof encoded !== 'string' ||
    !encoded.isWellFormed() ||
    Buffer.byteLength(encoded, 'utf8') > limit
  ) {
    throw new RangeError('Envelope byte limit exceeded');
  }
  return encoded;
}
