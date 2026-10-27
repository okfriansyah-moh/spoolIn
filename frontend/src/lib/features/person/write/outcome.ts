import { operation } from '../../session/outcome';
import type { IdentityOutcome } from '../../session/commands';
import { mutationResult } from '../../editing/operation-result';

export type PersonMutationOutcome = Readonly<{
  receipt: IdentityOutcome;
  problemCode: string | null;
  resultPersonId: string | null;
  resultVersion: string | null;
}>;

export function personMutationOutcome(
  value: unknown,
  expected: string,
): PersonMutationOutcome {
  const result = mutationResult(value, expected);
  return Object.freeze({
    receipt: result.receipt,
    problemCode: result.problemCode,
    resultPersonId: result.resultId,
    resultVersion: result.resultVersion,
  });
}

export function recoverPersonOperation(
  value: unknown,
  expected: string,
): IdentityOutcome | null {
  if (value === null) return null;
  return operation(value, expected);
}
