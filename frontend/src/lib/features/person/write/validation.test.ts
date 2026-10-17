import { describe, expect, it } from 'vitest';
import { specimenPolicy } from '../../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { validatePersonDraft } from './validation';

const policy = specimenPolicy();

describe('validatePersonDraft', () => {
  it('accepts one-character professional name (PRD-BOUND-01)', () => {
    expect(validatePersonDraft({ name: 'V' }, policy)).toEqual([]);
  });

  it('rejects empty trimmed name', () => {
    const errors = validatePersonDraft({ name: '   ' }, policy);
    expect(errors.some((error) => error.id === 'person-name')).toBe(true);
  });

  it('rejects 101 code point name', () => {
    const errors = validatePersonDraft({ name: 'A'.repeat(101) }, policy);
    expect(errors.some((error) => error.id === 'person-name')).toBe(true);
  });
});
