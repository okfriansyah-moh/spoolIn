import { describe, expect, it } from 'vitest';
import { compareText } from '../../src/operations/order.js';

describe('stable protocol ordering', () => {
  it('preserves default string ordering across Unicode and punctuation', () => {
    const values = ['a', 'A', '10', '2', 'ä', 'é', '😀', '\uE000', '', 'a-b', 'a_b'];
    expect([...values].sort(compareText)).toEqual([...values].sort());
    expect(compareText('same', 'same')).toBe(0);
    expect(compareText('a', 'b')).toBe(-1);
    expect(compareText('b', 'a')).toBe(1);
  });
});
