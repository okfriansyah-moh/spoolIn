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
import type { DraftSource, DraftRecord } from '../../draft/types';
import { PersonDraftSync } from './draft-sync';
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
    biography: person.biography,
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
  private readonly draftSync: PersonDraftSync;

  constructor(
    private readonly request: GraphqlRequest,
    draftSource: DraftSource,
    private readonly mode: 'create' | 'edit',
    private readonly personId: string | null,
  ) {
    this.current = initialState(mode, personId);
    this.draftSync = new PersonDraftSync(draftSource, personId);
  }

  get snapshot(): EditorState { return this.current; }

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
    this.draftSync.reset({ catalogOperationId: null });
    if (this.mode === 'create') {
      this.publish({ ...this.current, summaryTitle: 'Create person' });
      await this.restoreBackedDraft(context, signal);
      return;
    }
    await this.bootstrapExistingPerson(context, signal);
    if (this.current.phase !== 'target-changed') await this.restoreBackedDraft(context, signal);
  }

  private async bootstrapExistingPerson(
    context: WriteContext,
    signal: AbortSignal,
  ): Promise<void> {
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
      this.draftSync.reset({
        catalogOperationId: null,
      });
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
    if (this.current.phase === 'submitting' || this.current.phase === 'uncertain') return;
    this.publish({ ...this.current, draft: Object.freeze({ ...next }) });
    if (this.writeContext && this.current.saved) {
      this.draftSync.schedule(
        next,
        this.writeContext,
        this.current.saved.version,
      );
    } else if (this.writeContext && this.mode === 'create') {
      this.draftSync.schedule(next, this.writeContext, null);
    }
  }

  toggleStaleReview(): void {
    this.publish({
      ...this.current,
      staleReviewOpen: !this.current.staleReviewOpen,
    });
  }

  private async restoreBackedDraft(context: WriteContext, signal: AbortSignal): Promise<void> {
    try {
      const drafts = await this.draftSync.load(context, signal);
      if (signal.aborted || drafts.length === 0) return;
      if (drafts.length > 1) {
        this.publish({ ...this.current, phase: 'failed', message: 'Several saved drafts are retained. This form starts a new draft; no saved draft was selected automatically.' });
        return;
      }
      const draft = drafts[0];
    if (draft) await this.applyBackedDraft(draft, signal);
    } catch {
      if (!signal.aborted) this.publish({ ...this.current, phase: 'failed', message: 'Saved drafts could not be checked. Retry before continuing.' });
    }
  }

  private async applyBackedDraft(draft: DraftRecord, signal: AbortSignal): Promise<void> {
    this.draftSync.adopt(draft);
    if (draft.operationId) {
      this.publish({ ...this.current, phase: 'uncertain', pendingOperationId: draft.operationId,
        message: 'Check the original save before continuing this retained draft.' });
      await this.checkOriginalResult();
      if (signal.aborted || this.current.phase !== 'failed') return;
    }
    const stale = this.current.saved && draft.baseVersion !== this.current.saved.version;
    this.publish({ ...this.current, draft: draft.values as PersonDraft,
      phase: stale ? 'stale' : 'idle', pendingOperationId: null,
      message: stale ? 'The saved person changed. Compare its current values with the retained draft.' : 'Saved draft recovered.',
      staleReviewOpen: !!stale });
  }

  private async backSubmission(operationId: string): Promise<boolean> {
    if (!this.writeContext) return false;
    const backed = await this.draftSync.prepareSubmission(operationId, this.current.draft,
      this.writeContext, this.current.saved?.version ?? null);
    if (backed) return true;
    this.pendingWriteId = null;
    this.publish({ ...this.current, phase: 'failed', pendingOperationId: null,
      message: 'Changes are not backed up yet. Retry after the original draft save is confirmed.' });
    return false;
  }

  private beginSubmit(): {
    operationId: string;
    generation: number;
    signal: AbortSignal;
  } | null {
    if (
      this.pendingWriteId ||
      this.current.phase === 'submitting' ||
      this.current.phase === 'uncertain' ||
      this.current.phase === 'stale' ||
      this.current.phase === 'target-changed' ||
      (this.mode === 'create' && this.current.saved !== null)
    ) {
      return null;
    }
    if (!this.writeContext || !this.policy) return null;
    const fieldErrors = validatePersonDraft(this.current.draft, this.policy);
    if (fieldErrors.length) {
      this.publish({
        ...this.current,
        phase: 'failed',
        fieldErrors: Object.freeze(fieldErrors),
        message: 'Changes were not saved.',
      });
      return null;
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
    return { operationId, generation, signal };
  }

  private async submitCreate(
    operationId: string,
    generation: number,
    signal: AbortSignal,
  ): Promise<void> {
    if (!this.writeContext) return;
    const input: { name: string; biography?: string } = {
      name: this.current.draft.name ?? '',
    };
    const biography = this.current.draft.biography?.trim();
    if (biography) input.biography = biography;
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
  }

  private personUpdatePatch(): { name?: string; biography?: string | null } {
    const saved = this.current.saved;
    if (!saved) return {};
    const patch: { name?: string; biography?: string | null } = {};
    if ((this.current.draft.name ?? '') !== saved.name)
      patch.name = this.current.draft.name ?? '';
    if ((this.current.draft.biography ?? '') !== (saved.biography ?? '')) {
      patch.biography = this.current.draft.biography ?? null;
    }
    return patch;
  }

  private async submitUpdate(
    operationId: string,
    generation: number,
    signal: AbortSignal,
  ): Promise<void> {
    if (!this.writeContext || !this.personId) return;
    const patch = this.personUpdatePatch();
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
    const saved = this.current.saved;
    if (!saved) return;
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
  }

  private async handleSubmitFailure(
    error: unknown,
    generation: number,
  ): Promise<void> {
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
    const rejected = error instanceof TransportFailure && error.kind === 'rejected';
    const operationId = this.current.pendingOperationId;
    if (rejected) this.pendingWriteId = null;
    this.publish({ ...this.current, phase: rejected ? 'failed' : 'uncertain',
      pendingOperationId: rejected ? null : operationId,
      message: rejected ? problemMessage(error.code) : 'Save result not confirmed. Check the original operation before repeating it.' });
  }

  async submit(): Promise<void> {
    const started = this.beginSubmit();
    if (!started) return;
    const { operationId, generation, signal } = started;
    try {
      if (!await this.backSubmission(operationId)) return;
      if (this.mode === 'create') {
        await this.submitCreate(operationId, generation, signal);
        return;
      }
      await this.submitUpdate(operationId, generation, signal);
    } catch (error) {
      await this.handleSubmitFailure(error, generation);
    }
  }

  async checkOriginalResult(): Promise<void> {
    const operationId = this.current.pendingOperationId ?? this.pendingWriteId;
    if (!operationId || !this.writeContext) return;
    const signal = this.active?.signal ?? new AbortController().signal;
    try {
      const receipt = await recoverCatalogOperation(this.request, operationId, this.writeContext.csrf, signal);
      await this.applyOriginalReceipt(receipt);
    } catch {
      this.publish({ ...this.current, phase: 'uncertain',
        message: 'Save result not confirmed. Check the original operation before repeating it.' });
    }
  }

  private async applyOriginalReceipt(receipt: Awaited<ReturnType<typeof recoverCatalogOperation>>): Promise<void> {
    if (receipt?.state === 'EXPIRED' || (receipt && 'resultExpired' in receipt && receipt.resultExpired === true)) {
      this.publish({ ...this.current, phase: 'uncertain',
        message: 'Original save result expired. Review the current record; do not repeat the original change.' });
      return;
    }
    if (receipt?.state === 'SUCCEEDED') {
      const personId = receipt.resultId ?? this.personId;
      if (personId) await this.publishSavedPerson(personId);
      this.pendingWriteId = null;
      this.publish({ ...this.current, phase: 'resolved', pendingOperationId: null,
        message: 'Original save confirmed. The current record has been refreshed; no repeat submission is needed.' });
      return;
    }
    if (receipt?.state === 'FAILED') {
      this.pendingWriteId = null;
      this.publish({ ...this.current, phase: 'failed', pendingOperationId: null,
        message: problemMessage(receipt.problemCode) });
      return;
    }
    this.publish({ ...this.current, phase: 'uncertain',
      message: 'Save result not confirmed. Check the original operation before repeating it.' });
  }

  private async finishWrite(
    state: string,
    problemCode: string | null,
    operationId: string,
    personId: string | null,
    _version?: string | null,
  ): Promise<void> {
    this.pendingWriteId = null;
    if (state === 'SUCCEEDED' && personId && this.writeContext) {
      await this.publishSavedPerson(personId);
      return;
    }
    await this.publishFinishFailure(state, problemCode, operationId);
  }

  private async publishSavedPerson(personId: string): Promise<void> {
    if (!this.writeContext) return;
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
  }

  private async publishFinishFailure(
    state: string,
    problemCode: string | null,
    operationId: string,
  ): Promise<void> {
    if (problemCode === 'VERSION_CONFLICT' || problemCode === 'STALE_VERSION') {
      await this.enterStaleReview();
      return;
    }
    this.publish({
      ...this.current,
      phase: state === 'UNCERTAIN' || state === 'PENDING' || state === 'EXPIRED' ? 'uncertain' : 'failed',
      pendingOperationId: state === 'FAILED' ? null : operationId,
      message: problemMessage(problemCode),
    });
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
    this.draftSync.dispose();
    this.listeners.clear();
  }

  private publish(state: EditorState): void {
    this.current = Object.freeze(state);
    this.listeners.forEach((listener) => listener(this.current));
  }
}

export function createPersonEditorController(
  request: GraphqlRequest,
  draftSource: DraftSource,
  mode: 'create' | 'edit',
  personId: string | null,
): PersonEditorController {
  return new PersonEditorController(request, draftSource, mode, personId);
}
