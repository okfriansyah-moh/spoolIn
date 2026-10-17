import type { GraphqlRequest } from '../../../transport/graphql';
import { TransportFailure } from '../../../transport/graphql';
import type { Policy } from '../../../policy/types';
import type { PersonDraft } from '../../../generated/json-types';
import {
  createPerson,
  fetchManagedPerson,
  recoverCatalogOperation,
  updatePerson,
} from './api';
import type { EditorState, ManagedPerson, WriteContext } from './types';
import { EMPTY_PERSON_DRAFT } from './types';
import { problemMessage, validatePersonDraft } from './validation';

function initialState(
  mode: 'create' | 'edit',
  personId: string | null,
): EditorState {
  return Object.freeze({
    phase: 'idle',
    mode,
    personId,
    saved: null,
    current: null,
    draft: EMPTY_PERSON_DRAFT,
    fieldErrors: Object.freeze([]),
    summaryTitle: mode === 'create' ? 'Create person' : 'Edit person',
    message: '',
    pendingOperationId: null,
    staleReviewOpen: false,
  });
}

function personToDraft(person: ManagedPerson): PersonDraft {
  return Object.freeze({
    name: person.name,
    biography: person.biography ?? undefined,
  });
}

export class PersonEditorController {
  private current = initialState('create', null);
  private readonly listeners = new Set<(state: EditorState) => void>();
  private writeContext: WriteContext | null = null;
  private policy: Policy | null = null;
  private pendingWriteId: string | null = null;
  private writeGeneration = 0;
  private active: AbortController | null = null;

  constructor(
    private readonly request: GraphqlRequest,
    private readonly mode: 'create' | 'edit',
    private readonly personId: string | null,
  ) {
    this.current = initialState(mode, personId);
  }

  subscribe(listener: (state: EditorState) => void): () => void {
    this.listeners.add(listener);
    listener(this.current);
    return () => this.listeners.delete(listener);
  }

  async bootstrap(
    policy: Policy,
    context: WriteContext,
    signal: AbortSignal,
  ): Promise<void> {
    this.policy = policy;
    this.writeContext = context;
    if (this.mode === 'create') {
      this.publish({ ...this.current, summaryTitle: 'Create person' });
      return;
    }
    if (!this.personId) {
      this.publish({
        ...this.current,
        phase: 'target-changed',
        message: 'This person is not available for editing.',
      });
      return;
    }
    try {
      const person = await fetchManagedPerson(
        this.request,
        this.personId,
        context.csrf,
        signal,
      );
      if (signal.aborted) return;
      if (!person || person.state !== 'ACTIVE') {
        this.publish({
          ...this.current,
          phase: 'target-changed',
          message:
            person?.state === 'ARCHIVED'
              ? 'Restore before editing. Person edits and new credit assignment stay disabled while archived.'
              : 'This person is unavailable.',
          saved: person,
          current: person,
        });
        return;
      }
      this.publish({
        ...this.current,
        saved: person,
        current: person,
        draft: personToDraft(person),
        summaryTitle: person.name,
      });
    } catch {
      if (signal.aborted) return;
      this.publish({
        ...this.current,
        phase: 'failed',
        message: 'Person details could not be loaded.',
      });
    }
  }

  updateDraft(next: PersonDraft): void {
    if (this.current.phase === 'submitting') return;
    this.publish({ ...this.current, draft: Object.freeze({ ...next }) });
  }

  toggleStaleReview(): void {
    this.publish({
      ...this.current,
      staleReviewOpen: !this.current.staleReviewOpen,
    });
  }

