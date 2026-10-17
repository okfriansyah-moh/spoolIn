import type { DraftValues } from '../../generated/json-types';
import { createTransport, type GraphqlRequest } from '../../transport/graphql';
import { record, uuid } from '../session/parse';
import discardDocument from './discard-draft.graphql?raw';
import draftsDocument from './drafts.graphql?raw';
import { discardOutcome, saveOutcome } from './outcome';
import recoverDocument from './recover.graphql?raw';
import saveDocument from './save-draft.graphql?raw';
import type {
  DraftDiscardOutcome,
  DraftMutationOutcome,
  DraftRecord,
  DraftReceipt,
  DraftSource,
} from './types';
import { optionalString, optionalUuid, requiredString } from './parse-helpers';
import { receiptState } from './outcome';

function draftRecord(value: unknown): DraftRecord {
  const input = record(value);
  const id = uuid(input.id);
  const targetKind = requiredString(input.targetKind);
  const values = input.values as DraftValues;
  const version = requiredString(input.version);
  const expiresAt = requiredString(input.expiresAt);
  if (!targetKind || !version || !expiresAt || !values)
    throw new Error('Draft list unavailable.');
  return Object.freeze({
    id,
    targetKind,
    targetId: optionalUuid(input.targetId),
    baseVersion: optionalString(input.baseVersion),
    values,
    operationId: optionalUuid(input.operationId),
    version,
    expiresAt,
  });
}

function recoverReceipt(value: unknown, expected: string): DraftReceipt | null {
  if (value === null) return null;
  const input = record(value);
  const operationId = uuid(input.id);
  if (operationId !== expected || typeof input.resultExpired !== 'boolean')
    throw new Error('Draft recovery unavailable.');
  const resultId = optionalUuid(input.resultId);
  const version = optionalString(input.version);
  return Object.freeze({
    operationId,
    state: receiptState(input.state),
    resultId,
    version,
    resultExpired: input.resultExpired,
  });
}

class DraftApi implements DraftSource {
  constructor(private readonly request: GraphqlRequest) {}

  async recoverOperation(
    operationId: string,
    csrf: string,
    signal: AbortSignal,
  ): Promise<DraftReceipt | null> {
    const data = record(
      await this.request(
        recoverDocument,
        { id: operationId },
        { csrf, signal },
      ),
    );
    return recoverReceipt(data.operation, operationId);
  }

  async listDrafts(
    csrf: string,
    signal: AbortSignal,
  ): Promise<readonly DraftRecord[]> {
    const data = record(
      await this.request(draftsDocument, {}, { csrf, signal }),
    );
    const drafts = data.drafts;
    if (!Array.isArray(drafts)) throw new Error('Draft list unavailable.');
    return Object.freeze(drafts.map(draftRecord));
  }

  async saveDraft(
    operationId: string,
    draftId: string,
    expectedVersion: string | null,
    input: {
      targetKind: string;
      targetId?: string | null;
      baseVersion?: string | null;
      values: DraftValues;
      operationId?: string | null;
      policyRevision: string;
    },
    csrf: string,
    signal: AbortSignal,
  ): Promise<DraftMutationOutcome> {
    const data = record(
      await this.request(
        saveDocument,
        {
          operationId,
          id: draftId,
          expectedVersion,
          input: {
            targetKind: input.targetKind,
            targetId: input.targetId ?? null,
            baseVersion: input.baseVersion ?? null,
            values: input.values,
            operationId: input.operationId ?? null,
            policyRevision: input.policyRevision,
          },
        },
        { mutation: true, csrf, signal },
      ),
    );
    return saveOutcome(data.saveDraft, operationId);
  }

  async discardDraft(
    operationId: string,
    draftId: string,
    expectedVersion: string,
    csrf: string,
    signal: AbortSignal,
  ): Promise<DraftDiscardOutcome> {
    const data = record(
      await this.request(
        discardDocument,
        { operationId, id: draftId, expectedVersion },
        { mutation: true, csrf, signal },
      ),
    );
    return discardOutcome(data.discardDraft, operationId);
  }
}

export function createDraftSource(
  request: GraphqlRequest = createTransport(),
): DraftSource {
  return new DraftApi(request);
}
