import { boundFor, checkText, trimOrdinary } from '../../../policy/rules';
import type { PersonDraft } from '../../../generated/json-types';
import type { Policy } from '../../../policy/types';
import type { FieldError } from './types';

export function validatePersonDraft(draft: PersonDraft, policy: Policy): FieldError[] {
  const errors: FieldError[] = [];
  const bound = boundFor(policy, 'personName');
  const name = draft.name ?? '';
  if (trimOrdinary(name) === '') {
    errors.push({ id: 'person-name', message: 'Professional name: Enter a name.' });
  } else {
    const result = checkText(name, bound);
    if (result.error) errors.push({ id: 'person-name', message: `Professional name: ${result.error}` });
  }
  if (draft.biography !== undefined && draft.biography !== null && draft.biography !== '') {
    const bio = checkText(draft.biography, boundFor(policy, 'synopsis'));
    if (bio.error) errors.push({ id: 'person-biography', message: `Biography: ${bio.error}` });
  }
  return errors;
}

export function problemMessage(code: string | null): string {
  switch (code) {
    case 'STALE_VERSION':
    case 'VERSION_CONFLICT':
      return 'This person changed while you were editing. Review the current record before saving again.';
    case 'FORBIDDEN':
      return 'You do not have permission to save this person.';
    case 'FIELD_INVALID':
    case 'VALIDATION_FAILED':
      return 'Changes were not saved. Review the highlighted fields.';
    default:
      return 'Changes were not saved.';
  }
}
