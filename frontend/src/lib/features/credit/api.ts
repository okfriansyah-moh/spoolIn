import { readPageInfo, type PageSlice } from '../../state/pagination';
import { TransportFailure, type GraphqlRequest } from '../../transport/graphql';
import { withCsrfAndSignal, withSignal } from '../../transport/request-options';
import { record, uuid } from '../session/parse';
import { mutation, operation, safeProblem } from '../session/outcome';
import type { IdentityOutcome } from '../session/commands';
import type { CreditRoleOption, ManagedCredit, PersonOption } from './types';

export type CreditCommandOutcome =
  | IdentityOutcome
  | Readonly<{
      state: 'REJECTED';
      operationId: string;
      problem: NonNullable<IdentityOutcome['problem']>;
    }>;

const MANAGED_CREDITS = `query ManagedCredits($titleId: UUID!, $first: Int, $after: Cursor) {
  managedCredits(titleId: $titleId, first: $first, after: $after) {
    nodes {
      id personId personName version characters
      role { id label category allowsCharacters }
    }
    pageInfo { endCursor hasNextPage }
  }
}`;

const CREDIT_ROLES = `query CreditRoles { creditRoles { id label category allowsCharacters } }`;

const MANAGED_PEOPLE = `query ManagedPeople($state: RecordState!, $first: Int, $after: Cursor) {
  managedPeople(state: $state, first: $first, after: $after) {
    nodes { id name state }
    pageInfo { hasNextPage endCursor }
  }
}`;

const ADD_CREDIT = `mutation AddCredit(
  $operationId: UUID!
  $titleId: UUID!
  $expectedVersion: Long!
  $policyRevision: String!
  $input: CreditInput!
) {
  addCredit(
    operationId: $operationId
    titleId: $titleId
    expectedVersion: $expectedVersion
    policyRevision: $policyRevision
    input: $input
  ) {
    operation { id state resultId version resultExpired problem { code retryAfterMs } }
    problem { code retryAfterMs }
  }
}`;

function requiredText(value: unknown): string {
  if (typeof value !== 'string') throw new Error('Credit text unavailable.');
  return value;
}

function creditVersion(value: unknown): string {
  if (typeof value !== 'string' || !/^(0|[1-9][0-9]*)$/.test(value))
    throw new Error('Credit version unavailable.');
  if (BigInt(value) > 9223372036854775807n)
    throw new Error('Credit version unavailable.');
  return value;
}

function mapRole(value: unknown): CreditRoleOption {
  const role = record(value);
  if (
    (role.category !== 'CAST' && role.category !== 'CREATOR') ||
    typeof role.allowsCharacters !== 'boolean'
  )
    throw new Error('Credit role unavailable.');
  return Object.freeze({
    id: uuid(role.id),
    label: requiredText(role.label),
    category: role.category,
    allowsCharacters: role.allowsCharacters,
  });
}

function mapCredit(node: Record<string, unknown>): ManagedCredit {
  if (
    !Array.isArray(node.characters) ||
    !node.characters.every((name) => typeof name === 'string')
  )
    throw new Error('Credit character names unavailable.');
  return Object.freeze({
    id: uuid(node.id),
    personId: uuid(node.personId),
    personName: requiredText(node.personName),
    version: creditVersion(node.version),
    characters: Object.freeze([...node.characters]),
    role: mapRole(node.role),
  });
}

export async function fetchTitleCredits(
  request: GraphqlRequest,
  titleId: string,
  csrf: string,
  signal?: AbortSignal,
): Promise<readonly ManagedCredit[]> {
  return (await fetchTitleCreditsPage(request, titleId, csrf, null, signal))
    .nodes;
}

