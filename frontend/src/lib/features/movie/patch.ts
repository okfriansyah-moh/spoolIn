import type { MovieDraft } from '../../generated/json-types';
import { trimOrdinary } from '../../policy/rules';
import type { ManagedMovie } from './types';

export type MovieInputPayload = Readonly<{
  title: string;
  originalTitle: string | null;
  synopsis: string;
  genreIds: readonly string[];
  release: { year: number; month?: number | null; day?: number | null };
  synopsisLanguage: string;
  countries: readonly string[];
}>;

export type MoviePatchPayload = Readonly<Record<string, unknown>>;

export function draftToCreateInput(draft: MovieDraft): MovieInputPayload {
  const year = draft.release?.year;
  if (year === null || year === undefined) throw new Error('Release year required.');
  const original =
    draft.originalTitle === undefined || draft.originalTitle === null
      ? null
      : trimOrdinary(draft.originalTitle) === ''
        ? null
        : trimOrdinary(draft.originalTitle);
  return Object.freeze({
    title: trimOrdinary(draft.title ?? ''),
    originalTitle: original,
    synopsis: trimOrdinary(draft.synopsis ?? ''),
    genreIds: Object.freeze([...(draft.genreIds ?? [])]),
    release: Object.freeze({
      year,
      month: draft.release?.month ?? null,
      day: draft.release?.day ?? null,
    }),
    synopsisLanguage: draft.synopsisLanguage?.trim() || 'und',
    countries: Object.freeze([...(draft.countries ?? [])]),
  });
}

export function draftToUpdatePatch(
  saved: ManagedMovie,
  draft: MovieDraft,
  originalTitleTouched: boolean,
): MoviePatchPayload {
  const patch: Record<string, unknown> = {};
  const title = trimOrdinary(draft.title ?? '');
  if (title !== saved.title) patch.title = title;
  const synopsis = trimOrdinary(draft.synopsis ?? '');
  if (synopsis !== saved.synopsis) patch.synopsis = synopsis;
  const draftGenres = [...(draft.genreIds ?? [])].sort().join(',');
  const savedGenres = [...saved.genreIds].sort().join(',');
  if (draftGenres !== savedGenres) patch.genreIds = [...(draft.genreIds ?? [])];
  const year = draft.release?.year;
  if (year !== null && year !== undefined && String(year) !== saved.releaseYear) {
    patch.release = {
      year,
      month: draft.release?.month ?? null,
      day: draft.release?.day ?? null,
    };
  }
  if (originalTitleTouched) {
    const raw = draft.originalTitle;
    if (raw === null || trimOrdinary(raw) === '') patch.originalTitle = null;
    else patch.originalTitle = trimOrdinary(raw);
  } else if (
    draft.originalTitle !== undefined &&
    draft.originalTitle !== null &&
    trimOrdinary(draft.originalTitle) !== (saved.originalTitle ?? '')
  ) {
    patch.originalTitle = trimOrdinary(draft.originalTitle);
  }
  return Object.freeze(patch);
}

export function patchIsEmpty(patch: MoviePatchPayload): boolean {
  return Object.keys(patch).length === 0;
}
