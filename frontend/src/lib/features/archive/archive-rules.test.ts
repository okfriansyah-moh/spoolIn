import { describe, expect, it } from 'vitest';
import {
  formatDeletionImpactSummary,
  permanentDeleteReady,
  personDeleteSubmitDisabled,
} from './rules';

describe('T025 archive rules', () => {
  it('blocks person permanent delete until impact token is present', () => {
    expect(permanentDeleteReady('people', null)).toBe(false);
    expect(permanentDeleteReady('people', '')).toBe(false);
    expect(permanentDeleteReady('people', '   ')).toBe(false);
    expect(permanentDeleteReady('people', 'token-abc')).toBe(true);
  });

  it('allows movie permanent delete without impact token', () => {
    expect(permanentDeleteReady('movies', null)).toBe(true);
    expect(permanentDeleteReady('movies', '')).toBe(true);
  });

  it('formats deletion impact summaries for person confirmation', () => {
    expect(formatDeletionImpactSummary(0, [])).toBe(
      'No active credits reference this person.',
    );
    expect(
      formatDeletionImpactSummary(2, [
        'The Last Light',
        'Letters from Tomorrow',
      ]),
    ).toBe('Affects 2 credit(s): The Last Light, Letters from Tomorrow');
  });

  it('keeps person delete disabled while impact token is missing or busy', () => {
    expect(personDeleteSubmitDisabled('people', null, false)).toBe(true);
    expect(personDeleteSubmitDisabled('people', 'token', true)).toBe(true);
    expect(personDeleteSubmitDisabled('people', 'token', false)).toBe(false);
    expect(personDeleteSubmitDisabled('movies', null, false)).toBe(false);
  });
});
