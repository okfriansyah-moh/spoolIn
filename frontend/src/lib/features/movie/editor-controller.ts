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
import { movieToDraft } from './projection';
import type { EditorState, WriteContext } from './types';
import { EMPTY_MOVIE_DRAFT } from './types';
import { problemMessage, validateMovieDraft } from './validation';
import type { DraftSource } from '../draft/types';

function initialState(
  mode: 'create' | 'edit',
  movieId: string | null,
): EditorState {
  return Object.freeze({
    phase: 'idle',
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
        return;
      }
      await this.bootstrapExistingMovie(genres, context, signal);
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
    if (this.current.phase === 'submitting') return;
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
    await this.draftSync.discard(this.writeContext);
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

  private publishCreateBootstrap(
    genres: EditorState['genres'],
  ): void {
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
      expectedVersion: movie.version,
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

  private beginSubmit():
    | { operationId: string; generation: number; signal: AbortSignal }
    | null {
    if (
      this.pendingWriteId ||
      this.current.phase === 'submitting' ||
      this.current.phase === 'uncertain'
    ) {
      return null;
    }
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
      outcome.receipt.state,
      outcome.problemCode,
      operationId,
      outcome.resultMovieId,
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
      outcome.receipt.state,
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
    state: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED',
    problemCode: string | null,
    operationId: string,
    movieId: string | null,
    nextVersion?: string | null,
  ): Promise<void> {
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
    if (!this.writeContext || !movieId) return;
    const movie = await fetchManagedMovie(
      this.request,
      movieId,
      this.writeContext.csrf,
      this.active?.signal,
    );
    if (!movie) return;
    const saved = version ? { ...movie, version } : movie;
    this.originalTitleTouched = false;
    this.draftSync.reset({
      catalogOperationId: null,
      expectedVersion: saved.version,
    });
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
