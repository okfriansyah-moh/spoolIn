import type { MovieDraft } from '../../generated/json-types';
import type { ManagedMovie, ManagedTitleRow } from './types';

type WireTitle = Readonly<{
  id: string;
  kind: string;
  state: string;
  title: string;
  originalTitle?: string | null;
  synopsis?: string;
  genres?: ReadonlyArray<{ id: string; label: string }>;
  release?: {
    year?: number | null;
    month?: number | null;
    day?: number | null;
  };
  releaseContext?: string | null;
  runtimeMinutes?: number | null;
  originalLanguage?: string | null;
  synopsisLanguage?: string;
  countries?: readonly string[];
  version: string;
  artwork?: { url?: string | null } | null;
}>;

export function toManagedRow(node: WireTitle): ManagedTitleRow {
  return Object.freeze({
    id: node.id,
    kind: node.kind === 'TV' ? 'TV' : 'MOVIE',
    state: node.state === 'ARCHIVED' ? 'ARCHIVED' : 'ACTIVE',
    title: node.title,
    releaseContext: node.releaseContext ?? formatRelease(node.release),
    version: node.version,
  });
}

function recordKind(kind: string): 'MOVIE' | 'TV' {
  return kind === 'TV' ? 'TV' : 'MOVIE';
}

function recordState(state: string): 'ACTIVE' | 'ARCHIVED' {
  return state === 'ARCHIVED' ? 'ARCHIVED' : 'ACTIVE';
}

export function toManagedMovie(
  node: WireTitle,
  artworkUnavailable = false,
): ManagedMovie {
  const year = node.release?.year;
  return Object.freeze({
    id: node.id,
    kind: recordKind(node.kind),
    state: recordState(node.state),
    title: node.title,
    originalTitle: node.originalTitle ?? null,
    synopsis: node.synopsis ?? '',
    genreIds: Object.freeze((node.genres ?? []).map((genre) => genre.id)),
    releaseYear: year === null || year === undefined ? '' : String(year),
    releaseMonth: node.release?.month ?? null,
    releaseDay: node.release?.day ?? null,
    runtimeMinutes: node.runtimeMinutes ?? null,
    originalLanguage: node.originalLanguage ?? null,
    synopsisLanguage: node.synopsisLanguage ?? 'und',
    countries: Object.freeze([...(node.countries ?? [])]),
    version: node.version,
    artworkUrl: node.artwork?.url ?? null,
    artworkUnavailable,
  });
}

export function movieToDraft(movie: ManagedMovie): MovieDraft {
  return Object.freeze({
    title: movie.title,
    originalTitle: movie.originalTitle,
    synopsis: movie.synopsis,
    genreIds: Object.freeze([...movie.genreIds]),
    release: Object.freeze({
      year: movie.releaseYear ? Number(movie.releaseYear) : null,
      month: movie.releaseMonth ?? null,
      day: movie.releaseDay ?? null,
    }),
    runtimeMinutes: movie.runtimeMinutes ?? null,
    originalLanguage: movie.originalLanguage ?? null,
    synopsisLanguage: movie.synopsisLanguage ?? 'und',
    countries: Object.freeze([...(movie.countries ?? [])]),
  });
}

function formatRelease(release: WireTitle['release']): string {
  if (!release?.year) return 'Release unknown';
  if (release.month)
    return `${release.year}-${String(release.month).padStart(2, '0')}`;
  return String(release.year);
}
