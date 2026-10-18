import { expect, it } from 'vitest';
import { validateArtworkFile } from './file-validation';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';

it('checks effective formats and byte bounds before reading a file', () => {
  const policy = {
    ...specimenPolicy().artwork,
    maxInputBytes: '12',
    formats: ['PNG'],
  };
  expect(validateArtworkFile({ size: 12, type: 'image/png' }, policy)).toBe('');
  expect(
    validateArtworkFile({ size: 13, type: 'image/png' }, policy),
  ).toContain('byte limit');
  expect(
    validateArtworkFile({ size: 12, type: 'image/jpeg' }, policy),
  ).toContain('format');
  expect(validateArtworkFile({ size: 0, type: 'image/png' }, policy)).toContain(
    'nonempty',
  );
  expect(
    validateArtworkFile({ size: 12, type: 'image/png' }, undefined),
  ).toContain('unavailable');
});
