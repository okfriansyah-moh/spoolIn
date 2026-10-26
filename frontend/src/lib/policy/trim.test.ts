import { expect, it } from 'vitest';
import { trimOrdinary } from './rules';

it('trims Unicode White_Space including NEL while preserving BOM and interior whitespace', () => {
  const whitespace = '\u0009\u000A\u000B\u000C\u000D\u0020\u0085\u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u2028\u2029\u202F\u205F\u3000';
  expect(trimOrdinary(`${whitespace}a\u0085b${whitespace}`)).toBe('a\u0085b');
  expect(trimOrdinary(whitespace)).toBe('');
  expect(trimOrdinary('')).toBe('');
  expect(trimOrdinary('\uFEFF text \uFEFF')).toBe('\uFEFF text \uFEFF');
  expect(trimOrdinary(` ${'x '.repeat(10000)} `)).toBe('x '.repeat(10000).trimEnd());
});