  async submit(): Promise<void> {
    if (
      this.pendingWriteId ||
      this.current.phase === 'submitting' ||
      this.current.phase === 'uncertain'
    )
      return;
    if (!this.writeContext || !this.policy) return;
    const fieldErrors = validatePersonDraft(this.current.draft, this.policy);
    if (fieldErrors.length) {
      this.publish({
        ...this.current,
        phase: 'failed',
        fieldErrors: Object.freeze(fieldErrors),
        message: 'Changes were not saved.',
      });
      return;
    }
    const operationId = globalThis.crypto.randomUUID();
    this.pendingWriteId = operationId;
    const generation = ++this.writeGeneration;
    this.active?.abort();
    this.active = new AbortController();
    const signal = this.active.signal;
    this.publish({
      ...this.current,
      phase: 'submitting',
      pendingOperationId: operationId,
      fieldErrors: Object.freeze([]),
      message: '',
    });
    try {
      if (this.mode === 'create') {
        const input = {
          name: this.current.draft.name ?? '',
          biography: this.current.draft.biography || undefined,
        };
        const outcome = await createPerson(
          this.request,
          operationId,
          this.writeContext.policyRevision,
          input,
          this.writeContext.csrf,
          signal,
        );
        if (generation !== this.writeGeneration) return;
        await this.finishWrite(
          outcome.receipt.state,
          outcome.problemCode,
          operationId,
          outcome.resultPersonId,
        );
        return;
      }
      const saved = this.current.saved;
      if (!saved || !this.personId) return;
      const patch: { name?: string; biography?: string | null } = {};
      if ((this.current.draft.name ?? '') !== saved.name)
        patch.name = this.current.draft.name ?? '';
      if ((this.current.draft.biography ?? '') !== (saved.biography ?? '')) {
        patch.biography = this.current.draft.biography ?? null;
      }
      if (!Object.keys(patch).length) {
        this.pendingWriteId = null;
        this.publish({
          ...this.current,
          phase: 'idle',
          pendingOperationId: null,
          message: 'No changes to save.',
        });
        return;
      }
      const outcome = await updatePerson(
        this.request,
        operationId,
        this.personId,
        saved.version,
        this.writeContext.policyRevision,
        patch,
        this.writeContext.csrf,
        signal,
      );
      if (generation !== this.writeGeneration) return;
      await this.finishWrite(
        outcome.receipt.state,
        outcome.problemCode,
        operationId,
        outcome.resultPersonId ?? this.personId,
        outcome.resultVersion,
      );
    } catch (error) {
      if (generation !== this.writeGeneration) return;
      if (
        error instanceof TransportFailure &&
        error.kind === 'rejected' &&
        error.code === 'CONFLICT'
      ) {
        this.pendingWriteId = null;
        await this.enterStaleReview();
        return;
      }
      this.pendingWriteId = null;
      this.publish({
        ...this.current,
        phase: 'failed',
        pendingOperationId: null,
        message:
          error instanceof TransportFailure
            ? problemMessage(error.code)
            : 'Changes were not saved.',
      });
    }
  }

  async checkOriginalResult(): Promise<void> {
    if (!this.writeContext || !this.pendingWriteId) return;
    const operationId = this.pendingWriteId;
    const signal = this.active?.signal ?? new AbortController().signal;
    const receipt = await recoverCatalogOperation(
      this.request,
      operationId,
      this.writeContext.csrf,
      signal,
    );
    if (!receipt) {
      this.publish({
        ...this.current,
        phase: 'uncertain',
        message:
          'Save result not confirmed. Check again before submitting another save.',
      });
      return;
    }
    this.pendingWriteId = null;
    this.publish({
      ...this.current,
      phase: 'resolved',
      pendingOperationId: null,
      message: 'Original save confirmed.',
    });
  }

  private async finishWrite(
    state: string,
    problemCode: string | null,
    operationId: string,
    personId: string | null,
    version?: string | null,
  ): Promise<void> {
    this.pendingWriteId = null;
    if (state === 'SUCCEEDED' && personId && this.writeContext) {
      const person = await fetchManagedPerson(
        this.request,
        personId,
        this.writeContext.csrf,
      );
      this.publish({
        ...this.current,
        phase: 'saved',
        pendingOperationId: null,
        saved: person,
        current: person,
        draft: person ? personToDraft(person) : this.current.draft,
        message: '',
        summaryTitle: person?.name ?? this.current.summaryTitle,
      });
      return;
    }
    if (problemCode === 'VERSION_CONFLICT' || problemCode === 'STALE_VERSION') {
      await this.enterStaleReview();
      return;
    }
    this.publish({
      ...this.current,
      phase: state === 'UNCERTAIN' ? 'uncertain' : 'failed',
      pendingOperationId: state === 'UNCERTAIN' ? operationId : null,
      message: problemMessage(problemCode),
    });
    void version;
  }

  private async enterStaleReview(): Promise<void> {
    if (!this.writeContext || !this.personId) return;
    const current = await fetchManagedPerson(
      this.request,
      this.personId,
      this.writeContext.csrf,
    );
    this.publish({
      ...this.current,
      phase: 'stale',
      current,
      message:
        'This person changed, current record and retained draft differ. Review before a fresh versioned save.',
      staleReviewOpen: true,
    });
  }

  dispose(): void {
    this.active?.abort();
    this.listeners.clear();
  }

  private publish(state: EditorState): void {
    this.current = Object.freeze(state);
    this.listeners.forEach((listener) => listener(this.current));
  }
}

export function createPersonEditorController(
  request: GraphqlRequest,
  mode: 'create' | 'edit',
  personId: string | null,
): PersonEditorController {
  return new PersonEditorController(request, mode, personId);
}
