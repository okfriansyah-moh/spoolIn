import { describe, expect, it } from 'vitest';
import { draftToCreateInput, draftToUpdatePatch } from './patch';
import type { ManagedMovie } from './types';

const saved: ManagedMovie = Object.freeze({
  id: '190db10d-02b8-437e-8e7c-ece9c105fe2a',
  kind: 'MOVIE',
  state: 'ACTIVE',
  title: 'Harbor Story',
  originalTitle: 'Cahaya',
  synopsis: 'Original synopsis',
  genreIds: Object.freeze(['10000000-0000-4000-8000-000000000010']),
  releaseYear: '2024',
  version: '5',
  artworkUrl: null,
  artworkUnavailable: false,
});

describe('T017 movie patch', () => {
  it('builds create input with unknown optional fields omitted as null', () => {
    const input = draftToCreateInput({
      title: 'Harbor Story',
      synopsis: 'A pilot returns home.',
      genreIds: ['10000000-0000-4000-8000-000000000010'],
      release: { year: 2024, month: 10, day: null },
    });
    expect(input.originalTitle).toBeNull();
    expect(input.release.year).toBe(2024);
    expect(input.release.month).toBe(10);
  });

  it('clears optional originalTitle when explicitly touched to blank', () => {
    const patch = draftToUpdatePatch(
      saved,
      {
        title: 'Harbor Story',
        originalTitle: '',
        synopsis: 'Original synopsis',
        genreIds: [...saved.genreIds],
        release: { year: 2024, month: null, day: null },
      },
      true,
    );
    expect(patch.originalTitle).toBeNull();
    expect(patch.title).toBeUndefined();
  });

  it('omits unchanged optional originalTitle when not touched', () => {
    const patch = draftToUpdatePatch(
      saved,
      {
        title: 'Harbor Story',
        synopsis: 'Revised synopsis',
        genreIds: [...saved.genreIds],
        release: { year: 2024, month: null, day: null },
      },
      false,
    );
    expect(patch.synopsis).toBe('Revised synopsis');
    expect(patch.originalTitle).toBeUndefined();
  });
});
