import { TransportFailure } from '../../transport/graphql';
import { record, uuid } from '../session/parse';
import type {
  DraftDiscardOutcome,
  DraftMutationOutcome,
  DraftReceipt,
} from './types';

function receipt(value: unknown, expected: string): DraftReceipt {
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
  const resultId =
    input.resultId === null
      ? null
      : typeof input.resultId === 'string'
        ? uuid(input.resultId)
        : null;
  const version =
    input.version === null
      ? null
      : typeof input.version === 'string'
        ? input.version
        : null;
  return Object.freeze({
    operationId,
    state,
    resultId,
    version,
    resultExpired: input.resultExpired,
  });
}

function problemCode(value: unknown): string | null {
  if (value === null) return null;
  const input = record(value);
  return typeof input.code === 'string' ? input.code : null;
}

export function saveOutcome(value: unknown, expected: string): DraftMutationOutcome {
  const input = record(value);
  return Object.freeze({
    receipt: receipt(input.operation, expected),
    problemCode: problemCode(input.problem),
  });
}

export function discardOutcome(
  value: unknown,
  expected: string,
): DraftDiscardOutcome {
  const input = record(value);
  const discarded = input.discarded === true;
  return Object.freeze({
    receipt: receipt(input.operation, expected),
    discarded,
    problemCode: problemCode(input.problem),
  });
}
