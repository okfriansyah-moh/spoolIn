import { expect, it } from 'vitest';
import { compareUtf16 } from './ordering';

it('orders identifiers and fingerprints by UTF-16 code units independent of locale', () => {
  const values = ['é', 'a', 'Z', '10', '2', '😀', '\uE000', 'A', 'ä'];
  expect([...values].sort(compareUtf16)).toEqual(['10', '2', 'A', 'Z', 'a', 'ä', 'é', '😀', '\uE000']);
  expect(compareUtf16('same', 'same')).toBe(0);
  expect(compareUtf16('A', 'a')).toBeLessThan(0);
  expect(compareUtf16('a', 'A')).toBeGreaterThan(0);
});
