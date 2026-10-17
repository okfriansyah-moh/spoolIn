import type { GraphqlRequest } from '../../transport/graphql';
import { record } from '../session/parse';
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
    operation { id state resultId version problem { code } }
    problem { code message }
  }
}`;

function mapCredit(node: Record<string, unknown>): ManagedCredit {
  const role = record(node.role);
  return Object.freeze({
    id: String(node.id),
    personId: String(node.personId),
    personName: String(node.personName),
    version: String(node.version),
    characters: Array.isArray(node.characters) ? node.characters.map(String) : [],
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
  const data = record(await request(MANAGED_CREDITS, { titleId }, { csrf, signal }));
  const page = record(data.managedCredits);
  const nodes = page.nodes;
  if (!Array.isArray(nodes)) return Object.freeze([]);
  return Object.freeze(nodes.map((node) => mapCredit(node as Record<string, unknown>)));
}

export async function fetchCreditRoles(
  request: GraphqlRequest,
  signal?: AbortSignal,
): Promise<readonly CreditRoleOption[]> {
  const data = record(await request(CREDIT_ROLES, {}, { signal }));
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
  const data = record(await request(MANAGED_PEOPLE, { state: 'ACTIVE' }, { csrf, signal }));
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

export async function addCredit(
  request: GraphqlRequest,
  operationId: string,
  titleId: string,
  expectedVersion: string,
  policyRevision: string,
  input: { personId: string; roleId: string; characters: string[] },
  csrf: string,
  signal: AbortSignal,
): Promise<string | null> {
  const data = record(
    await request(
      ADD_CREDIT,
      {
        operationId,
        titleId,
        expectedVersion: Number(expectedVersion),
        policyRevision,
        input,
      },
      { mutation: true, csrf, signal },
    ),
  );
  const result = record(data.addCredit);
  const problem = result.problem;
  if (problem && typeof problem === 'object') {
    const code = (problem as Record<string, unknown>).code;
    if (code === 'CREDIT_DUPLICATE' || code === 'ALREADY_EXISTS') return 'Review the credit — no duplicate added.';
  }
  const operation = record(result.operation);
  if (operation.state === 'FAILED') return 'Changes were not saved.';
  return null;
}
