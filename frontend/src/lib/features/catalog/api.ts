/**
 * T013 – Catalog GraphQL transport.
 * Wraps the typed document nodes so callers never see raw query strings.
 */
import { readPageInfo } from '../../state/pagination';
import { TransportFailure, type GraphqlRequest } from '../../transport/graphql';
import { withSignal } from '../../transport/request-options';
import type {
  TitlesQuery,
  TitlesQueryVariables,
  TitleQuery,
  TitleQueryVariables,
  GenresQuery,
} from '../../generated/graphql';
import {
  TitlesDocument,
  TitleDocument,
  GenresDocument,
} from '../../generated/graphql';

// Raw query strings – imported from the shared all-operations.graphql
// so the transport layer receives the exact wire format.
const TITLES_QUERY = `query Titles($filter: SearchInput!, $first: Int, $after: Cursor) {
  titles(filter: $filter, first: $first, after: $after) {
    nodes {
      id kind state title originalTitle synopsis
      genres { id label }
      release { year month day precision }
      runtimeMinutes originalLanguage synopsisLanguage countries
      tvStatus
      tvEnd { year month day precision }
      releaseContext
      provenance { source externalId recordUrl retrievedAt reuseNote fieldSources locallyChanged }
      artwork { assetId url width height }
      credits {
        nodes { id personId personName titleId titleName role { id label category allowsCharacters } characters version }
        pageInfo { endCursor hasNextPage }
      }
      version
    }
    pageInfo { endCursor hasNextPage }
  }
}`;

const TITLE_QUERY = `query Title($id: UUID!) {
  title(id: $id) {
    id kind state title originalTitle synopsis
    genres { id label }
    release { year month day precision }
    runtimeMinutes originalLanguage synopsisLanguage countries
    tvStatus
    tvEnd { year month day precision }
    releaseContext
    provenance { source externalId recordUrl retrievedAt reuseNote fieldSources locallyChanged }
    artwork { assetId url width height }
    credits {
      nodes { id personId personName titleId titleName role { id label category allowsCharacters } characters version }
      pageInfo { endCursor hasNextPage }
    }
    version
  }
}`;

const GENRES_QUERY = `query Genres {
  genres { id label }
}`;

export type CatalogTitleNode = TitlesQuery['titles']['nodes'][number];
export type CatalogTitleDetail = NonNullable<TitleQuery['title']>;
export type GenreItem = GenresQuery['genres'][number];

function record(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Unexpected response shape');
  return Object.fromEntries(Object.entries(value));
}

async function readCatalog(
  request: GraphqlRequest,
  query: string,
  variables: Record<string, unknown>,
  signal?: AbortSignal,
): Promise<unknown> {
  try {
    return await request(query, variables, withSignal(signal));
  } catch (error) {
    if (
      signal?.aborted ||
      !(error instanceof TransportFailure) ||
      error.code !== 'UNAVAILABLE'
    )
      throw error;
    // A cold service or interrupted read may recover; never retry rejected access or rate limits.
    await new Promise((resolve) => globalThis.setTimeout(resolve, 150));
    if (signal?.aborted) throw error;
    return request(query, variables, withSignal(signal));
  }
}

export async function fetchTitles(
  request: GraphqlRequest,
  variables: TitlesQueryVariables,
  signal?: AbortSignal,
): Promise<TitlesQuery['titles']> {
  const data = record(
    await readCatalog(
      request,
      TITLES_QUERY,
      variables as Record<string, unknown>,
      signal,
    ),
  );
  return (data as { titles: TitlesQuery['titles'] }).titles;
}

export async function fetchTitle(
  request: GraphqlRequest,
  variables: TitleQueryVariables,
  signal?: AbortSignal,
): Promise<CatalogTitleDetail | null> {
  const data = record(
    await request(
      TITLE_QUERY,
      variables as Record<string, unknown>,
      withSignal(signal),
    ),
  );
  return (data as { title: CatalogTitleDetail | null }).title;
}

export async function fetchGenres(
  request: GraphqlRequest,
  signal?: AbortSignal,
): Promise<GenresQuery['genres']> {
  const data = record(await request(GENRES_QUERY, {}, withSignal(signal)));
  return (data as { genres: GenresQuery['genres'] }).genres;
}

// Re-export typed document nodes for tests that want to mock at the document level
export { TitlesDocument, TitleDocument, GenresDocument };

export async function fetchTitleCredits(
  request: GraphqlRequest,
  id: string,
  after: string | null,
  signal?: AbortSignal,
): Promise<CatalogTitleDetail['credits']> {
  const data = record(
    await request(
      `query TitleCredits($id: UUID!, $first: Int, $after: Cursor) {
    titleCredits(id: $id, first: $first, after: $after) {
      nodes { id personId personName titleId titleName role { id label category allowsCharacters } characters version }
      pageInfo { endCursor hasNextPage }
    }
  }`,
      { id, after },
      withSignal(signal),
    ),
  );
  const result = data.titleCredits as CatalogTitleDetail['credits'];
  if (!Array.isArray(result?.nodes)) throw new Error('Credits unavailable.');
  return { ...result, pageInfo: readPageInfo(result.pageInfo, after) };
}

export type CatalogFilterOptions = {
  genres: { id: string; label: string }[];
  releaseYears: { year: number; months: number[] }[];
};

export async function fetchCatalogFilterOptions(
  request: GraphqlRequest,
  signal?: AbortSignal,
): Promise<CatalogFilterOptions> {
  const data = record(
    await readCatalog(
      request,
      `query CatalogFilterOptions {
      catalogFilterOptions { genres { id label } releaseYears { year months } }
    }`,
      {},
      signal,
    ),
  );
  const options = record(data.catalogFilterOptions);
  if (!Array.isArray(options.genres) || !Array.isArray(options.releaseYears))
    throw new Error('Unexpected filter choices');
  return {
    genres: options.genres.map((value) => {
      const genre = record(value);
      if (typeof genre.id !== 'string' || typeof genre.label !== 'string')
        throw new Error('Unexpected genre choice');
      return { id: genre.id, label: genre.label };
    }),
    releaseYears: options.releaseYears.map((value) => {
      const date = record(value);
      if (
        typeof date.year !== 'number' ||
        !Number.isInteger(date.year) ||
        date.year < 1 ||
        date.year > 9999 ||
        !Array.isArray(date.months) ||
        !date.months.every(
          (month) => Number.isInteger(month) && month >= 1 && month <= 12,
        )
      )
        throw new Error('Unexpected release date choice');
      return { year: date.year, months: date.months };
    }),
  };
}
