import { mutation, operation } from '../session/outcome';
import type { IdentityOutcome } from '../session/commands';
import { record } from '../session/parse';

export type MovieMutationOutcome = Readonly<{
  receipt: IdentityOutcome;
  problemCode: string | null;
  resultMovieId: string | null;
  resultVersion: string | null;
}>;

function resultMovieId(value: unknown): string | null {
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

export function movieMutationOutcome(
  value: unknown,
  expected: string,
): MovieMutationOutcome {
  const receipt = mutation(value, expected);
  return Object.freeze({
    receipt,
    problemCode: problemCode(value),
    resultMovieId: resultMovieId(value),
    resultVersion: resultVersion(value),
  });
}

export function recoverMovieOperation(
  value: unknown,
  expected: string,
): IdentityOutcome | null {
  if (value === null) return null;
  return operation(value, expected);
}
