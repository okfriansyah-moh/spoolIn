import type { FieldError } from './types';
import { operation } from '../session/outcome';
import type { IdentityOutcome } from '../session/commands';
import { record } from '../session/parse';
import { mutationResult } from '../editing/operation-result';

export type MovieMutationOutcome = Readonly<{
  receipt: IdentityOutcome;
  problemCode: string | null;
  resultMovieId: string | null;
  resultVersion: string | null;
  fieldErrors: readonly FieldError[];
}>;

const fieldIds: Record<string, string> = {
  title: 'movie-title',
  synopsis: 'movie-synopsis',
  originalTitle: 'movie-originalTitle',
  genreIds: 'movie-genres',
  genres: 'movie-genres',
  'release.year': 'movie-release-year',
  releaseYear: 'movie-release-year',
  'release.month': 'movie-releaseMonth',
  'release.day': 'movie-releaseDay',
  runtimeMinutes: 'movie-runtimeMinutes',
  originalLanguage: 'movie-originalLanguage',
  synopsisLanguage: 'movie-synopsisLanguage',
  countries: 'movie-countries',
};

function movieFieldErrors(value: unknown): readonly FieldError[] {
  const problem = record(value).problem;
  if (problem === null) return [];
  const fields = record(problem).fields;
  if (!Array.isArray(fields)) return [];
  return fields.flatMap((value) => {
    const field = record(value);
    if (typeof field.path !== 'string' || typeof field.message !== 'string')
      return [];
    const path = field.path
      .replace(/^(values|input)\./, '')
      .replace(/\[\d+\]/g, '')
      .replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase());
    return [
      { id: fieldIds[path] ?? 'movie-editor-form', message: field.message },
    ];
  });
}

export function movieMutationOutcome(
  value: unknown,
  expected: string,
): MovieMutationOutcome {
  const result = mutationResult(value, expected);
  return Object.freeze({
    receipt: result.receipt,
    problemCode: result.problemCode,
    resultMovieId: result.resultId,
    resultVersion: result.resultVersion,
    fieldErrors: movieFieldErrors(value),
  });
}

export function recoverMovieOperation(
  value: unknown,
  expected: string,
): IdentityOutcome | null {
  if (value === null) return null;
  return operation(value, expected);
}
