import type { TextBound } from './types';
import { checkText, optionalIntent, trimOrdinary } from './rules';

export interface PolicyFeedback {
  errors: { id: string; message: string }[];
  outcome: string;
}
function textFeedback(
  value: string,
  bound: TextBound,
  id: string,
  label: string,
  success: string,
): PolicyFeedback {
  const result = checkText(value, bound);
  return {
    errors: result.error ? [{ id, message: `${label}: ${result.error}` }] : [],
    outcome: result.error ? '' : success,
  };
}
export function registrationFeedback(
  value: string,
  bound: TextBound,
): PolicyFeedback {
  return textFeedback(
    value,
    bound,
    'policy-name',
    'Registration name',
    'The name meets the current registration rule.',
  );
}
export function draftFeedback(value: string, bound: TextBound): PolicyFeedback {
  return textFeedback(
    value,
    bound,
    'policy-draft',
    'Synopsis',
    'The revised draft meets the current rule.',
  );
}
export function optionalFeedback(
  present: boolean,
  value: string,
  bound: TextBound,
): PolicyFeedback {
  const intent = optionalIntent(present, value);
  if (intent.kind === 'omitted')
    return { errors: [], outcome: 'Original title stays unchanged.' };
  if (trimOrdinary(value) !== '') {
    const result = checkText(value, bound);
    if (result.error)
      return {
        errors: [{ id: 'policy-original', message: result.error }],
        outcome: '',
      };
  }
  return {
    errors: [],
    outcome:
      intent.kind === 'clear'
        ? 'Original title will be cleared.'
        : `Original title will become ${intent.value}.`,
  };
}
