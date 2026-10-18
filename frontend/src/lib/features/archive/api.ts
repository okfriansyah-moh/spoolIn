import type { GraphqlRequest, RequestOptions } from '../../transport/graphql';
import { mutation } from '../session/outcome';
import { record } from '../session/parse';
import type { ArchiveListItem, ArchiveTab } from './types';

const ARCHIVED_TITLES = `query ArchivedTitles($first: Int = 48) {
  managedTitles(state: ARCHIVED, first: $first) {
    nodes { id title state version }
  }
}`;

const ARCHIVED_PEOPLE = `query ArchivedPeople($first: Int = 48) {
  managedPeople(state: ARCHIVED, first: $first) {
    nodes { id name state version }
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
  request: GraphqlRequest,
  tab: ArchiveTab,
  csrf: string,
  signal?: AbortSignal,
): Promise<readonly ArchiveRow[]> {
  const query = tab === 'movies' ? ARCHIVED_TITLES : ARCHIVED_PEOPLE;
  const options: RequestOptions = { csrf };
  if (signal) options.signal = signal;
  const data = record(await request(query, { first: 48 }, options));
  const page = record(
    tab === 'movies' ? data.managedTitles : data.managedPeople,
  );
  const nodes = page.nodes;
  if (!Array.isArray(nodes)) throw new Error('Archives unavailable.');
  return Object.freeze(
    nodes.map((node) => {
      const row = node as Record<string, unknown>;
      return {
        id: String(row.id),
        label: String(tab === 'movies' ? row.title : row.name),
        state: String(row.state),
        version: String(row.version),
      };
    }),
  );
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
  return {
    token: String(impact.token),
    affectedCreditCount: Number(impact.affectedCreditCount),
    affectedTitles: Array.isArray(impact.affectedTitles)
      ? Object.freeze(impact.affectedTitles.map(String))
      : Object.freeze([]),
  };
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
    await request(
      MOVIE_LIFECYCLE,
      { ...input },
      { mutation: true, csrf },
    ),
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
    await request(
      PERSON_LIFECYCLE,
      { ...input },
      { mutation: true, csrf },
    ),
  );
  return lifecycleState(data.personLifecycle, input.operationId);
}

export class LifecycleFailure extends Error {
  constructor(readonly code: string) { super('Lifecycle change was not confirmed.'); }
}

function lifecycleState(value: unknown, expected: string): string {
  const receipt = mutation(value, expected);
  if (receipt.resultExpired) throw new LifecycleFailure('RESULT_EXPIRED');
  if (receipt.problem) throw new LifecycleFailure(receipt.problem.code);
  if (receipt.state === 'FAILED' || receipt.state === 'EXPIRED') throw new LifecycleFailure(receipt.state);
  return receipt.state;
}
