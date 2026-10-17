import { describe, expect, it } from 'vitest';
import { permanentDeleteReady } from './rules';

describe('T025 archive rules', () => {
  it('blocks person permanent delete until impact token is present', () => {
    expect(permanentDeleteReady('people', null)).toBe(false);
    expect(permanentDeleteReady('people', '')).toBe(false);
    expect(permanentDeleteReady('people', 'token-abc')).toBe(true);
  });

  it('allows movie permanent delete without impact token', () => {
    expect(permanentDeleteReady('movies', null)).toBe(true);
  });
});