async function currentCreditPerson(
  request: GraphqlRequest,
  id: string,
  csrf: string,
  signal?: AbortSignal,
): Promise<boolean | undefined> {
  const data = record(
    await request(
      `query ManagedCreditPerson($id: UUID!) {
    managedPerson(id: $id) { id state }
  }`,
      { id },
      withCsrfAndSignal(csrf, signal),
    ),
  );
  if (data.managedPerson === null) return undefined;
  const person = record(data.managedPerson);
  if (uuid(person.id) !== id)
    throw new Error('Current credit person identity unavailable.');
  if (person.state === 'ACTIVE') return false;
  if (person.state === 'ARCHIVED') return true;
  throw new Error('Current credit person state unavailable.');
}

async function withCurrentPeople(
  request: GraphqlRequest,
  nodes: readonly ManagedCredit[],
  csrf: string,
  signal?: AbortSignal,
): Promise<readonly ManagedCredit[]> {
  const states = new Map<string, boolean | undefined>();
  for (const id of new Set(nodes.map((credit) => credit.personId))) {
    if (signal?.aborted) throw new Error('Credit reference read cancelled.');
    try {
      states.set(id, await currentCreditPerson(request, id, csrf, signal));
    } catch (error) {
      if (error instanceof TransportFailure && error.code === 'RATE_LIMITED')
        throw error;
      states.set(id, undefined);
    }
  }
  if (signal?.aborted) throw new Error('Credit reference read cancelled.');
  return Object.freeze(
    nodes.map((credit) => {
      const personArchived = states.get(credit.personId);
      return Object.freeze({
        ...credit,
        ...(personArchived === undefined ? {} : { personArchived }),
      });
    }),
  );
}

export async function fetchTitleCreditsPage(
  request: GraphqlRequest,
  titleId: string,
  csrf: string,
  after: string | null,
  signal?: AbortSignal,
): Promise<PageSlice<ManagedCredit>> {
  const data = record(
    await request(
      MANAGED_CREDITS,
      { titleId, after },
      withCsrfAndSignal(csrf, signal),
    ),
  );
  const page = record(data.managedCredits);
  if (!Array.isArray(page.nodes)) throw new Error('Credit data unavailable.');
  const pageInfo = readPageInfo(page.pageInfo, after);
  const nodes = page.nodes.map((node) => mapCredit(record(node)));
  return Object.freeze({
    nodes: await withCurrentPeople(request, nodes, csrf, signal),
    pageInfo,
  });
}

export async function fetchCreditRoles(
  request: GraphqlRequest,
  signal?: AbortSignal,
): Promise<readonly CreditRoleOption[]> {
  const data = record(await request(CREDIT_ROLES, {}, withSignal(signal)));
  const roles = data.creditRoles;
  if (!Array.isArray(roles)) throw new Error('Credit roles unavailable.');
  return Object.freeze(roles.map(mapRole));
}

export async function fetchActivePeople(
  request: GraphqlRequest,
  csrf: string,
  signal?: AbortSignal,
): Promise<readonly PersonOption[]> {
  return (await fetchActivePeoplePage(request, csrf, null, signal)).nodes;
}

export async function fetchActivePeoplePage(
  request: GraphqlRequest,
  csrf: string,
  after: string | null,
  signal?: AbortSignal,
): Promise<Readonly<{ nodes: readonly PersonOption[]; next: string | null }>> {
  const data = record(
    await request(
      MANAGED_PEOPLE,
      { state: 'ACTIVE', after },
      withCsrfAndSignal(csrf, signal),
    ),
  );
  const page = record(data.managedPeople);
  if (!Array.isArray(page.nodes)) throw new Error('Credit people unavailable.');
  const info = record(page.pageInfo);
  if (typeof info.hasNextPage !== 'boolean')
    throw new Error('Credit people page unavailable.');
  const next = info.hasNextPage ? info.endCursor : null;
  if (info.hasNextPage && (typeof next !== 'string' || !next || next === after))
    throw new Error('Credit people cursor unavailable.');
  const nodes = page.nodes.map((node) => {
    const row = record(node);
    return Object.freeze({ id: uuid(row.id), name: requiredText(row.name) });
  });
  if (new Set(nodes.map((person) => person.id)).size !== nodes.length)
    throw new Error('Duplicate credit people page.');
  return Object.freeze({
    nodes: Object.freeze(nodes),
    next: next as string | null,
  });
}

