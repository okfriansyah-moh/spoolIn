import type { MovieDraft } from '../../generated/json-types';

export type MovieDraftField =
  | 'title'
  | 'synopsis'
  | 'originalTitle'
  | 'releaseYear'
  | 'releaseMonth'
  | 'releaseDay'
  | 'runtimeMinutes'
  | 'originalLanguage'
  | 'synopsisLanguage'
  | 'countries';

export type MovieDraftFieldUpdate = Readonly<{
  draft: MovieDraft;
  originalTitleTouched?: boolean;
}>;

function releaseFieldUpdate(
  draft: MovieDraft,
  field: 'releaseYear' | 'releaseMonth' | 'releaseDay',
  value: string,
): MovieDraftFieldUpdate {
  const key = {
    releaseYear: 'year',
    releaseMonth: 'month',
    releaseDay: 'day',
  }[field];
  return {
    draft: {
      ...draft,
      release: {
        year: draft.release?.year ?? null,
        month: draft.release?.month ?? null,
        day: draft.release?.day ?? null,
        [key]: value.trim() === '' ? null : Number(value),
      },
    },
  };
}

export function applyMovieDraftField(
  draft: MovieDraft,
  field: MovieDraftField,
  value: string,
): MovieDraftFieldUpdate {
  if (
    field === 'releaseYear' ||
    field === 'releaseMonth' ||
    field === 'releaseDay'
  ) {
    return releaseFieldUpdate(draft, field, value);
  }
  if (field === 'runtimeMinutes')
    return {
      draft: {
        ...draft,
        runtimeMinutes: value.trim() === '' ? null : Number(value),
      },
    };
  if (field === 'countries')
    return {
      draft: {
        ...draft,
        countries: value
          .split(',')
          .map((item) => item.trim())
          .filter(Boolean),
      },
    };
  if (field === 'originalTitle')
    return {
      draft: { ...draft, originalTitle: value },
      originalTitleTouched: true,
    };
  return { draft: { ...draft, [field]: value } };
}
