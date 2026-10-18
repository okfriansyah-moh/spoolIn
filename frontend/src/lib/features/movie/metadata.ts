import type { MovieDraft } from '../../generated/json-types';
import type { FieldError } from './types';

function invalidMonth(month: number | null | undefined): boolean {
  return month != null && (!Number.isInteger(month) || month < 1 || month > 12);
}

function invalidDay(draft: MovieDraft): boolean {
  const day = draft.release?.day;
  return (
    day != null &&
    (!Number.isInteger(day) ||
      draft.release?.month == null ||
      day < 1 ||
      day > daysInMonth(draft))
  );
}

function invalidRuntime(runtime: number | null | undefined): boolean {
  return (
    runtime != null &&
    (!Number.isInteger(runtime) || runtime < 1 || runtime > 100000)
  );
}

function languageErrors(draft: MovieDraft): FieldError[] {
  const errors: FieldError[] = [];
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
  return errors;
}

export function metadataErrors(draft: MovieDraft): FieldError[] {
  const errors: FieldError[] = [];
  if (invalidMonth(draft.release?.month))
    errors.push({
      id: 'movie-releaseMonth',
      message: 'Release month: Use 1–12 or leave blank.',
    });
  if (invalidDay(draft))
    errors.push({
      id: 'movie-releaseDay',
      message: 'Release day: Enter a valid day with a known release month.',
    });
  if (invalidRuntime(draft.runtimeMinutes))
    errors.push({
      id: 'movie-runtimeMinutes',
      message: 'Runtime: Use 1–100000 whole minutes or leave blank.',
    });
  errors.push(...languageErrors(draft));
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
