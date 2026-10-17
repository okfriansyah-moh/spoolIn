import type { PersonDraft } from '../../../generated/json-types';

export type WriteContext = Readonly<{
  accountId: string;
  csrf: string;
  policyRevision: string;
}>;

export type FieldError = Readonly<{ id: string; message: string }>;

export type ManagedPerson = Readonly<{
  id: string;
  name: string;
  biography: string | null;
  state: 'ACTIVE' | 'ARCHIVED';
  version: string;
}>;

export type PersonListRow = Readonly<{
  id: string;
  name: string;
  state: 'ACTIVE' | 'ARCHIVED';
}>;

export type ListState =
  | { kind: 'loading' }
  | { kind: 'failure' }
  | { kind: 'ready'; people: readonly PersonListRow[] };

export type EditorPhase =
  | 'idle'
  | 'submitting'
  | 'saved'
  | 'failed'
  | 'uncertain'
  | 'stale'
  | 'resolved'
  | 'target-changed';

export type EditorState = Readonly<{
  phase: EditorPhase;
  mode: 'create' | 'edit';
  personId: string | null;
  saved: ManagedPerson | null;
  current: ManagedPerson | null;
  draft: PersonDraft;
  fieldErrors: readonly FieldError[];
  summaryTitle: string;
  message: string;
  pendingOperationId: string | null;
  staleReviewOpen: boolean;
}>;

export const EMPTY_PERSON_DRAFT: PersonDraft = Object.freeze({ name: '', biography: undefined });
