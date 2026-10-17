import { describe, expect, it } from 'vitest';
import {
  FOREGROUND_POLL_CAP_MS,
  pollDelayMs,
  transferPercent,
} from './polling';
import { stageLabel, transferCompleteIsNotSaved } from './progress';

describe('T026 artwork progress', () => {
  it('shows processing is separate from transfer completion', () => {
    expect(transferCompleteIsNotSaved(100, 100, false)).toBe(true);
    expect(transferCompleteIsNotSaved(100, 100, true)).toBe(false);
  });

  it('labels saved only after attachment', () => {
    expect(stageLabel('saved')).toBe('Saved');
    expect(stageLabel('processing')).toBe('Processing…');
    expect(stageLabel('uncertain')).toBe('Checking result…');
  });

  it('doubles catalog poll delay up to five seconds', () => {
    expect(pollDelayMs(0)).toBe(500);
    expect(pollDelayMs(1)).toBe(1000);
    expect(pollDelayMs(2)).toBe(2000);
    expect(pollDelayMs(3)).toBe(4000);
    expect(pollDelayMs(4)).toBe(5000);
    expect(pollDelayMs(10)).toBe(5000);
  });

  it('caps foreground polling at five minutes', () => {
    expect(FOREGROUND_POLL_CAP_MS).toBe(300_000);
  });

  it('derives transfer percentage from xhr byte counts only', () => {
    expect(transferPercent(0, 200)).toBe(0);
    expect(transferPercent(100, 200)).toBe(50);
    expect(transferPercent(200, 200)).toBe(100);
  });
});
