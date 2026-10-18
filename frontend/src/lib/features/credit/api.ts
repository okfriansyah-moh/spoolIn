import type { GraphqlRequest } from '../../transport/graphql';
import { withCsrfAndSignal, withSignal } from '../../transport/request-options';
import { record } from '../session/parse';
import { mutation, operation } from '../session/outcome';
import type { IdentityOutcome } from '../session/commands';
import type { CreditRoleOption, ManagedCredit, PersonOption } from './types';

const MANAGED_CREDITS = `query ManagedCredits($titleId: UUID!, $first: Int = 48) {
  managedCredits(titleId: $titleId, first: $first) {
    nodes {
      id personId personName version characters
      role { id label category allowsCharacters }
    }
  }
}`;

const CREDIT_ROLES = `query CreditRoles { creditRoles { id label category allowsCharacters } }`;

const MANAGED_PEOPLE = `query ManagedPeople($state: RecordState!, $first: Int = 48) {
  managedPeople(state: $state, first: $first) {
    nodes { id name state }
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

function mapCredit(node: Record<string, unknown>): ManagedCredit {
  const role = record(node.role);
  return Object.freeze({
    id: String(node.id),
    personId: String(node.personId),
    personName: String(node.personName),
    version: String(node.version),
    characters: Array.isArray(node.characters)
      ? node.characters.map(String)
      : [],
    role: Object.freeze({
      id: String(role.id),
      label: String(role.label),
      category: String(role.category),
      allowsCharacters: Boolean(role.allowsCharacters),
    }),
  });
}

export async function fetchTitleCredits(
  request: GraphqlRequest,
  titleId: string,
  csrf: string,
  signal?: AbortSignal,
): Promise<readonly ManagedCredit[]> {
  const data = record(
    await request(
      MANAGED_CREDITS,
      { titleId },
      withCsrfAndSignal(csrf, signal),
    ),
  );
  const page = record(data.managedCredits);
  const nodes = page.nodes;
  if (!Array.isArray(nodes)) return Object.freeze([]);
  return Object.freeze(
    nodes.map((node) => mapCredit(node as Record<string, unknown>)),
  );
}

export async function fetchCreditRoles(
  request: GraphqlRequest,
  signal?: AbortSignal,
): Promise<readonly CreditRoleOption[]> {
  const data = record(await request(CREDIT_ROLES, {}, withSignal(signal)));
  const roles = data.creditRoles;
  if (!Array.isArray(roles)) return Object.freeze([]);
  return Object.freeze(
    roles.map((role) => {
      const node = role as Record<string, unknown>;
      return Object.freeze({
        id: String(node.id),
        label: String(node.label),
        category: String(node.category),
        allowsCharacters: Boolean(node.allowsCharacters),
      });
    }),
  );
}

export async function fetchActivePeople(
  request: GraphqlRequest,
  csrf: string,
  signal?: AbortSignal,
): Promise<readonly PersonOption[]> {
  const data = record(
    await request(
      MANAGED_PEOPLE,
      { state: 'ACTIVE' },
      withCsrfAndSignal(csrf, signal),
    ),
  );
  const page = record(data.managedPeople);
  const nodes = page.nodes;
  if (!Array.isArray(nodes)) return Object.freeze([]);
  return Object.freeze(
    nodes.map((node) => {
      const row = node as Record<string, unknown>;
      return Object.freeze({ id: String(row.id), name: String(row.name) });
    }),
  );
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
  request: GraphqlRequest, document: string, field: string,
  variables: Record<string, unknown>, csrf: string, signal: AbortSignal,
): Promise<IdentityOutcome> {
  const data = record(await request(document, variables, { mutation: true, csrf, signal }));
  return mutation(data[field], String(variables.operationId));
}

export async function addCredit(
  request: GraphqlRequest, operationId: string, titleId: string,
  expectedVersion: string, policyRevision: string,
  input: { personId: string; roleId: string; characters: string[] },
  csrf: string, signal: AbortSignal,
): Promise<IdentityOutcome> {
  return writeCredit(request, ADD_CREDIT, 'addCredit', {
    operationId, titleId, expectedVersion, policyRevision, input,
  }, csrf, signal);
}

export async function updateCredit(
  request: GraphqlRequest, operationId: string, id: string,
  expectedVersion: string, policyRevision: string,
  patch: { roleId?: string; characters?: string[] }, csrf: string, signal: AbortSignal,
): Promise<IdentityOutcome> {
  return writeCredit(request, UPDATE_CREDIT, 'updateCredit', {
    operationId, id, expectedVersion, policyRevision, patch,
  }, csrf, signal);
}

export async function removeCredit(
  request: GraphqlRequest, operationId: string, id: string,
  expectedVersion: string, csrf: string, signal: AbortSignal,
): Promise<IdentityOutcome> {
  return writeCredit(request, REMOVE_CREDIT, 'removeCredit', {
    operationId, id, expectedVersion,
  }, csrf, signal);
}

export async function recoverCredit(
  request: GraphqlRequest, operationId: string, csrf: string, signal: AbortSignal,
): Promise<IdentityOutcome | null> {
  const document = `query CreditOperation($id: UUID!) {
    operation(id: $id, owner: CATALOG) {
      id state resultExpired problem { code retryAfterMs }
    }
  }`;
  const data = record(await request(document, { id: operationId }, withCsrfAndSignal(csrf, signal)));
  return data.operation === null ? null : operation(data.operation, operationId);
}
