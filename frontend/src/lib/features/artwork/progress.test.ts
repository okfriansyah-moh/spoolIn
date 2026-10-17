import { describe, expect, it } from 'vitest';
import { stageLabel, transferCompleteIsNotSaved } from './progress';

describe('T026 artwork progress', () => {
  it('shows processing is separate from transfer completion', () => {
    expect(transferCompleteIsNotSaved(100, 100, false)).toBe(true);
  });

  it('labels saved only after attachment', () => {
    expect(stageLabel('saved')).toBe('Saved');
    expect(stageLabel('processing')).toBe('Processing…');
  });
});
