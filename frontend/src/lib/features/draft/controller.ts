import { TransportFailure } from '../../transport/graphql';
import type { PersonDraft } from '../../generated/json-types';
import { DraftHandles } from './handles';
import type {
  DraftEligibility,
  DraftLayers,
  DraftRecord,
  DraftRequestKind,
  DraftSession,
  DraftSource,
  DraftTarget,
  DraftViewState,
} from './types';
import { AUTOSAVE_IDLE_MS } from './types';

type PendingCore = Readonly<{
  operationId: string;
  kind: 'save' | 'discard';
  draftId: string;
  generation: number;
}>;

const concealedView = (): DraftViewState =>
  Object.freeze({
    concealed: true,
    notice: 'idle',
    message: '',
    layers: null,
    catalogOperationId: null,
    backed: false,
    unbacked: false,
    editing: false,
  });

export class DraftController {
  private session: DraftSession = Object.freeze({
    accountId: null,
    csrf: '',
    policyRevision: '',
    generation: 0,
  });
  private target: DraftTarget = Object.freeze({ kind: 'PERSON', id: null });
  private draftId = '';
  private expectedVersion: string | null = null;
  private catalogOperationId: string | null = null;
  private layers: DraftLayers = Object.freeze({
    base: null,
    current: null,
    draft: Object.freeze({ name: '', biography: null }),
  });
  private editing = '';
  private backed = false;
  private unbacked = false;
  private notice: DraftViewState['notice'] = 'idle';
  private message = '';
  private concealed = true;
  private pendingCore: PendingCore | null = null;
  private autosaveTimer: ReturnType<typeof globalThis.setTimeout> | undefined;
  private active: AbortController | null = null;
  private readonly handles = new DraftHandles();
  private readonly listeners = new Set<(state: DraftViewState) => void>();
  private readonly requestLog: DraftRequestKind[] = [];

  constructor(private readonly source: DraftSource) {}

  get snapshot(): DraftViewState {
    return this.view();
  }

  get requestSequence(): readonly DraftRequestKind[] {
    return this.requestLog;
  }

  subscribe(listener: (state: DraftViewState) => void): () => void {
    this.listeners.add(listener);
    listener(this.view());
    return () => {
      this.listeners.delete(listener);
    };
  }

  bindSession(next: DraftSession): void {
    const previous = this.session.accountId;
    if (previous !== null && previous !== next.accountId) {
      this.concealForAccountChange();
      return;
    }
    if (next.accountId === null) {
      this.concealForSessionLoss();
      return;
    }
    this.session = Object.freeze({ ...next });
    if (this.concealed && previous === next.accountId) this.unlockSameAccount();
  }

  beginEdit(
    target: DraftTarget,
    options: {
      draftId?: string;
      catalogOperationId?: string | null;
      base?: PersonDraft | null;
      current?: PersonDraft | null;
      initial?: PersonDraft;
    } = {},
  ): void {
    if (this.session.accountId === null) {
      this.concealForSessionLoss();
      return;
    }
    this.target = Object.freeze({ ...target });
    this.draftId = options.draftId ?? globalThis.crypto.randomUUID();
    this.expectedVersion = null;
    this.catalogOperationId = options.catalogOperationId ?? null;
    const draft = Object.freeze({
      name: options.initial?.name ?? '',
      biography: options.initial?.biography ?? null,
    });
    this.layers = Object.freeze({
      base: options.base ?? null,
      current: options.current ?? null,
      draft,
    });
    this.editing = draft.name ?? '';
    this.backed = false;
    this.unbacked = false;
    this.notice = 'idle';
    this.message = '';
    this.concealed = false;
    this.publish();
  }

  setName(value: string): void {
    if (this.concealed || this.session.accountId === null) return;
    if (value === this.editing) return;
    this.editing = value;
    this.layers = Object.freeze({
      ...this.layers,
      draft: Object.freeze({ ...this.layers.draft, name: value }),
    });
    this.scheduleAutosave();
    this.publish();
  }

