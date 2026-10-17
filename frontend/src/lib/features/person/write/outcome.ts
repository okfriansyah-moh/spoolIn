import { mutation, operation } from '../../session/outcome';
import type { IdentityOutcome } from '../../session/commands';
import { record } from '../../session/parse';

export type PersonMutationOutcome = Readonly<{
  receipt: IdentityOutcome;
  problemCode: string | null;
  resultPersonId: string | null;
  resultVersion: string | null;
}>;

function resultPersonId(value: unknown): string | null {
  const input = record(value);
  const operationNode = record(input.operation);
  const resultId = operationNode.resultId;
  return typeof resultId === 'string' ? resultId : null;
}

function resultVersion(value: unknown): string | null {
  const input = record(value);
  const operationNode = record(input.operation);
  const version = operationNode.version;
  return typeof version === 'string' ? version : null;
}

function problemCode(value: unknown): string | null {
  const input = record(value);
  const problem = input.problem;
  if (problem === null) return null;
  const node = record(problem);
  return typeof node.code === 'string' ? node.code : null;
}

export function personMutationOutcome(value: unknown, expected: string): PersonMutationOutcome {
  const receipt = mutation(value, expected);
  return Object.freeze({
    receipt,
    problemCode: problemCode(value),
    resultPersonId: resultPersonId(value),
    resultVersion: resultVersion(value),
  });
}

export function recoverPersonOperation(value: unknown, expected: string): IdentityOutcome | null {
  if (value === null) return null;
  return operation(value, expected);
}
