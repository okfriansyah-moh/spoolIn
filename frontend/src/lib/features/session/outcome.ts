import { TransportFailure } from '../../transport/graphql';
import type { IdentityOutcome, SafeProblem } from './commands';
import { record, uuid } from './parse';
function retryAfter(value: unknown): number {
  if (value === null || value === undefined) return 0;
  if (
    typeof value !== 'number' ||
    !Number.isInteger(value) ||
    value < 0 ||
    value > 2147483647
  )
    throw new TransportFailure('unavailable');
  return value;
}

export function safeProblem(value: unknown): SafeProblem | null {
  if (value === null) return null;
  const input = record(value);
  if (
    typeof input.code !== 'string' ||
    !/^[A-Z][A-Z0-9_]{0,63}$/.test(input.code)
  )
    throw new TransportFailure('unavailable');
  const retry = retryAfter(input.retryAfterMs);
  return Object.freeze({ code: input.code, retryAfterMs: retry ?? 0 });
}
export function operation(value: unknown, expected: string): IdentityOutcome {
  const input = record(value);
  const operationId = uuid(input.id);
  if (operationId !== expected || typeof input.resultExpired !== 'boolean')
    throw new TransportFailure('unavailable');
  const state = input.state;
  if (
    state !== 'PENDING' &&
    state !== 'SUCCEEDED' &&
    state !== 'FAILED' &&
    state !== 'EXPIRED'
  )
    throw new TransportFailure('unavailable');
  const problem = safeProblem(input.problem);
  if (state === 'SUCCEEDED' && problem !== null)
    throw new TransportFailure('unavailable');
  return Object.freeze({
    operationId,
    state,
    resultExpired: input.resultExpired,
    problem,
  });
}
export function mutation(value: unknown, expected: string): IdentityOutcome {
  const input = record(value);
  const result = operation(input.operation, expected);
  const problem = safeProblem(input.problem);
  if (result.state === 'SUCCEEDED' && problem !== null)
    throw new TransportFailure('unavailable');
  return Object.freeze({ ...result, problem: problem ?? result.problem });
}
