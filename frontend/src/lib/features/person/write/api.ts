import type { GraphqlRequest } from '../../../transport/graphql';
import { withCsrfAndSignal } from '../../../transport/request-options';
import { record } from '../../session/parse';
import type { ManagedPerson, PersonListRow } from './types';
import {
  personMutationOutcome,
  recoverPersonOperation,
  type PersonMutationOutcome,
} from './outcome';

const MANAGED_PEOPLE_QUERY = `query ManagedPeople($state: RecordState!, $first: Int = 24) {
  managedPeople(state: $state, first: $first) {
    nodes { id name state version biography }
    pageInfo { endCursor hasNextPage }
  }
}`;

const MANAGED_PERSON_QUERY = `query ManagedPerson($id: UUID!) {
  managedPerson(id: $id) {
    id name biography state version
  }
}`;

const CREATE_PERSON = `mutation CreatePerson($operationId: UUID!, $policyRevision: String!, $input: PersonInput!) {
  createPerson(operationId: $operationId, policyRevision: $policyRevision, input: $input) {
    operation { id state resultId version resultExpired problem { code } }
    problem { code fields { path message } }
  }
}`;

const UPDATE_PERSON = `mutation UpdatePerson(
  $operationId: UUID!
  $id: UUID!
  $expectedVersion: Long!
  $policyRevision: String!
  $patch: PersonPatch!
) {
  updatePerson(
    operationId: $operationId
    id: $id
    expectedVersion: $expectedVersion
    policyRevision: $policyRevision
    patch: $patch
  ) {
    operation { id state resultId version resultExpired problem { code } }
    problem { code fields { path message } }
  }
}`;

const CATALOG_OPERATION_QUERY = `query CatalogOperation($id: UUID!) {
  operation(id: $id, owner: CATALOG) {
    id state resultId version resultExpired problem { code }
  }
}`;

function toRow(node: Record<string, unknown>): PersonListRow {
  return Object.freeze({
    id: String(node.id),
    name: String(node.name),
    state: node.state === 'ARCHIVED' ? 'ARCHIVED' : 'ACTIVE',
  });
}

function toManaged(node: Record<string, unknown>): ManagedPerson {
  return Object.freeze({
    id: String(node.id),
    name: String(node.name),
    biography: typeof node.biography === 'string' ? node.biography : null,
    state: node.state === 'ARCHIVED' ? 'ARCHIVED' : 'ACTIVE',
    version: String(node.version),
  });
}

export async function fetchManagedPeople(
  request: GraphqlRequest,
  csrf: string,
  signal?: AbortSignal,
): Promise<readonly PersonListRow[]> {
  const data = record(
    await request(
      MANAGED_PEOPLE_QUERY,
      { state: 'ACTIVE' },
      withCsrfAndSignal(csrf, signal),
    ),
  );
  const page = record(data.managedPeople);
  const nodes = page.nodes;
  if (!Array.isArray(nodes)) throw new Error('Managed people unavailable.');
  return Object.freeze(
    nodes.map((node) => toRow(node as Record<string, unknown>)),
  );
}

export async function fetchManagedPerson(
  request: GraphqlRequest,
  id: string,
  csrf: string,
  signal?: AbortSignal,
): Promise<ManagedPerson | null> {
  const data = record(
    await request(MANAGED_PERSON_QUERY, { id }, withCsrfAndSignal(csrf, signal)),
  );
  const person = data.managedPerson;
  if (person === null) return null;
  return toManaged(person as Record<string, unknown>);
}

export async function createPerson(
  request: GraphqlRequest,
  operationId: string,
  policyRevision: string,
  input: { name: string; biography?: string },
  csrf: string,
  signal: AbortSignal,
): Promise<PersonMutationOutcome> {
  const data = record(
    await request(
      CREATE_PERSON,
      { operationId, policyRevision, input },
      { mutation: true, csrf, signal },
    ),
  );
  return personMutationOutcome(data.createPerson, operationId);
}

export async function updatePerson(
  request: GraphqlRequest,
  operationId: string,
  id: string,
  expectedVersion: string,
  policyRevision: string,
  patch: { name?: string; biography?: string | null },
  csrf: string,
  signal: AbortSignal,
): Promise<PersonMutationOutcome> {
  const data = record(
    await request(
      UPDATE_PERSON,
      {
        operationId,
        id,
        expectedVersion: Number(expectedVersion),
        policyRevision,
        patch,
      },
      { mutation: true, csrf, signal },
    ),
  );
  return personMutationOutcome(data.updatePerson, operationId);
}

export async function recoverCatalogOperation(
  request: GraphqlRequest,
  operationId: string,
  csrf: string,
  signal: AbortSignal,
) {
  const data = record(
    await request(
      CATALOG_OPERATION_QUERY,
      { id: operationId },
      { csrf, signal },
    ),
  );
  return recoverPersonOperation(data.operation, operationId);
}
