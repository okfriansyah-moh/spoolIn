import { boundFor, checkText, trimOrdinary } from '../../policy/rules';
import type { MovieDraft } from '../../generated/json-types';
import type { Policy } from '../../policy/types';
import type { FieldError } from './types';

function textField(
  value: string,
  field: 'title' | 'synopsis' | 'originalTitle',
  label: string,
  policy: Policy,
  required: boolean,
): FieldError[] {
  const bound = boundFor(policy, field);
  if (!required && trimOrdinary(value) === '') return [];
  const result = checkText(value, bound);
  return result.error
    ? [{ id: `movie-${field}`, message: `${label}: ${result.error}` }]
    : [];
}

export function validateMovieDraft(
  draft: MovieDraft,
  policy: Policy,
  mode: 'create' | 'edit',
): FieldError[] {
  const errors: FieldError[] = [];
  errors.push(
    ...textField(draft.title ?? '', 'title', 'Title', policy, true),
    ...textField(draft.synopsis ?? '', 'synopsis', 'Synopsis', policy, true),
  );
  if (
    mode === 'edit' &&
    draft.originalTitle !== undefined &&
    draft.originalTitle !== null
  ) {
    errors.push(
      ...textField(
        draft.originalTitle,
        'originalTitle',
        'Original title',
        policy,
        false,
      ),
    );
  } else if (mode === 'create' && draft.originalTitle) {
    errors.push(
      ...textField(
        draft.originalTitle,
        'originalTitle',
        'Original title',
        policy,
        false,
      ),
    );
  }
  const year = draft.release?.year;
  if (
    year === null ||
    year === undefined ||
    !Number.isInteger(year) ||
    year < 1 ||
    year > 9999
  ) {
    errors.push({
      id: 'movie-release-year',
      message: 'Release year: Enter a four-digit release year.',
    });
  }
  const genreIds = draft.genreIds ?? [];
  if (!genreIds.length) {
    errors.push({
      id: 'movie-genres',
      message: 'Genres: Choose at least one genre.',
    });
  }
  return [...new Map(errors.map((error) => [error.id, error])).values()];
}

export function problemMessage(code: string | null): string {
  switch (code) {
    case 'STALE_VERSION':
      return 'This movie changed while you were editing. Review the current version before saving again.';
    case 'FORBIDDEN':
      return 'You do not have permission to save this movie.';
    case 'FIELD_INVALID':
    case 'VALIDATION_FAILED':
      return 'Changes were not saved. Review the highlighted fields.';
    default:
      return 'Changes were not saved.';
  }
}
