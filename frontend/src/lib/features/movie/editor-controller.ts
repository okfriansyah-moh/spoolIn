import type { GraphqlRequest } from '../../transport/graphql';
import { TransportFailure } from '../../transport/graphql';
import type { MovieDraft } from '../../generated/json-types';
import type { Policy } from '../../policy/types';
import {
  createMovie,
  fetchManagedMovie,
  fetchMovieGenres,
  recoverCatalogOperation,
  updateMovie,
} from './api';
import { MovieDraftSync } from './draft-sync';
import { draftToCreateInput, draftToUpdatePatch, patchIsEmpty } from './patch';
import { rebaseMovieMetadata } from './metadata';
import { movieToDraft } from './projection';
import type { EditorState, WriteContext } from './types';
import { EMPTY_MOVIE_DRAFT } from './types';
import { problemMessage, validateMovieDraft } from './validation';
import type { DraftSource, DraftRecord } from '../draft/types';

function initialState(
  mode: 'create' | 'edit',
  movieId: string | null,
): EditorState {
  return Object.freeze({
    phase: 'idle',
    draftChoices: Object.freeze([]),
    draftBaseKnown: true,
    mode,
    movieId,
    saved: null,
    current: null,
    draft: EMPTY_MOVIE_DRAFT,
    genres: Object.freeze([]),
    fieldErrors: Object.freeze([]),
    summaryTitle: mode === 'create' ? 'Create movie' : 'Edit movie',
    message: '',
    pendingOperationId: null,
    staleReviewOpen: false,
    mediaOutage: false,
    discardDialogOpen: false,
  });
}

export class MovieEditorController {
  private current = initialState('create', null);
  private readonly listeners = new Set<(state: EditorState) => void>();
  private writeContext: WriteContext | null = null;
  private policy: Policy | null = null;
  private pendingWriteId: string | null = null;
  private writeGeneration = 0;
  private originalTitleTouched = false;
  private active: AbortController | null = null;
  private readonly draftSync: MovieDraftSync;

  constructor(
    private readonly request: GraphqlRequest,
    draftSource: DraftSource,
    private readonly mode: 'create' | 'edit',
    private readonly movieId: string | null,
  ) {
    this.current = initialState(mode, movieId);
    this.draftSync = new MovieDraftSync(draftSource, movieId);
  }

  get snapshot(): EditorState {
    return this.current;
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
    this.draftSync.reset({ catalogOperationId: null });
    try {
      const genres = await fetchMovieGenres(this.request, signal);
      if (signal.aborted) return;
      if (this.mode === 'create') {
        this.publishCreateBootstrap(genres);
        await this.restoreBackedDraft(context, signal);
        return;
      }
      await this.bootstrapExistingMovie(genres, context, signal);
      if (this.current.phase !== 'target-changed')
        await this.restoreBackedDraft(context, signal);
    } catch {
      if (signal.aborted) return;
      this.publish({
        ...this.current,
        phase: 'failed',
        message: 'Movie details could not be loaded.',
      });
    }
  }

