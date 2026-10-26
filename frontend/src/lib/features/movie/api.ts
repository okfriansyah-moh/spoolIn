import { readPageInfo, type PageSlice } from '../../state/pagination';
import type { GraphqlRequest } from '../../transport/graphql';
import { withCsrfAndSignal } from '../../transport/request-options';
import { record } from '../session/parse';
import { fetchGenres } from '../catalog/api';
import type { GenreOption, ManagedMovie, ManagedTitleRow } from './types';
import {
  movieMutationOutcome,
  type MovieMutationOutcome,
} from './outcome';
import { toManagedMovie, toManagedRow } from './projection';
import type { MovieInputPayload, MoviePatchPayload } from './patch';


const MANAGED_TITLES_QUERY = `query ManagedTitles($state: RecordState!, $first: Int, $after: Cursor) {
  managedTitles(state: $state, first: $first, after: $after) {
    nodes {
      id kind state title releaseContext version
      release { year month day }
    }
    pageInfo { endCursor hasNextPage }
  }
}`;

const MANAGED_TITLE_QUERY = `query ManagedTitle($id: UUID!) {
  managedTitle(id: $id) {
    id kind state title originalTitle synopsis
    runtimeMinutes originalLanguage synopsisLanguage countries
    genres { id label }
    release { year month day }
    releaseContext
    artwork { assetId url width height }
    version
  }
}`;

const CREATE_MOVIE_MUTATION = `mutation CreateMovie($operationId: UUID!, $policyRevision: String!, $input: MovieInput!) {
  createMovie(operationId: $operationId, policyRevision: $policyRevision, input: $input) {
    operation { id state resultId version resultExpired problem { code retryAfterMs } }
    problem { code retryAfterMs fields { path message } }
  }
}`;

const UPDATE_MOVIE_MUTATION = `mutation UpdateMovie(
  $operationId: UUID!
  $id: UUID!
  $expectedVersion: Long!
  $policyRevision: String!
  $patch: MoviePatch!
) {
  updateMovie(
    operationId: $operationId
    id: $id
    expectedVersion: $expectedVersion
    policyRevision: $policyRevision
    patch: $patch
  ) {
    operation { id state resultId version resultExpired problem { code retryAfterMs } }
    problem { code retryAfterMs fields { path message } }
  }
}`;

export async function fetchManagedTitlesPage(
  request: GraphqlRequest,
  csrf: string,
  after: string | null,
  signal?: AbortSignal,
): Promise<PageSlice<ManagedTitleRow>> {
  const data = record(
    await request(
      MANAGED_TITLES_QUERY,
      { state: 'ACTIVE', after },
      withCsrfAndSignal(csrf, signal),
    ),
  );
  const page = record(data.managedTitles);
  if (!Array.isArray(page.nodes))
    throw new Error('Managed titles unavailable.');
  return Object.freeze({
    nodes: Object.freeze(
      page.nodes.map((node) =>
        toManagedRow(node as Parameters<typeof toManagedRow>[0]),
      ),
    ),
    pageInfo: readPageInfo(page.pageInfo, after),
  });
}

export async function fetchManagedMovie(
  request: GraphqlRequest,
  id: string,
  csrf: string,
  signal?: AbortSignal,
): Promise<ManagedMovie | null> {
  const data = record(
    await request(MANAGED_TITLE_QUERY, { id }, withCsrfAndSignal(csrf, signal)),
  );
  const title = data.managedTitle;
  if (title === null) return null;
  return toManagedMovie(title as Parameters<typeof toManagedMovie>[0]);
}

export async function fetchMovieGenres(
  request: GraphqlRequest,
  signal?: AbortSignal,
): Promise<readonly GenreOption[]> {
  const genres = await fetchGenres(request, signal);
  return Object.freeze(
    genres.map((genre) => Object.freeze({ id: genre.id, label: genre.label })),
  );
}

export async function createMovie(
  request: GraphqlRequest,
  operationId: string,
  policyRevision: string,
  input: MovieInputPayload,
  csrf: string,
  signal: AbortSignal,
): Promise<MovieMutationOutcome> {
  const data = record(
    await request(
      CREATE_MOVIE_MUTATION,
      { operationId, policyRevision, input },
      { mutation: true, csrf, signal },
    ),
  );
  return movieMutationOutcome(data.createMovie, operationId);
}

export async function updateMovie(
  request: GraphqlRequest,
  operationId: string,
  id: string,
  expectedVersion: string,
  policyRevision: string,
  patch: MoviePatchPayload,
  csrf: string,
  signal: AbortSignal,
): Promise<MovieMutationOutcome> {
  const data = record(
    await request(
      UPDATE_MOVIE_MUTATION,
      {
        operationId,
        id,
        expectedVersion,
        policyRevision,
        patch,
      },
      { mutation: true, csrf, signal },
    ),
  );
  return movieMutationOutcome(data.updateMovie, operationId);
}

export { recoverCatalogOperation, type RecoveredCatalogOperation } from '../editing/operation-result';
