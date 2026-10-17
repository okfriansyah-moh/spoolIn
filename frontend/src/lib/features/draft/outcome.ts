import { TransportFailure } from '../../transport/graphql';
import { record, uuid } from '../session/parse';
import type {
  DraftDiscardOutcome,
  DraftMutationOutcome,
  DraftReceipt,
} from './types';

export function receiptState(value: unknown): DraftReceipt['state'] {
  if (
    value === 'PENDING' ||
    value === 'SUCCEEDED' ||
    value === 'FAILED' ||
    value === 'EXPIRED'
  ) {
    return value;
  }
  throw new TransportFailure('unavailable');
}

function optionalUuid(value: unknown): string | null {
  if (value === null) return null;
  return typeof value === 'string' ? uuid(value) : null;
}

function optionalVersion(value: unknown): string | null {
  if (value === null) return null;
  return typeof value === 'string' ? value : null;
}

function receipt(value: unknown, expected: string): DraftReceipt {
  const input = record(value);
  const operationId = uuid(input.id);
  if (operationId !== expected || typeof input.resultExpired !== 'boolean')
    throw new TransportFailure('unavailable');
  return Object.freeze({
    operationId,
    state: receiptState(input.state),
    resultId: optionalUuid(input.resultId),
    version: optionalVersion(input.version),
    resultExpired: input.resultExpired,
  });
}

function problemCode(value: unknown): string | null {
  if (value === null) return null;
  const input = record(value);
  return typeof input.code === 'string' ? input.code : null;
}

export function saveOutcome(
  value: unknown,
  expected: string,
): DraftMutationOutcome {
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
