import type { Policy } from '../../policy/types';
import { boundFor, checkText, trimOrdinary } from '../../policy/rules';
import type { AccountSubmission } from './commands';
export type FieldError = { id: string; message: string };
function field(
  value: string,
  name: 'registrationName' | 'email' | 'password',
  label: string,
  policy: Policy,
): FieldError[] {
  const result = checkText(value, boundFor(policy, name));
  const id = name === 'registrationName' ? 'account-name' : `account-${name}`;
  return result.error ? [{ id, message: `${label}: ${result.error}` }] : [];
}
export function validateSubmission(
  input: AccountSubmission,
  policy: Policy,
): FieldError[] {
  const errors = field(input.email, 'email', 'Email', policy);
  if (!/^[^\s@]+@[^\s@]+$/u.test(trimOrdinary(input.email)))
    errors.push({
      id: 'account-email',
      message: 'Email: Enter a valid email address.',
    });
  if (input.kind === 'register') {
    errors.push(
      ...field(input.name, 'registrationName', 'Name', policy),
      ...field(input.password, 'password', 'Password', policy),
    );
    if (/\p{Cc}/u.test(input.name))
      errors.push({
        id: 'account-name',
        message: 'Name: Remove control characters.',
      });
  } else if (!input.password)
    errors.push({
      id: 'account-password',
      message: 'Password: Enter your existing password.',
    });
  return [...new Map(errors.map((error) => [error.id, error])).values()];
}
