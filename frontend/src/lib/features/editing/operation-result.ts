import type { GraphqlRequest } from '../../transport/graphql';
import { mutation, operation } from '../session/outcome';
import { record } from '../session/parse';

export function mutationResult(value: unknown, expected: string) {
  const receipt = mutation(value, expected);
  const input = record(value);
  const result = record(input.operation);
  const problem = input.problem === null ? null : record(input.problem);
  return {
    receipt,
    problemCode: typeof problem?.code === 'string' ? problem.code : null,
    resultId: typeof result.resultId === 'string' ? result.resultId : null,
    resultVersion: typeof result.version === 'string' ? result.version : null,
  };
}

export type RecoveredCatalogOperation = Readonly<{
  operationId: string;
  state: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED';
  resultId: string | null;
  version: string | null;
  problemCode: string | null;
  resultExpired: boolean;
}>;

export async function recoverCatalogOperation(
  request: GraphqlRequest, operationId: string, csrf: string, signal: AbortSignal,
): Promise<RecoveredCatalogOperation | null> {
  const data = record(await request(`query CatalogOperation($id: UUID!) {
    operation(id: $id, owner: CATALOG) { id state resultId version resultExpired problem { code retryAfterMs } }
  }`, { id: operationId }, { csrf, signal }));
  if (data.operation === null) return null;
  const receipt = operation(data.operation, operationId);
  const result = record(data.operation);
  return Object.freeze({ operationId: receipt.operationId, state: receipt.state,
    resultId: typeof result.resultId === 'string' ? result.resultId : null,
    version: typeof result.version === 'string' ? result.version : null,
    problemCode: receipt.problem?.code ?? null, resultExpired: receipt.resultExpired });
}
