/**
 * T013 – Catalog filter unit tests.
 *
 * Covers:
 * - URL param parsing / round-trip
 * - Text code-point validation (AC-159, ATDD-133)
 * - Month-without-year correction (AC-014)
 * - Clearing year clears month (AC-014)
 * - hasActiveFilters distinguishes empty from no-match (AC-013, AC-015)
 * - buildSearchInput for valid/invalid input (AC-159, TD-C-007)
 */
import { describe, it, expect } from 'vitest';
import {
  parseCriteria,
  criteriaToParams,
  validateText,
  isValidMonth,
  hasActiveFilters,
  buildSearchInput,
  codePoints,
  formatReleaseDate,
} from './filter';

// ---------------------------------------------------------------------------
// parseCriteria
// ---------------------------------------------------------------------------
describe('parseCriteria', () => {
  it('returns empty criteria when params are absent', () => {
    const c = parseCriteria(new URLSearchParams());
    expect(c.text).toBe('');
    expect(c.kind).toBe('');
    expect(c.genres).toEqual([]);
    expect(c.year).toBe('');
    expect(c.month).toBe('');
  });

  it('reads q, kind, year, month from params', () => {
    const c = parseCriteria(
      new URLSearchParams('q=light&kind=MOVIE&year=2024&month=10'),
    );
    expect(c.text).toBe('light');
    expect(c.kind).toBe('MOVIE');
    expect(c.year).toBe('2024');
    expect(c.month).toBe('10');
  });

  it('clears month when year is absent (no hidden restriction)', () => {
    const c = parseCriteria(new URLSearchParams('q=light&month=10'));
    expect(c.year).toBe('');
    expect(c.month).toBe(''); // month cleared because no year
  });

  it('deduplicates and sorts genres', () => {
    const c = parseCriteria(new URLSearchParams('genre=Z&genre=A&genre=Z'));
    expect(c.genres).toEqual(['A', 'Z']);
  });

  it('handles multiple genre params', () => {
    const params = new URLSearchParams();
    params.append('genre', 'drama');
    params.append('genre', 'mystery');
    const c = parseCriteria(params);
    expect(c.genres).toEqual(['drama', 'mystery']);
  });
});

// ---------------------------------------------------------------------------
// criteriaToParams round-trip
// ---------------------------------------------------------------------------
describe('criteriaToParams', () => {
  it('produces empty params for empty criteria', () => {
    const p = criteriaToParams({
      text: '',
      kind: '',
      genres: [],
      year: '',
      month: '',
    });
    expect(p.toString()).toBe('');
  });

  it('includes q when text is set', () => {
    const p = criteriaToParams({
      text: 'Light',
      kind: '',
      genres: [],
      year: '',
      month: '',
    });
    expect(p.get('q')).toBe('Light');
  });

  it('includes kind when set', () => {
    const p = criteriaToParams({
      text: '',
      kind: 'TV',
      genres: [],
      year: '',
      month: '',
    });
    expect(p.get('kind')).toBe('TV');
  });

  it('includes year and month when both set', () => {
    const p = criteriaToParams({
      text: '',
      kind: '',
      genres: [],
      year: '2024',
      month: '10',
    });
    expect(p.get('year')).toBe('2024');
    expect(p.get('month')).toBe('10');
  });

  it('omits month when year is absent', () => {
    const p = criteriaToParams({
      text: '',
      kind: '',
      genres: [],
      year: '',
      month: '10',
    });
    expect(p.has('month')).toBe(false);
  });

  it('deduplicates and sorts genres in output', () => {
    const p = criteriaToParams({
      text: '',
      kind: '',
      genres: ['Z', 'A', 'Z'],
      year: '',
      month: '',
    });
    expect(p.getAll('genre')).toEqual(['A', 'Z']);
  });

  it('round-trips criteria correctly', () => {
    const original = {
      text: 'hello',
      kind: 'MOVIE',
      genres: ['g2', 'g1'],
      year: '2023',
      month: '5',
    };
    const parsed = parseCriteria(criteriaToParams(original));
    expect(parsed.text).toBe('hello');
    expect(parsed.kind).toBe('MOVIE');
    expect(parsed.genres).toEqual(['g1', 'g2']); // sorted
    expect(parsed.year).toBe('2023');
    expect(parsed.month).toBe('5');
  });
});

// ---------------------------------------------------------------------------
// validateText (AC-159, ATDD-133: PRD-BOUND-07 – 0–200 code points)
// ---------------------------------------------------------------------------
describe('validateText', () => {
  it('accepts empty string', () => {
    expect(validateText('').valid).toBe(true);
  });

  it('accepts exactly 200 code points', () => {
    const text = 'a'.repeat(200);
    const result = validateText(text);
    expect(result.valid).toBe(true);
  });

  it('rejects 201 code points', () => {
    const text = 'a'.repeat(201);
    const result = validateText(text);
    expect(result.valid).toBe(false);
    if (!result.valid) expect(result.reason).toBe('too-long');
  });

  it('counts surrogate pairs as 1 code point each', () => {
    // '𝄞' is a single code point (U+1D11E) but 2 UTF-16 chars
    const text = '𝄞'.repeat(200);
    expect(text.length).toBe(400); // UTF-16
    expect(codePoints(text)).toBe(200);
    expect(validateText(text).valid).toBe(true);

    const tooLong = '𝄞'.repeat(201);
    const result = validateText(tooLong);
    expect(result.valid).toBe(false);
    if (!result.valid) expect(result.codePoints).toBe(201);
  });
});

