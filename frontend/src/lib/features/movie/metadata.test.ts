import { describe, expect, it } from 'vitest';
import { applyMovieDraftField } from './draft-field-update';
import { draftToCreateInput, draftToUpdatePatch } from './patch';
import { movieToDraft, toManagedMovie } from './projection';
import { metadataErrors, rebaseMovieMetadata } from './metadata';

const movie = toManagedMovie({
  id: 'title',
  kind: 'MOVIE',
  state: 'ACTIVE',
  version: '7',
  title: 'Loaded title',
  synopsis: 'Loaded synopsis',
  originalTitle: 'Original',
  genres: [],
  release: { year: 2024, month: 2, day: 29 },
  runtimeMinutes: 123,
  originalLanguage: 'id',
  synopsisLanguage: 'en-GB',
  countries: ['ID', 'GB'],
});

describe('movie metadata production projection and write paths', () => {
  it('retains every loaded date, language, runtime and country through draft and create conversion', () => {
    const draft = movieToDraft(movie);
    expect(draftToCreateInput(draft)).toMatchObject({
      release: { year: 2024, month: 2, day: 29 },
      runtimeMinutes: 123,
      originalLanguage: 'id',
      synopsisLanguage: 'en-GB',
      countries: ['ID', 'GB'],
    });
    expect(draftToUpdatePatch(movie, draft, false)).toEqual({});
  });
  it('writes month/day changes with unchanged year and explicitly clears optional values', () => {
    const draft = {
      ...movieToDraft(movie),
      release: { year: 2024, month: 3, day: null },
      runtimeMinutes: null,
      originalLanguage: '',
      countries: [],
    };
    expect(draftToUpdatePatch(movie, draft, false)).toEqual({
      release: { year: 2024, month: 3, day: null },
      runtimeMinutes: null,
      originalLanguage: null,
      countries: [],
    });
  });
  it('rebases untouched metadata but preserves changed date precision and deliberate clears', () => {
    const base = movieToDraft(movie);
    const latest = {
      ...base,
      runtimeMinutes: 124,
      countries: ['FR'],
      synopsisLanguage: 'fr',
    };
    const draft = {
      ...base,
      originalLanguage: '',
      release: { year: 2024, month: 2, day: null },
    };
    expect(rebaseMovieMetadata(base, draft, latest)).toMatchObject({
      runtimeMinutes: 124,
      countries: ['FR'],
      synopsisLanguage: 'fr',
      originalLanguage: '',
      release: { year: 2024, month: 2, day: null },
    });
  });
  it('rejects impossible precision and invalid runtime before submission instead of silently clearing', () => {
    expect(
      metadataErrors({
        release: { year: 2023, month: 2, day: 29 },
        runtimeMinutes: 0,
      }).map((x) => x.id),
    ).toEqual(['movie-releaseDay', 'movie-runtimeMinutes']);
    expect(
      metadataErrors({ release: { year: 2024, month: null, day: 1 } }),
    ).toHaveLength(1);
    const draft = applyMovieDraftField({}, 'runtimeMinutes', 'invalid').draft;
    expect(metadataErrors(draft)).toHaveLength(1);
    expect(
      applyMovieDraftField({}, 'countries', 'ID, GB').draft.countries,
    ).toEqual(['ID', 'GB']);
  });
});

it('edits release precision without losing other date fields and clears blank numbers', () => {
  const original = movieToDraft(movie);
  const changed = applyMovieDraftField(original, 'releaseMonth', ' 3 ').draft;
  expect(changed.release).toEqual({ year: 2024, month: 3, day: 29 });
  expect(original.release).toEqual({ year: 2024, month: 2, day: 29 });
  expect(applyMovieDraftField(changed, 'releaseDay', '').draft.release).toEqual(
    {
      year: 2024,
      month: 3,
      day: null,
    },
  );
  expect(applyMovieDraftField({}, 'releaseYear', '2025').draft.release).toEqual(
    {
      year: 2025,
      month: null,
      day: null,
    },
  );
  expect(
    applyMovieDraftField(original, 'runtimeMinutes', '').draft.runtimeMinutes,
  ).toBeNull();
  expect(applyMovieDraftField(original, 'originalTitle', '')).toMatchObject({
    originalTitleTouched: true,
    draft: { originalTitle: '' },
  });
  expect(applyMovieDraftField(original, 'title', 'New title').draft.title).toBe(
    'New title',
  );
});

it('accepts valid optional metadata and reports invalid fields in form order', () => {
  expect(metadataErrors(movieToDraft(movie))).toEqual([]);
  expect(
    metadataErrors({
      release: { year: 2024, month: 13, day: 1 },
      runtimeMinutes: 100001,
      originalLanguage: 'bad_tag',
      synopsisLanguage: 'x'.repeat(36),
      countries: ['id'],
    }).map((error) => error.id),
  ).toEqual([
    'movie-releaseMonth',
    'movie-runtimeMinutes',
    'movie-originalLanguage',
    'movie-synopsisLanguage',
    'movie-countries',
  ]);
});