  updateDraft(
    next: MovieDraft,
    options: { originalTitleTouched?: boolean } = {},
  ): void {
    if (
      this.current.phase === 'submitting' ||
      this.current.phase === 'uncertain' ||
      this.current.draftChoices.length
    )
      return;
    if (options.originalTitleTouched) this.originalTitleTouched = true;
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

  toggleGenre(genreId: string, checked: boolean): void {
    const current = [...(this.current.draft.genreIds ?? [])];
    const next = checked
      ? current.includes(genreId)
        ? current
        : [...current, genreId]
      : current.filter((id) => id !== genreId);
    this.updateDraft({ ...this.current.draft, genreIds: Object.freeze(next) });
  }

  toggleStaleReview(): void {
    this.publish({
      ...this.current,
      staleReviewOpen: !this.current.staleReviewOpen,
    });
  }

  openDiscardDialog(): void {
    this.publish({ ...this.current, discardDialogOpen: true });
  }

  closeDiscardDialog(): void {
    this.publish({ ...this.current, discardDialogOpen: false });
  }

  async confirmDiscard(): Promise<void> {
    if (!this.writeContext) return;
    if (!(await this.draftSync.discard(this.writeContext))) {
      this.publish({
        ...this.current,
        phase: 'failed',
        message:
          'Discard is not confirmed. Retry the original discard before leaving.',
      });
      return;
    }
    this.publish({ ...this.current, discardDialogOpen: false });
    if (this.current.saved) {
      this.publish({
        ...this.current,
        draft: movieToDraft(this.current.saved),
        phase: 'idle',
        message: '',
        fieldErrors: Object.freeze([]),
        discardDialogOpen: false,
      });
      return;
    }
    this.publish({
      ...this.current,
      draft: EMPTY_MOVIE_DRAFT,
      phase: 'idle',
      message: '',
      fieldErrors: Object.freeze([]),
      discardDialogOpen: false,
    });
  }

  private publishCreateBootstrap(genres: EditorState['genres']): void {
    this.publish({
      ...this.current,
      genres,
      draft: EMPTY_MOVIE_DRAFT,
      summaryTitle: 'Create movie',
    });
  }

  private async bootstrapExistingMovie(
    genres: EditorState['genres'],
    context: WriteContext,
    signal: AbortSignal,
  ): Promise<void> {
    if (!this.movieId) {
      this.publish({
        ...this.current,
        phase: 'target-changed',
        message: 'This movie is not available for editing.',
        genres,
      });
      return;
    }
    const movie = await fetchManagedMovie(
      this.request,
      this.movieId,
      context.csrf,
      signal,
    );
    if (signal.aborted) return;
    if (!movie || movie.state !== 'ACTIVE' || movie.kind === 'TV') {
      this.publish({
        ...this.current,
        phase: 'target-changed',
        message:
          movie?.kind === 'TV'
            ? 'TV entries are read-only in movie management.'
            : 'This movie is now archived or unavailable.',
        genres,
        saved: movie,
        current: movie,
      });
      return;
    }
    const draft = movieToDraft(movie);
    this.draftSync.reset({
      catalogOperationId: null,
    });
    this.publish({
      ...this.current,
      genres,
      saved: movie,
      current: movie,
      draft,
      summaryTitle: `Edit movie`,
      mediaOutage: movie.artworkUnavailable,
    });
  }

  private async restoreBackedDraft(
    context: WriteContext,
    signal: AbortSignal,
  ): Promise<void> {
    const drafts = await this.draftSync.load(context, signal);
    if (signal.aborted || drafts.length === 0) return;
    if (drafts.length > 1) {
      this.publish({
        ...this.current,
        phase: 'idle',
        draftChoices: Object.freeze(
          drafts.map((draft) => ({
            id: draft.id,
            label: (draft.values as MovieDraft).title || 'Untitled movie draft',
            expiresAt: draft.expiresAt,
          })),
        ),
        message:
          'Choose a retained draft or deliberately start a new draft. No saved draft was selected automatically.',
      });
      return;
    }
    const draft = drafts[0];
    if (draft) await this.applyBackedDraft(draft, signal);
  }

  private async applyBackedDraft(
    draft: DraftRecord,
    signal: AbortSignal,
  ): Promise<void> {
    this.publish({ ...this.current, draftChoices: Object.freeze([]) });
    this.draftSync.adopt(draft);
    if (draft.operationId) {
      this.publish({
        ...this.current,
        phase: 'uncertain',
        pendingOperationId: draft.operationId,
        message:
          'Check the original save before continuing this retained draft.',
      });
      await this.checkOriginalResult();
      if (signal.aborted || this.current.phase !== 'failed') return;
    }
    const stale =
      this.current.saved && draft.baseVersion !== this.current.saved.version;
    this.originalTitleTouched = Object.prototype.hasOwnProperty.call(
      draft.values,
      'originalTitle',
    );
    this.publish({
      ...this.current,
      draft: draft.values as MovieDraft,
      phase: stale ? 'stale' : 'idle',
      pendingOperationId: null,
      draftBaseKnown: !stale,
      message: stale
        ? 'The saved movie changed. Compare its current values with the retained draft.'
        : 'Saved draft recovered.',
      staleReviewOpen: !!stale,
    });
  }

  async selectRetainedDraft(id: string): Promise<void> {
    if (
      !this.writeContext ||
      this.current.phase === 'uncertain' ||
      this.current.phase === 'submitting'
    )
      return;
    const signal = this.active?.signal ?? new AbortController().signal;
    try {
      const drafts = await this.draftSync.load(this.writeContext, signal);
      const selected = drafts.find((draft) => draft.id === id);
      if (!selected || signal.aborted) {
        this.publish({
          ...this.current,
          message:
            'That retained draft is no longer available. Choose another draft or start again.',
        });
        return;
      }
      await this.applyBackedDraft(selected, signal);
    } catch {
      this.publish({
        ...this.current,
        message:
          'The retained draft could not be checked. Retry before continuing.',
      });
    }
  }

  startNewDraft(): void {
    if (!this.current.draftChoices.length || this.current.phase === 'uncertain')
      return;
    this.draftSync.reset();
    this.publish({
      ...this.current,
      draftChoices: Object.freeze([]),
      draftBaseKnown: true,
      phase: 'idle',
      draft: this.current.saved
        ? movieToDraft(this.current.saved)
        : EMPTY_MOVIE_DRAFT,
      message:
        'Started a new draft. Other retained drafts remain available after reload.',
    });
  }

  private reviewedStale(reviewed: boolean): boolean {
    return (
      reviewed && this.current.phase === 'stale' && this.current.staleReviewOpen
    );
  }

  async rebaseAfterReview(reviewed: boolean): Promise<void> {
    if (!this.reviewedStale(reviewed)) return;
    if (!this.writeContext || !this.movieId) return;
    const reviewedVersion = this.current.current?.version;
    try {
      const latest = await fetchManagedMovie(
        this.request,
        this.movieId,
        this.writeContext.csrf,
      );
      if (!latest || latest.state !== 'ACTIVE' || latest.kind !== 'MOVIE') {
        this.publish({
          ...this.current,
          phase: 'target-changed',
          current: latest,
          message:
            'This movie is no longer available for editing. The draft is retained.',
        });
        return;
      }
      if (latest.version !== reviewedVersion) {
        this.publish({
          ...this.current,
          current: latest,
          message:
            'Current values changed again. Review the new values before continuing.',
        });
        return;
      }
      const draft = this.rebaseDraft(latest);
      this.pendingWriteId = null;
      this.publish({
        ...this.current,
        saved: latest,
        current: latest,
        draft,
        draftBaseKnown: true,
        phase: 'idle',
        pendingOperationId: null,
        staleReviewOpen: false,
        message:
          'Current values reviewed. Your draft is retained; save when ready to apply it.',
      });
      this.draftSync.schedule(
        this.current.draft,
        this.writeContext,
        latest.version,
      );
    } catch {
      this.publish({
        ...this.current,
        message:
          'The current record could not be checked. Review again before continuing.',
      });
    }
  }

  private rebaseText<K extends 'title' | 'synopsis'>(
    field: K,
    latest: NonNullable<EditorState['saved']>,
  ): string {
    const value = this.current.draft[field];
    return value !== undefined && value !== this.current.saved?.[field]
      ? (value ?? '')
      : latest[field];
  }

  private rebaseGenres(
    latest: NonNullable<EditorState['saved']>,
  ): readonly string[] {
    const baseGenres = [...(this.current.saved?.genreIds ?? [])]
      .sort()
      .join(',');
    const draftGenres = [
      ...(this.current.draft.genreIds ?? this.current.saved?.genreIds ?? []),
    ]
      .sort()
      .join(',');
    return draftGenres !== baseGenres
      ? (this.current.draft.genreIds ?? [])
      : latest.genreIds;
  }

  private rebaseDraft(latest: NonNullable<EditorState['saved']>): MovieDraft {
    if (!this.current.saved || !this.current.draftBaseKnown)
      return this.current.draft;
    return {
      ...rebaseMovieMetadata(movieToDraft(this.current.saved), this.current.draft, movieToDraft(latest)),
      title: this.rebaseText('title', latest),
      synopsis: this.rebaseText('synopsis', latest),
      originalTitle: this.originalTitleTouched
        ? (this.current.draft.originalTitle ?? null)
        : latest.originalTitle,
      genreIds: this.rebaseGenres(latest),
    };
  }

  private async backSubmission(operationId: string): Promise<boolean> {
    if (!this.writeContext) return false;
    const backed = await this.draftSync.prepareSubmission(
      operationId,
      this.current.draft,
      this.writeContext,
      this.current.saved?.version ?? null,
    );
    if (backed) return true;
    this.pendingWriteId = null;
    this.publish({
      ...this.current,
      phase: 'failed',
      pendingOperationId: null,
      message:
        'Changes are not backed up yet. Retry after the original draft save is confirmed.',
    });
    return false;
  }

  private submissionBlocked(): boolean {
    return (
      !!this.pendingWriteId ||
      this.current.draftChoices.length > 0 ||
      ['submitting', 'uncertain', 'stale', 'target-changed'].includes(
        this.current.phase,
      ) ||
      (this.mode === 'create' && this.current.saved !== null)
    );
  }

  private beginSubmit(): {
    operationId: string;
    generation: number;
    signal: AbortSignal;
  } | null {
    if (this.submissionBlocked()) return null;
    if (!this.writeContext || !this.policy) return null;
    const fieldErrors = validateMovieDraft(
      this.current.draft,
      this.policy,
      this.mode,
    );
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
    const generation = this.begin();
    const signal = this.active?.signal;
    if (!signal) return null;
    this.publish({
      ...this.current,
      phase: 'submitting',
      pendingOperationId: operationId,
      fieldErrors: Object.freeze([]),
      message: '',
    });
    return { operationId, generation, signal };
  }

  async submit(): Promise<void> {
    const started = this.beginSubmit();
    if (!started) return;
    const { operationId, generation, signal } = started;
    try {
      if (!(await this.backSubmission(operationId))) return;
      if (this.mode === 'create') {
        await this.submitCreate(operationId, generation, signal);
        return;
      }
      await this.submitUpdate(operationId, generation, signal);
    } catch (error) {
      await this.handleSubmitFailure(error, generation, operationId);
    }
  }

  private async submitCreate(
    operationId: string,
    generation: number,
    signal: AbortSignal,
  ): Promise<void> {
    if (!this.writeContext) return;
    const input = draftToCreateInput(this.current.draft);
    const outcome = await createMovie(
      this.request,
      operationId,
      this.writeContext.policyRevision,
      input,
      this.writeContext.csrf,
      signal,
    );
    if (generation !== this.writeGeneration) return;
    await this.finishWrite(
      outcome.receipt,
      outcome.problemCode,
      operationId,
      outcome.resultMovieId,
      outcome.resultVersion,
    );
  }

  private async submitUpdate(
    operationId: string,
    generation: number,
    signal: AbortSignal,
  ): Promise<void> {
    if (!this.writeContext) return;
    const saved = this.current.saved;
    if (!saved || !this.movieId) return;
    const patch = draftToUpdatePatch(
      saved,
      this.current.draft,
      this.originalTitleTouched,
    );
    if (patchIsEmpty(patch)) {
      this.pendingWriteId = null;
      this.publish({
        ...this.current,
        phase: 'idle',
        pendingOperationId: null,
        message: 'No changes to save.',
      });
      return;
    }
    const outcome = await updateMovie(
      this.request,
      operationId,
      this.movieId,
      saved.version,
      this.writeContext.policyRevision,
      patch,
      this.writeContext.csrf,
      signal,
    );
    if (generation !== this.writeGeneration) return;
    await this.finishWrite(
      outcome.receipt,
      outcome.problemCode,
      operationId,
      outcome.resultMovieId ?? this.movieId,
      outcome.resultVersion,
    );
  }

  private async handleSubmitFailure(
    error: unknown,
    generation: number,
    operationId: string,
  ): Promise<void> {
    if (generation !== this.writeGeneration) return;
    if (error instanceof TransportFailure && error.kind === 'rejected') {
      if (error.code === 'CONFLICT') {
        this.pendingWriteId = null;
        await this.enterStaleReview();
        return;
      }
      this.pendingWriteId = null;
      this.publish({
        ...this.current,
        phase: 'failed',
        pendingOperationId: null,
        message: problemMessage(error.code),
      });
      return;
    }
    this.pendingWriteId = null;
    this.publish({
      ...this.current,
      phase: 'uncertain',
      pendingOperationId: operationId,
      message:
        'Save result not confirmed. Check the original operation before repeating it.',
    });
  }

  private async applyOriginalReceipt(
    receipt: Awaited<ReturnType<typeof recoverCatalogOperation>>,
    operationId: string,
  ): Promise<void> {
    if (
      receipt?.state === 'EXPIRED' ||
      (receipt && 'resultExpired' in receipt && receipt.resultExpired === true)
    ) {
      this.publish({
        ...this.current,
        phase: 'uncertain',
        message:
          'Original save result expired. Review the current record; do not repeat the original change.',
      });
      return;
    }
    if (receipt?.state === 'SUCCEEDED') {
      const movieId = receipt.resultId ?? this.movieId;
      await this.refreshAfterSuccess(movieId, 'resolved', receipt.version);
      this.publish({
        ...this.current,
        phase: 'resolved',
        pendingOperationId: operationId,
        message:
          'Original save confirmed. The current record has been refreshed; no repeat submission is needed.',
      });
      this.pendingWriteId = null;
      return;
    }
    if (receipt?.state === 'FAILED') {
      this.pendingWriteId = null;
      this.publish({
        ...this.current,
        phase: 'failed',
        pendingOperationId: null,
        message: problemMessage(receipt.problemCode),
      });
      return;
    }
    this.publish({
      ...this.current,
      phase: 'uncertain',
      message:
        'Save result not confirmed. Check the original operation before repeating it.',
    });
  }

  async checkOriginalResult(): Promise<void> {
    const operationId = this.current.pendingOperationId;
    if (!operationId || !this.writeContext) return;
    const generation = this.begin();
    const signal = this.active?.signal;
    if (!signal) return;
    try {
      const receipt = await recoverCatalogOperation(
        this.request,
        operationId,
        this.writeContext.csrf,
        signal,
      );
      if (generation !== this.writeGeneration) return;
      await this.applyOriginalReceipt(receipt, operationId);
    } catch {
      this.publish({
        ...this.current,
        phase: 'uncertain',
        message:
          'Save result not confirmed. Check the original operation before repeating it.',
      });
    }
  }

  dispose(): void {
    this.active?.abort();
    this.draftSync.dispose();
    this.listeners.clear();
  }

  private async finishWrite(
    receipt: import('../session/commands').IdentityOutcome,
    problemCode: string | null,
    operationId: string,
    movieId: string | null,
    nextVersion?: string | null,
  ): Promise<void> {
    const { state } = receipt;
    if (receipt.resultExpired || state === 'EXPIRED') {
      this.pendingWriteId = operationId;
      this.publish({ ...this.current, phase: 'uncertain', pendingOperationId: operationId,
        message: 'Original save result expired. Review the current record; do not repeat the original change.' });
      return;
    }
    if (state === 'SUCCEEDED') {
      await this.refreshAfterSuccess(movieId, 'saved', nextVersion ?? null);
      this.pendingWriteId = null;
      this.publish({
        ...this.current,
        phase: 'saved',
        pendingOperationId: null,
        message: 'Movie saved.',
        movieId: movieId ?? this.movieId,
      });
      return;
    }
    if (state === 'FAILED') {
      this.pendingWriteId = null;
      if (problemCode === 'STALE_VERSION') {
        await this.enterStaleReview();
        return;
      }
      this.publish({
        ...this.current,
        phase: 'failed',
        pendingOperationId: null,
        fieldErrors: Object.freeze([]),
        message: problemMessage(problemCode),
      });
      return;
    }
    this.pendingWriteId = operationId;
    this.publish({
      ...this.current,
      phase: 'uncertain',
      pendingOperationId: operationId,
      message:
        'Save result not confirmed. Check the original operation before repeating it.',
    });
  }

  private async enterStaleReview(): Promise<void> {
    if (!this.writeContext || !this.movieId) return;
    const refreshed = await fetchManagedMovie(
      this.request,
      this.movieId,
      this.writeContext.csrf,
      this.active?.signal,
    );
    this.publish({
      ...this.current,
      phase: 'stale',
      pendingOperationId: null,
      current: refreshed,
      staleReviewOpen: false,
      message: problemMessage('STALE_VERSION'),
    });
  }

  private async refreshAfterSuccess(
    movieId: string | null,
    phase: 'saved' | 'resolved',
    version?: string | null,
  ): Promise<void> {
    if (!this.writeContext || !movieId) throw new Error('Saved movie identity unavailable.');
    const movie = await fetchManagedMovie(
      this.request,
      movieId,
      this.writeContext.csrf,
      this.active?.signal,
    );
    if (!movie || movie.id !== movieId || movie.kind !== 'MOVIE' ||
        (version && BigInt(movie.version) < BigInt(version)))
      throw new Error('Saved movie result could not be verified.');
    const saved = movie;
    this.originalTitleTouched = false;
    await this.draftSync.discard(this.writeContext);
    this.publish({
      ...this.current,
      phase,
      saved,
      current: saved,
      draft: movieToDraft(saved),
      mediaOutage: saved.artworkUnavailable,
    });
  }

  private begin(): number {
    this.writeGeneration += 1;
    this.active?.abort();
    this.active = new AbortController();
    return this.writeGeneration;
  }

  private publish(state: EditorState): void {
    this.current = Object.freeze(state);
    this.listeners.forEach((listener) => listener(this.current));
  }
}

export function createMovieEditorController(
  request: GraphqlRequest,
  draftSource: DraftSource,
  mode: 'create' | 'edit',
  movieId: string | null,
): MovieEditorController {
  return new MovieEditorController(request, draftSource, mode, movieId);
}