  async recoverPendingOperation(): Promise<void> {
    const pending = this.pendingCore;
    if (!pending || this.session.accountId === null || !this.session.csrf) return;
    const generation = this.begin();
    const signal = this.signal();
    try {
      this.requestLog.push('recover-operation');
      const receipt = await this.source.recoverOperation(
        pending.operationId,
        this.session.csrf,
        signal,
      );
      if (generation !== this.session.generation) return;
      if (receipt?.state === 'SUCCEEDED' && receipt.resultId) {
        this.draftId = receipt.resultId;
        this.expectedVersion = receipt.version;
        this.pendingCore = null;
        this.notice = 'recheck';
        this.message = 'Check before continuing';
        this.publish();
        return;
      }
      if (receipt?.state === 'FAILED' || receipt?.state === 'EXPIRED') {
        this.pendingCore = null;
        this.unbacked = true;
        this.notice = 'unbacked';
        this.message = 'Changes are not backed up yet.';
        this.publish();
      }
    } catch (error) {
      if (generation !== this.session.generation) return;
      if (error instanceof TransportFailure && error.kind === 'rejected') {
        this.unbacked = true;
        this.notice = 'unbacked';
        this.message = 'Changes are not backed up yet.';
        this.publish();
      }
    }
  }

  async resumeAfterEligibility(
    eligibility: DraftEligibility,
  ): Promise<void> {
    if (this.session.accountId === null || !this.session.csrf) {
      this.concealForSessionLoss();
      return;
    }
    if (this.pendingCore) {
      await this.recoverPendingOperation();
      if (this.pendingCore) {
        this.notice = 'recheck';
        this.message = 'Check before continuing';
        this.concealed = true;
        this.publish();
        return;
      }
    }
    if (!eligibility.permitted) {
      this.concealForAccountChange();
      this.message = eligibility.message;
      this.publish();
      return;
    }
    const generation = this.begin();
    const signal = this.signal();
    try {
      this.requestLog.push('list-drafts');
      const drafts = await this.source.listDrafts(this.session.csrf, signal);
      if (generation !== this.session.generation) return;
      const match = this.pickDraft(drafts);
      if (match) this.applyServerDraft(match);
      this.concealed = false;
      this.notice = this.unbacked ? 'unbacked' : this.backed ? 'backed' : 'idle';
      this.message = this.unbacked
        ? 'Changes are not backed up yet.'
        : this.backed
          ? 'Draft saved.'
          : '';
      this.publish();
    } catch {
      if (generation !== this.session.generation) return;
      this.notice = 'recheck';
      this.message = 'Check before continuing';
      this.concealed = true;
      this.publish();
    }
  }

  async discard(): Promise<void> {
    if (
      this.concealed ||
      this.session.accountId === null ||
      !this.session.csrf ||
      this.expectedVersion === null
    )
      return;
    const operationId = globalThis.crypto.randomUUID();
    const generation = this.session.generation;
    const csrf = this.session.csrf;
    const draftId = this.draftId;
    const version = this.expectedVersion;
    this.notice = 'discarding';
    this.message = 'Discarding draft…';
    this.publish();
    try {
      this.requestLog.push('discard-draft');
      const outcome = await this.source.discardDraft(
        operationId,
        draftId,
        version,
        csrf,
        this.signal(),
      );
      if (generation !== this.session.generation) return;
      if (outcome.receipt.state === 'SUCCEEDED' && outcome.discarded) {
        this.backed = false;
        this.unbacked = false;
        this.notice = 'idle';
        this.message = 'Draft discarded.';
        this.expectedVersion = null;
        this.publish();
        return;
      }
      this.notice = 'discard-failed';
      this.message = 'Discard did not complete.';
      this.publish();
    } catch (error) {
      if (generation !== this.session.generation) return;
      this.notice =
        error instanceof TransportFailure && error.kind === 'uncertain'
          ? 'discard-uncertain'
          : 'discard-failed';
      this.message = 'Discard is not confirmed.';
      this.publish();
    }
  }

  trackPreviewUrl(url: string): string {
    return this.handles.trackObjectUrl(url);
  }

  get releasedHandleCount(): number {
    return this.handles.releasedCount;
  }

  dispose(): void {
    globalThis.clearTimeout(this.autosaveTimer);
    this.active?.abort();
    this.handles.release();
    this.listeners.clear();
  }

  private scheduleAutosave(): void {
    globalThis.clearTimeout(this.autosaveTimer);
    this.autosaveTimer = globalThis.setTimeout(() => {
      void this.autosave();
    }, AUTOSAVE_IDLE_MS);
  }

