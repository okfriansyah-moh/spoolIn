import type { GraphqlRequest } from '../../transport/graphql';
import { mutation, operation, safeProblem } from '../session/outcome';
import { record, uuid } from '../session/parse';
import type { IdentityOutcome } from '../session/commands';

export type RoleIntent = Readonly<{
  operationId: string;
  category: 'CAST' | 'CREATOR';
  label: string;
  policyRevision: string;
}>;
export type RoleOutcome =
  | (IdentityOutcome & Readonly<{ resultId: string | null }>)
  | Readonly<{
      state: 'REJECTED';
      operationId: string;
      problem: NonNullable<IdentityOutcome['problem']>;
    }>;

function outcome(value: unknown, id: string, envelope: boolean): RoleOutcome {
  const raw = record(value);
  if (envelope && raw.operation === null) {
    const problem = safeProblem(raw.problem);
    if (!problem) throw new Error('Role rejection unavailable.');
    return Object.freeze({ state: 'REJECTED', operationId: id, problem });
  }
  const receipt = envelope ? record(raw.operation) : raw;
  const checked = envelope ? mutation(raw, id) : operation(raw, id);
  return Object.freeze({
    ...checked,
    resultId:
      checked.state === 'SUCCEEDED' && !checked.resultExpired
        ? uuid(receipt.resultId)
        : null,
  });
}

export async function createCreditRole(
  request: GraphqlRequest,
  intent: RoleIntent,
  csrf: string,
  signal: AbortSignal,
): Promise<RoleOutcome> {
  const document = `mutation CreateCreditRole($operationId: UUID!, $category: RoleCategory!,
    $label: String!, $policyRevision: String!) {
    createRole(operationId: $operationId, category: $category, label: $label, policyRevision: $policyRevision) {
      operation { id state resultId resultExpired problem { code retryAfterMs } }
      problem { code retryAfterMs }
    }
  }`;
  const data = record(
    await request(document, { ...intent }, { mutation: true, csrf, signal }),
  );
  return outcome(data.createRole, intent.operationId, true);
}

export async function recoverCreditRole(
  request: GraphqlRequest,
  id: string,
  csrf: string,
  signal: AbortSignal,
): Promise<RoleOutcome | null> {
  const document = `query CreditRoleOperation($id: UUID!) {
    operation(id: $id, owner: CATALOG) { id state resultId resultExpired problem { code retryAfterMs } }
  }`;
  const data = record(await request(document, { id }, { csrf, signal }));
  return data.operation === null ? null : outcome(data.operation, id, false);
}