// ---------------------------------------------------------------------------
// isValidMonth (AC-014)
// ---------------------------------------------------------------------------
describe('isValidMonth', () => {
  it('returns false when year is empty', () => {
    expect(isValidMonth('10', '')).toBe(false);
  });

  it('returns true for months 1–12 with a year', () => {
    for (let m = 1; m <= 12; m++) {
      expect(isValidMonth(String(m), '2024')).toBe(true);
    }
  });

  it('returns false for month 0 or 13', () => {
    expect(isValidMonth('0', '2024')).toBe(false);
    expect(isValidMonth('13', '2024')).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// hasActiveFilters (AC-013, AC-015 – empty vs no-match)
// ---------------------------------------------------------------------------
describe('hasActiveFilters', () => {
  it('returns false for completely empty criteria', () => {
    expect(
      hasActiveFilters({ text: '', kind: '', genres: [], year: '', month: '' }),
    ).toBe(false);
  });

  it('returns true when text is set', () => {
    expect(
      hasActiveFilters({
        text: 'x',
        kind: '',
        genres: [],
        year: '',
        month: '',
      }),
    ).toBe(true);
  });

  it('returns true when kind is set', () => {
    expect(
      hasActiveFilters({
        text: '',
        kind: 'MOVIE',
        genres: [],
        year: '',
        month: '',
      }),
    ).toBe(true);
  });

  it('returns true when a genre is selected', () => {
    expect(
      hasActiveFilters({
        text: '',
        kind: '',
        genres: ['g1'],
        year: '',
        month: '',
      }),
    ).toBe(true);
  });

  it('returns true when year is set', () => {
    expect(
      hasActiveFilters({
        text: '',
        kind: '',
        genres: [],
        year: '2024',
        month: '',
      }),
    ).toBe(true);
  });
});

// ---------------------------------------------------------------------------
// buildSearchInput (AC-159, TD-C-007)
// ---------------------------------------------------------------------------
describe('buildSearchInput', () => {
  it('returns null for text > 200 code points', () => {
    const result = buildSearchInput({
      text: 'a'.repeat(201),
      kind: '',
      genres: [],
      year: '',
      month: '',
    });
    expect(result).toBeNull();
  });

  it('omits text when empty', () => {
    const result = buildSearchInput({
      text: '',
      kind: '',
      genres: [],
      year: '',
      month: '',
    });
    expect(result).not.toBeNull();
    expect(result?.text).toBeUndefined();
  });

  it('includes text when valid and non-empty', () => {
    const result = buildSearchInput({
      text: 'Light',
      kind: '',
      genres: [],
      year: '',
      month: '',
    });
    expect(result?.text).toBe('Light');
  });

  it('maps year and month to integers', () => {
    const result = buildSearchInput({
      text: '',
      kind: '',
      genres: [],
      year: '2024',
      month: '10',
    });
    expect(result?.year).toBe(2024);
    expect(result?.month).toBe(10);
  });

  it('omits month when year is absent', () => {
    const result = buildSearchInput({
      text: '',
      kind: '',
      genres: [],
      year: '',
      month: '10',
    });
    expect(result?.month).toBeUndefined();
  });

  it('passes genre IDs as genreIds', () => {
    const result = buildSearchInput({
      text: '',
      kind: '',
      genres: ['g1', 'g2'],
      year: '',
      month: '',
    });
    expect(result?.genreIds).toEqual(['g1', 'g2']);
  });
});

// ---------------------------------------------------------------------------
// formatReleaseDate
// ---------------------------------------------------------------------------
describe('formatReleaseDate', () => {
  it('returns null for null input', () => {
    expect(formatReleaseDate(null)).toBeNull();
  });

  it('returns null when year is null', () => {
    expect(
      formatReleaseDate({
        year: null,
        month: null,
        day: null,
        precision: 'YEAR',
      }),
    ).toBeNull();
  });

  it('returns year string for YEAR precision', () => {
    expect(
      formatReleaseDate({
        year: 2024,
        month: null,
        day: null,
        precision: 'YEAR',
      }),
    ).toBe('2024');
  });

  it('returns month-year for MONTH precision', () => {
    const label = formatReleaseDate({
      year: 2024,
      month: 10,
      day: null,
      precision: 'MONTH',
    });
    expect(label).toContain('2024');
    expect(label).toContain('October');
  });

  it('returns full date for DAY precision', () => {
    const label = formatReleaseDate({
      year: 2024,
      month: 10,
      day: 9,
      precision: 'DAY',
    });
    expect(label).toContain('2024');
    expect(label).toContain('October');
    expect(label).toContain('9');
  });
});
