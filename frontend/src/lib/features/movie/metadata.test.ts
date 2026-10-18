import { describe, expect, it } from 'vitest';
import { applyMovieDraftField } from './draft-field-update';
import { draftToCreateInput, draftToUpdatePatch } from './patch';
import { movieToDraft, toManagedMovie } from './projection';
import { metadataErrors, rebaseMovieMetadata } from './metadata';

const movie = toManagedMovie({ id: 'title', kind: 'MOVIE', state: 'ACTIVE', version: '7',
  title: 'Loaded title', synopsis: 'Loaded synopsis', originalTitle: 'Original', genres: [],
  release: { year: 2024, month: 2, day: 29 }, runtimeMinutes: 123,
  originalLanguage: 'id', synopsisLanguage: 'en-GB', countries: ['ID', 'GB'] });

describe('movie metadata production projection and write paths', () => {
  it('retains every loaded date, language, runtime and country through draft and create conversion', () => {
    const draft = movieToDraft(movie);
    expect(draftToCreateInput(draft)).toMatchObject({ release: { year: 2024, month: 2, day: 29 },
      runtimeMinutes: 123, originalLanguage: 'id', synopsisLanguage: 'en-GB', countries: ['ID', 'GB'] });
    expect(draftToUpdatePatch(movie, draft, false)).toEqual({});
  });
  it('writes month/day changes with unchanged year and explicitly clears optional values', () => {
    const draft = { ...movieToDraft(movie), release: { year: 2024, month: 3, day: null },
      runtimeMinutes: null, originalLanguage: '', countries: [] };
    expect(draftToUpdatePatch(movie, draft, false)).toEqual({
      release: { year: 2024, month: 3, day: null }, runtimeMinutes: null, originalLanguage: null, countries: [] });
  });
  it('rebases untouched metadata but preserves changed date precision and deliberate clears', () => {
    const base = movieToDraft(movie);
    const latest = { ...base, runtimeMinutes: 124, countries: ['FR'], synopsisLanguage: 'fr' };
    const draft = { ...base, originalLanguage: '', release: { year: 2024, month: 2, day: null } };
    expect(rebaseMovieMetadata(base, draft, latest)).toMatchObject({ runtimeMinutes: 124,
      countries: ['FR'], synopsisLanguage: 'fr', originalLanguage: '', release: { year: 2024, month: 2, day: null } });
  });
  it('rejects impossible precision and invalid runtime before submission instead of silently clearing', () => {
    expect(metadataErrors({ release: { year: 2023, month: 2, day: 29 }, runtimeMinutes: 0 }).map(x => x.id))
      .toEqual(['movie-releaseDay', 'movie-runtimeMinutes']);
    expect(metadataErrors({ release: { year: 2024, month: null, day: 1 } })).toHaveLength(1);
    const draft = applyMovieDraftField({}, 'runtimeMinutes', 'invalid').draft;
    expect(metadataErrors(draft)).toHaveLength(1);
    expect(applyMovieDraftField({}, 'countries', 'ID, GB').draft.countries).toEqual(['ID', 'GB']);
  });
});
