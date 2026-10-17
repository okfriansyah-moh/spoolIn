import { describe, expect, it } from 'vitest';
import { pollDelayMs } from './polling';

describe('upload poll schedule', () => {
  it('caps delay at five seconds', () => {
    expect(pollDelayMs(8)).toBe(5000);
  });
});
