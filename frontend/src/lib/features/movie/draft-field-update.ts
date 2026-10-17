import type { MovieDraft } from '../../generated/json-types';

export type MovieDraftField =
  'title' | 'synopsis' | 'originalTitle' | 'releaseYear';

export type MovieDraftFieldUpdate = Readonly<{
  draft: MovieDraft;
  originalTitleTouched?: boolean;
}>;

export function applyMovieDraftField(
  draft: MovieDraft,
  field: MovieDraftField,
  value: string,
): MovieDraftFieldUpdate {
  const next = { ...draft };
  if (field === 'releaseYear') {
    const year = value.trim() === '' ? null : Number(value);
    next.release = Object.freeze({
      ...(next.release ?? {}),
      year: Number.isInteger(year) ? year : null,
      month: next.release?.month ?? null,
      day: next.release?.day ?? null,
    });
    return { draft: next };
  }
  if (field === 'originalTitle') {
    next.originalTitle = value;
    return { draft: next, originalTitleTouched: true };
  }
  next[field] = value;
  return { draft: next };
}
