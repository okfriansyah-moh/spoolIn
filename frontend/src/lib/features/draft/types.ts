import type { DraftValues } from '../../generated/json-types';

export const AUTOSAVE_IDLE_MS = 750;

export type DraftTarget = Readonly<{
  kind: string;
  id: string | null;
}>;

export type DraftLayers = Readonly<{
  base: DraftValues | null;
  current: DraftValues | null;
  draft: DraftValues;
}>;

export type DraftNoticeKind =
  | 'idle'
  | 'saving'
  | 'backed'
  | 'unbacked'
  | 'discarding'
  | 'discard-failed'
  | 'discard-uncertain'
  | 'recheck';

export type DraftViewState = Readonly<{
  concealed: boolean;
  notice: DraftNoticeKind;
  message: string;
  layers: DraftLayers | null;
  catalogOperationId: string | null;
  backed: boolean;
  unbacked: boolean;
  editing: boolean;
}>;

export type DraftReceipt = Readonly<{
  operationId: string;
  state: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED';
  resultId: string | null;
  version: string | null;
  resultExpired: boolean;
}>;

export type DraftMutationOutcome = Readonly<{
  receipt: DraftReceipt;
  problemCode: string | null;
}>;

export type DraftDiscardOutcome = Readonly<{
  receipt: DraftReceipt;
  discarded: boolean;
  problemCode: string | null;
}>;

export type DraftRecord = Readonly<{
  id: string;
  targetKind: string;
  targetId: string | null;
  baseVersion: string | null;
  values: DraftValues;
  operationId: string | null;
  version: string;
  expiresAt: string;
}>;

export type DraftEligibility = Readonly<{
  permitted: boolean;
  message: string;
}>;

export type DraftRequestKind =
  | 'recover-operation'
  | 'list-drafts'
  | 'save-draft'
  | 'discard-draft';

export interface DraftSource {
  recoverOperation(
    operationId: string,
    csrf: string,
    signal: AbortSignal,
  ): Promise<DraftReceipt | null>;
  listDrafts(csrf: string, signal: AbortSignal): Promise<readonly DraftRecord[]>;
  saveDraft(
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
  ): Promise<DraftMutationOutcome>;
  discardDraft(
    operationId: string,
    draftId: string,
    expectedVersion: string,
    csrf: string,
    signal: AbortSignal,
  ): Promise<DraftDiscardOutcome>;
}

export interface DraftSession {
  accountId: string | null;
  csrf: string;
  policyRevision: string;
  generation: number;
}
