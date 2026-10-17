import { describe, expect, it } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { validateMovieDraft } from './validation';

const policy = specimenPolicy();

describe('T017 movie validation', () => {
  it('rejects whitespace-only title and missing genres', () => {
    const errors = validateMovieDraft(
      { title: '   ', synopsis: 'Valid synopsis', genreIds: [], release: { year: 2024 } },
      policy,
      'create',
    );
    expect(errors.some((error) => error.id === 'movie-title')).toBe(true);
    expect(errors.some((error) => error.id === 'movie-genres')).toBe(true);
  });
});
