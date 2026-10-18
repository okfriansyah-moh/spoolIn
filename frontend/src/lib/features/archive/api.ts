import type { GraphqlRequest, RequestOptions } from '../../transport/graphql';
import { mutation } from '../session/outcome';
import { readPageInfo, type PageSlice } from '../../state/pagination';
import { record } from '../session/parse';
import type { ArchiveListItem, ArchiveTab } from './types';

const ARCHIVED_TITLES = `query ArchivedTitles($first: Int = 48, $after: Cursor) {
  managedTitles(state: ARCHIVED, first: $first, after: $after) {
    nodes { id title state version }
    pageInfo { endCursor hasNextPage }
  }
}`;

const ARCHIVED_PEOPLE = `query ArchivedPeople($first: Int = 48, $after: Cursor) {
  managedPeople(state: ARCHIVED, first: $first, after: $after) {
    nodes { id name state version }
    pageInfo { endCursor hasNextPage }
  }
}`;

const DELETION_IMPACT = `query DeletionImpact($personId: UUID!) {
  deletionImpact(personId: $personId) {
    targetId token affectedCreditCount affectedTitles
  }
}`;

const MOVIE_LIFECYCLE = `mutation MovieLifecycle(
  $operationId: UUID!
  $id: UUID!
  $expectedVersion: Long!
  $action: LifecycleAction!
  $confirmed: Boolean!
) {
  movieLifecycle(
    operationId: $operationId
    id: $id
    expectedVersion: $expectedVersion
    action: $action
    confirmed: $confirmed
  ) {
    operation { id state resultExpired problem { code } }
    problem { code }
  }
}`;

const PERSON_LIFECYCLE = `mutation PersonLifecycle(
  $operationId: UUID!
  $id: UUID!
  $expectedVersion: Long!
  $action: LifecycleAction!
  $impactToken: String
  $confirmed: Boolean!
) {
  personLifecycle(
    operationId: $operationId
    id: $id
    expectedVersion: $expectedVersion
    action: $action
    impactToken: $impactToken
    confirmed: $confirmed
  ) {
    operation { id state resultExpired problem { code } }
    problem { code }
  }
}`;

export type ArchiveRow = ArchiveListItem;

export async function fetchArchived(
  request: GraphqlRequest, tab: ArchiveTab, csrf: string, signal?: AbortSignal,
): Promise<readonly ArchiveRow[]> {
  return (await fetchArchivedPage(request, tab, csrf, null, signal)).nodes;
}

function archiveRow(node: unknown, tab: ArchiveTab): ArchiveRow {
  const row = record(node);
  return Object.freeze({ id: String(row.id), label: String(tab === 'movies' ? row.title : row.name),
    state: String(row.state), version: String(row.version) });
}

export async function fetchArchivedPage(
  request: GraphqlRequest, tab: ArchiveTab, csrf: string, after: string | null, signal?: AbortSignal,
): Promise<PageSlice<ArchiveRow>> {
  const query = tab === 'movies' ? ARCHIVED_TITLES : ARCHIVED_PEOPLE;
  const options: RequestOptions = { csrf };
  if (signal) options.signal = signal;
  const data = record(await request(query, { first: 48, after }, options));
  const page = record(tab === 'movies' ? data.managedTitles : data.managedPeople);
  if (!Array.isArray(page.nodes)) throw new Error('Archives unavailable.');
  return Object.freeze({ nodes: Object.freeze(page.nodes.map((node) => archiveRow(node, tab))),
    pageInfo: readPageInfo(page.pageInfo, after) });
}

export async function fetchDeletionImpact(
  request: GraphqlRequest,
  personId: string,
  csrf: string,
): Promise<{
  token: string;
  affectedCreditCount: number;
  affectedTitles: readonly string[];
}> {
  const data = record(await request(DELETION_IMPACT, { personId }, { csrf }));
  const impact = record(data.deletionImpact);
  if (
    typeof impact.token !== 'string' ||
    !impact.token ||
    impact.token.length > 512 ||
    typeof impact.affectedCreditCount !== 'number' ||
    !Number.isInteger(impact.affectedCreditCount) ||
    impact.affectedCreditCount < 0 ||
    impact.affectedCreditCount > 2147483647 ||
    !Array.isArray(impact.affectedTitles) ||
    !impact.affectedTitles.every(
      (title) => typeof title === 'string' && title.length > 0,
    )
  ) {
    throw new Error('Complete deletion impact unavailable.');
  }
  return Object.freeze({
    token: impact.token,
    affectedCreditCount: impact.affectedCreditCount,
    affectedTitles: Object.freeze(impact.affectedTitles as string[]),
  });
}

export async function runMovieLifecycle(
  request: GraphqlRequest,
  csrf: string,
  input: {
    operationId: string;
    id: string;
    expectedVersion: string;
    action: 'ARCHIVE' | 'RESTORE' | 'DELETE';
    confirmed: boolean;
  },
): Promise<string> {
  const data = record(
    await request(MOVIE_LIFECYCLE, { ...input }, { mutation: true, csrf }),
  );
  return lifecycleState(data.movieLifecycle, input.operationId);
}

export async function runPersonLifecycle(
  request: GraphqlRequest,
  csrf: string,
  input: {
    operationId: string;
    id: string;
    expectedVersion: string;
    action: 'ARCHIVE' | 'RESTORE' | 'DELETE';
    impactToken?: string;
    confirmed: boolean;
  },
): Promise<string> {
  const data = record(
    await request(PERSON_LIFECYCLE, { ...input }, { mutation: true, csrf }),
  );
  return lifecycleState(data.personLifecycle, input.operationId);
}

export class LifecycleFailure extends Error {
  constructor(readonly code: string) {
    super('Lifecycle change was not confirmed.');
  }
}

function lifecycleState(value: unknown, expected: string): string {
  const receipt = mutation(value, expected);
  if (receipt.resultExpired) throw new LifecycleFailure('RESULT_EXPIRED');
  if (receipt.problem) throw new LifecycleFailure(receipt.problem.code);
  if (receipt.state === 'FAILED' || receipt.state === 'EXPIRED')
    throw new LifecycleFailure(receipt.state);
  return receipt.state;
}