  private async autosave(): Promise<void> {
    if (this.concealed || this.session.accountId === null || !this.session.csrf)
      return;
    const operationId = globalThis.crypto.randomUUID();
    const generation = this.session.generation;
    const csrf = this.session.csrf;
    const policyRevision = this.session.policyRevision;
    const draftId = this.draftId;
    const expectedVersion = this.expectedVersion;
    const values = this.layers.draft;
    this.notice = 'saving';
    this.message = 'Saving draft…';
    this.publish();
    this.pendingCore = Object.freeze({
      operationId,
      kind: 'save',
      draftId,
      generation,
    });
    try {
      this.requestLog.push('save-draft');
      const outcome = await this.source.saveDraft(
        operationId,
        draftId,
        expectedVersion,
        {
          targetKind: this.target.kind,
          targetId: this.target.id,
          baseVersion: this.layers.base ? this.expectedVersion : null,
          values,
          operationId: this.catalogOperationId,
          policyRevision,
        },
        csrf,
        this.signal(),
      );
      if (generation !== this.session.generation) return;
      this.pendingCore = null;
      if (outcome.receipt.state === 'SUCCEEDED' && outcome.receipt.resultId) {
        this.draftId = outcome.receipt.resultId;
        this.expectedVersion = outcome.receipt.version;
        this.backed = true;
        this.unbacked = false;
        this.notice = 'backed';
        this.message = 'Draft saved.';
        this.publish();
        return;
      }
      this.unbacked = true;
      this.backed = false;
      this.notice = 'unbacked';
      this.message = 'Changes are not backed up yet.';
      this.publish();
    } catch (error) {
      if (generation !== this.session.generation) return;
      this.unbacked = true;
      this.backed = false;
      this.notice = 'unbacked';
      this.message = 'Changes are not backed up yet.';
      this.publish();
      if (!(error instanceof TransportFailure && error.kind === 'rejected'))
        this.pendingCore = Object.freeze({
          operationId,
          kind: 'save',
          draftId,
          generation,
        });
    }
  }

  private pickDraft(drafts: readonly DraftRecord[]): DraftRecord | null {
    return (
      drafts.find(
        (item) =>
          item.targetKind === this.target.kind &&
          item.targetId === this.target.id,
      ) ?? null
    );
  }

  private applyServerDraft(record: DraftRecord): void {
    this.draftId = record.id;
    this.expectedVersion = record.version;
    this.catalogOperationId = record.operationId;
    this.layers = Object.freeze({
      base: this.layers.base,
      current: record.values,
      draft: record.values,
    });
    this.editing = (record.values as PersonDraft).name ?? '';
    this.backed = true;
    this.unbacked = false;
  }

  private unlockSameAccount(): void {
    this.notice = 'recheck';
    this.message = 'Check before continuing';
    this.concealed = true;
    this.publish();
  }

  private concealForSessionLoss(): void {
    globalThis.clearTimeout(this.autosaveTimer);
    this.active?.abort();
    this.handles.release();
    this.session = Object.freeze({
      accountId: null,
      csrf: '',
      policyRevision: '',
      generation: this.session.generation + 1,
    });
    this.concealed = true;
    this.editing = '';
    this.pendingCore = null;
    this.notice = 'idle';
    this.message = '';
    this.layers = Object.freeze({
      base: null,
      current: null,
      draft: Object.freeze({ name: '', biography: null }),
    });
    this.publish();
  }

  private concealForAccountChange(): void {
    this.concealForSessionLoss();
    this.message = 'This draft belongs to another account.';
    this.notice = 'idle';
    this.publish();
  }

  private view(): DraftViewState {
    if (this.concealed) {
      return Object.freeze({
        ...concealedView(),
        message: this.message,
        catalogOperationId: this.catalogOperationId,
      });
    }
    return Object.freeze({
      concealed: false,
      notice: this.notice,
      message: this.message,
      layers: this.layers,
      catalogOperationId: this.catalogOperationId,
      backed: this.backed,
      unbacked: this.unbacked,
      editing: true,
    });
  }

  private publish(): void {
    const state = this.view();
    for (const listener of this.listeners) listener(state);
  }

  private begin(): number {
    globalThis.clearTimeout(this.autosaveTimer);
    this.active?.abort();
    this.active = new AbortController();
    return this.session.generation;
  }

  private signal(): AbortSignal {
    if (!this.active) this.active = new AbortController();
    return this.active.signal;
  }
}
