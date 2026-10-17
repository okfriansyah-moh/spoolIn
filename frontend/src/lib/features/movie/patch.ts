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

function normalizedOriginalTitle(draft: MovieDraft): string | null {
  if (draft.originalTitle === undefined || draft.originalTitle === null) return null;
  const trimmed = trimOrdinary(draft.originalTitle);
  return trimmed === '' ? null : trimmed;
}

function releaseYear(draft: MovieDraft): number {
  const year = draft.release?.year;
  if (year === null || year === undefined) throw new Error('Release year required.');
  return year;
}

export function draftToCreateInput(draft: MovieDraft): MovieInputPayload {
  const year = releaseYear(draft);
  return Object.freeze({
    title: trimOrdinary(draft.title ?? ''),
    originalTitle: normalizedOriginalTitle(draft),
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
  applyTextAndGenrePatch(patch, saved, draft);
  applyReleasePatch(patch, saved, draft);
  applyOriginalTitlePatch(patch, saved, draft, originalTitleTouched);
  return Object.freeze(patch);
}

function applyTextAndGenrePatch(
  patch: Record<string, unknown>,
  saved: ManagedMovie,
  draft: MovieDraft,
): void {
  const title = trimOrdinary(draft.title ?? '');
  if (title !== saved.title) patch.title = title;
  const synopsis = trimOrdinary(draft.synopsis ?? '');
  if (synopsis !== saved.synopsis) patch.synopsis = synopsis;
  const draftGenres = [...(draft.genreIds ?? [])].sort().join(',');
  const savedGenres = [...saved.genreIds].sort().join(',');
  if (draftGenres !== savedGenres) patch.genreIds = [...(draft.genreIds ?? [])];
}

function applyReleasePatch(
  patch: Record<string, unknown>,
  saved: ManagedMovie,
  draft: MovieDraft,
): void {
  const year = draft.release?.year;
  if (year === null || year === undefined || String(year) === saved.releaseYear) return;
  patch.release = {
    year,
    month: draft.release?.month ?? null,
    day: draft.release?.day ?? null,
  };
}

function applyOriginalTitlePatch(
  patch: Record<string, unknown>,
  saved: ManagedMovie,
  draft: MovieDraft,
  originalTitleTouched: boolean,
): void {
  if (originalTitleTouched) {
    const raw = draft.originalTitle;
    patch.originalTitle =
      raw === null || raw === undefined || trimOrdinary(raw) === ''
        ? null
        : trimOrdinary(raw);
    return;
  }
  if (
    draft.originalTitle === undefined ||
    draft.originalTitle === null ||
    trimOrdinary(draft.originalTitle) === (saved.originalTitle ?? '')
  ) {
    return;
  }
  patch.originalTitle = trimOrdinary(draft.originalTitle);
}

export function patchIsEmpty(patch: MoviePatchPayload): boolean {
  return Object.keys(patch).length === 0;
}
