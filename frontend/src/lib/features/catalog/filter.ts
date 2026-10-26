import { compareUtf16 } from '../../state/ordering';
/**
 * T013 – Catalog filter URL serialisation and validation.
 *
 * URL params:  q, kind, genre (repeatable), year, month
 * Rules:
 *   - Clearing year also clears month (no hidden month restriction).
 *   - Month without year is invalid (correction shown, not applied).
 *   - Text > 200 code points is invalid (retained, correction shown, no query issued).
 *   - Genre list is sorted and deduplicated for stable URLs.
 */

import type { Criteria, TextValidity } from './types';
import { MAX_SEARCH_CODE_POINTS } from './types';

type QueryParams = Pick<URLSearchParams, 'get' | 'getAll'>;

/** Parse URL search params into Criteria. */
export function parseCriteria(params: QueryParams): Criteria {
  const text = params.get('q') ?? '';
  const kind = params.get('kind') ?? '';
  const year = params.get('year') ?? '';
  // Clearing year must also clear month – enforce on read so Back restores correctly.
  const month = year ? (params.get('month') ?? '') : '';
  const deduped = [...new Set(params.getAll('genre').map(String))].sort(compareUtf16);
  return Object.freeze({
    text,
    kind,
    genres: Object.freeze(deduped),
    year,
    month,
  });
}

/** Serialise Criteria to URLSearchParams (stable, sorted). */
export function criteriaToParams(criteria: Criteria): URLSearchParams {
  const params = new URLSearchParams();
  if (criteria.text) params.set('q', criteria.text);
  if (criteria.kind) params.set('kind', criteria.kind);
  const deduped = [...new Set(criteria.genres)].sort(compareUtf16);
  for (const g of deduped) params.append('genre', g);
  if (criteria.year) {
    params.set('year', criteria.year);
    if (criteria.month) params.set('month', criteria.month);
  }
  return params;
}

/** Count Unicode code points (not UTF-16 chars). */
export function codePoints(text: string): number {
  return [...text].length;
}

/** Validate search text length against the configured bound. */
export function validateText(
  text: string,
  max = MAX_SEARCH_CODE_POINTS,
): TextValidity {
  const cp = codePoints(text);
  if (cp > max) return { valid: false, reason: 'too-long', codePoints: cp };
  return { valid: true };
}

/** True when month is valid (1–12) for a given year string. */
export function isValidMonth(month: string, year: string): boolean {
  if (!year) return false;
  const m = Number(month);
  return Number.isInteger(m) && m >= 1 && m <= 12;
}

/** True when any filter category is active (used to distinguish empty vs no-match). */
export function hasActiveFilters(criteria: Criteria): boolean {
  return (
    criteria.text !== '' ||
    criteria.kind !== '' ||
    criteria.genres.length > 0 ||
    criteria.year !== '' ||
    criteria.month !== ''
  );
}

/**
 * Build the SearchInput variables from Criteria.
 * Returns null when text is invalid (caller must show correction, not query).
 */
export function buildSearchInput(criteria: Criteria): {
  text?: string;
  kind?: string;
  genreIds?: string[];
  year?: number;
  month?: number;
} | null {
  const textCheck = validateText(criteria.text);
  if (!textCheck.valid) return null;

  const input: {
    text?: string;
    kind?: string;
    genreIds?: string[];
    year?: number;
    month?: number;
  } = {};

  if (criteria.text) input.text = criteria.text;
  if (criteria.kind === 'MOVIE' || criteria.kind === 'TV')
    input.kind = criteria.kind;
  if (criteria.genres.length > 0) input.genreIds = [...criteria.genres];
  applyYearMonthFilters(input, criteria);
  return input;
}

function applyYearMonthFilters(
  input: { year?: number; month?: number },
  criteria: Criteria,
): void {
  if (!criteria.year) return;
  const y = Number(criteria.year);
  if (!Number.isInteger(y) || y <= 0) return;
  input.year = y;
  if (!criteria.month) return;
  const m = Number(criteria.month);
  if (Number.isInteger(m) && m >= 1 && m <= 12) input.month = m;
}

/** Format a partial release date label from year/month/day/precision. */
export function formatReleaseDate(
  release:
    | {
        year: number | null;
        month: number | null;
        day: number | null;
        precision: string;
      }
    | null
    | undefined,
): string | null {
  if (!release || release.year === null) return null;
  if (release.precision === 'YEAR') return String(release.year);
  if (release.precision === 'MONTH' && release.month !== null) {
    const d = new Date(release.year, release.month - 1);
    return d.toLocaleDateString('en-US', { year: 'numeric', month: 'long' });
  }
  if (
    release.precision === 'DAY' &&
    release.month !== null &&
    release.day !== null
  ) {
    const d = new Date(release.year, release.month - 1, release.day);
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }
  return String(release.year);
}
