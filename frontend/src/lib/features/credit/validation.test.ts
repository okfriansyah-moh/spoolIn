import { describe, expect, it } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { validateCharacterNames } from './validation';

const policy = specimenPolicy();

describe('validateCharacterNames', () => {
  it('preserves slash inside one character string', () => {
    expect(validateCharacterNames(['Maya / Echo'], policy, true)).toBeNull();
  });

  it('rejects duplicate trimmed names', () => {
    expect(validateCharacterNames(['Maya', 'Maya'], policy, true)).toMatch(
      /duplicate/i,
    );
  });

  it('rejects characters on creator role', () => {
    expect(validateCharacterNames(['Name'], policy, false)).toMatch(
      /does not accept/i,
    );
  });
});
