import { expect, it } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { validateSubmission } from './validation';
it('existing sign-in passwords are not subjected to new-password minimums or rewritten', () => {
  const input = {
    kind: 'signIn' as const,
    email: 'person@example.test',
    password: ' x ',
  };
  expect(validateSubmission(input, specimenPolicy())).toEqual([]);
  expect(input.password).toBe(' x ');
});
it('registration reports bounded name, email and password errors once per field', () => {
  const errors = validateSubmission(
    { kind: 'register', name: '\n', email: 'not-an-address', password: '' },
    specimenPolicy(),
  );
  expect(errors.map((error) => error.id).sort()).toEqual([
    'account-email',
    'account-name',
    'account-password',
  ]);
});
it('valid surrounding ordinary name spaces do not transform password or retained input', () => {
  const input = {
    kind: 'register' as const,
    name: ' A Person ',
    email: 'person@example.test',
    password: '  long password as entered  ',
  };
  expect(validateSubmission(input, specimenPolicy())).toEqual([]);
  expect(input.name).toBe(' A Person ');
  expect(input.password.startsWith('  ')).toBe(true);
});
