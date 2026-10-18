import type { MovieDraft } from '../../generated/json-types';
import type { FieldError } from './types';

export function metadataErrors(draft: MovieDraft): FieldError[] {
  const errors: FieldError[] = [];
  const month = draft.release?.month,
    day = draft.release?.day;
  if (month != null && (!Number.isInteger(month) || month < 1 || month > 12))
    errors.push({
      id: 'movie-releaseMonth',
      message: 'Release month: Use 1–12 or leave blank.',
    });
  if (
    day != null &&
    (!Number.isInteger(day) ||
      month == null ||
      day < 1 ||
      day > daysInMonth(draft))
  )
    errors.push({
      id: 'movie-releaseDay',
      message: 'Release day: Enter a valid day with a known release month.',
    });
  const runtime = draft.runtimeMinutes;
  if (
    runtime != null &&
    (!Number.isInteger(runtime) || runtime < 1 || runtime > 100000)
  )
    errors.push({
      id: 'movie-runtimeMinutes',
      message: 'Runtime: Use 1–100000 whole minutes or leave blank.',
    });
  for (const field of ['originalLanguage', 'synopsisLanguage'] as const) {
    const value = draft[field]?.trim();
    if (
      value &&
      (value.length > 35 || !/^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$/.test(value))
    )
      errors.push({
        id: `movie-${field}`,
        message: 'Language: Use a BCP47 tag up to 35 characters.',
      });
  }
  if (draft.countries?.some((country) => !/^[A-Z]{2}$/.test(country)))
    errors.push({
      id: 'movie-countries',
      message:
        'Countries: Use uppercase two-letter ISO codes separated by commas.',
    });
  return errors;
}

function daysInMonth(draft: MovieDraft): number {
  const year = draft.release?.year ?? 0,
    month = draft.release?.month ?? 0;
  if (month === 2)
    return year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0) ? 29 : 28;
  return [4, 6, 9, 11].includes(month) ? 30 : 31;
}

export function rebaseMovieMetadata(
  base: MovieDraft,
  draft: MovieDraft,
  latest: MovieDraft,
): MovieDraft {
  const next = { ...latest };
  for (const field of [
    'release',
    'runtimeMinutes',
    'originalLanguage',
    'synopsisLanguage',
    'countries',
  ] as const) {
    if (
      draft[field] !== undefined &&
      JSON.stringify(draft[field]) !== JSON.stringify(base[field])
    )
      Object.assign(next, { [field]: draft[field] });
  }
  return next;
}