const UPDATE_CREDIT = `mutation UpdateCredit(
  $operationId: UUID!, $id: UUID!, $expectedVersion: Long!,
  $policyRevision: String!, $patch: CreditPatch!
) {
  updateCredit(operationId: $operationId, id: $id, expectedVersion: $expectedVersion,
    policyRevision: $policyRevision, patch: $patch) {
    operation { id state resultExpired problem { code retryAfterMs } }
    problem { code retryAfterMs }
  }
}`;

const REMOVE_CREDIT = `mutation RemoveCredit($operationId: UUID!, $id: UUID!, $expectedVersion: Long!) {
  removeCredit(operationId: $operationId, id: $id, expectedVersion: $expectedVersion) {
    operation { id state resultExpired problem { code retryAfterMs } }
    problem { code retryAfterMs }
  }
}`;

async function writeCredit(
  request: GraphqlRequest,
  document: string,
  field: string,
  variables: Record<string, unknown>,
  csrf: string,
  signal: AbortSignal,
): Promise<CreditCommandOutcome> {
  const data = record(
    await request(document, variables, { mutation: true, csrf, signal }),
  );
  const result = record(data[field]);
  if (result.operation === null) {
    const problem = safeProblem(result.problem);
    if (!problem) throw new Error('Credit rejection unavailable.');
    return Object.freeze({
      state: 'REJECTED',
      operationId: String(variables.operationId),
      problem,
    });
  }
  return mutation(result, String(variables.operationId));
}

export async function addCredit(
  request: GraphqlRequest,
  operationId: string,
  titleId: string,
  expectedVersion: string,
  policyRevision: string,
  input: { personId: string; roleId: string; characters: string[] },
  csrf: string,
  signal: AbortSignal,
): Promise<CreditCommandOutcome> {
  return writeCredit(
    request,
    ADD_CREDIT,
    'addCredit',
    {
      operationId,
      titleId,
      expectedVersion,
      policyRevision,
      input,
    },
    csrf,
    signal,
  );
}

export async function updateCredit(
  request: GraphqlRequest,
  operationId: string,
  id: string,
  expectedVersion: string,
  policyRevision: string,
  patch: { roleId?: string; characters?: string[] },
  csrf: string,
  signal: AbortSignal,
): Promise<CreditCommandOutcome> {
  return writeCredit(
    request,
    UPDATE_CREDIT,
    'updateCredit',
    {
      operationId,
      id,
      expectedVersion,
      policyRevision,
      patch,
    },
    csrf,
    signal,
  );
}

export async function removeCredit(
  request: GraphqlRequest,
  operationId: string,
  id: string,
  expectedVersion: string,
  csrf: string,
  signal: AbortSignal,
): Promise<CreditCommandOutcome> {
  return writeCredit(
    request,
    REMOVE_CREDIT,
    'removeCredit',
    {
      operationId,
      id,
      expectedVersion,
    },
    csrf,
    signal,
  );
}

export async function recoverCredit(
  request: GraphqlRequest,
  operationId: string,
  csrf: string,
  signal: AbortSignal,
): Promise<
  | (IdentityOutcome &
      Readonly<{ resultId: string | null; version: string | null }>)
  | null
> {
  const document = `query CreditOperation($id: UUID!) {
    operation(id: $id, owner: CATALOG) {
      id state resultId version resultExpired problem { code retryAfterMs }
    }
  }`;
  const data = record(
    await request(
      document,
      { id: operationId },
      withCsrfAndSignal(csrf, signal),
    ),
  );
  if (data.operation === null) return null;
  const raw = record(data.operation);
  const receipt = operation(raw, operationId);
  const resultId = typeof raw.resultId === 'string' ? raw.resultId : null;
  const version =
    typeof raw.version === 'string' &&
    /^(0|[1-9][0-9]*)$/.test(raw.version) &&
    BigInt(raw.version) <= 9223372036854775807n
      ? raw.version
      : null;
  return Object.freeze({ ...receipt, resultId, version });
}
