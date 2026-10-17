import type { GraphqlRequest } from '../../transport/graphql';
import { TransportFailure } from '../../transport/graphql';
import { fetchManagedMovie, recoverCatalogOperation } from '../movie/api';
import { cancelArtwork, uploadArtworkWithProgress, type UploadArtworkInput } from './api';
import {
  FOREGROUND_POLL_CAP_MS,
  pollDelayMs,
  transferPercent,
} from './polling';
import { stageLabel, type ArtworkStage } from './progress';

export type ArtworkUploadView = Readonly<{
  stage: ArtworkStage;
  transferPercent: number;
  message: string;
  operationId: string | null;
  canCancel: boolean;
  canResumeCheck: boolean;
}>;

export const INITIAL_ARTWORK_UPLOAD_VIEW: ArtworkUploadView = Object.freeze({
  stage: 'idle',
  transferPercent: 0,
  message: stageLabel('idle'),
  operationId: null,
  canCancel: false,
  canResumeCheck: false,
});

function sleep(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) {
      reject(signal.reason ?? new DOMException('Aborted', 'AbortError'));
      return;
    }
    const timer = setTimeout(resolve, ms);
    signal.addEventListener(
      'abort',
      () => {
        clearTimeout(timer);
        reject(signal.reason ?? new DOMException('Aborted', 'AbortError'));
      },
      { once: true },
    );
  });
}

export class ArtworkUploadController {
  private generation = 0;
  private view: ArtworkUploadView = INITIAL_ARTWORK_UPLOAD_VIEW;
  private activeXhr: XMLHttpRequest | null = null;
  private pollAbort: AbortController | null = null;
  private readonly listeners = new Set<(view: ArtworkUploadView) => void>();

  constructor(
    private readonly request: GraphqlRequest,
    private readonly csrf: string,
  ) {}

  get snapshot(): ArtworkUploadView {
    return this.view;
  }

  subscribe(listener: (view: ArtworkUploadView) => void): () => void {
    this.listeners.add(listener);
    listener(this.view);
    return () => this.listeners.delete(listener);
  }

  dispose(): void {
    this.generation += 1;
    this.activeXhr?.abort();
    this.activeXhr = null;
    this.pollAbort?.abort();
    this.pollAbort = null;
    this.publish(INITIAL_ARTWORK_UPLOAD_VIEW);
  }

  async uploadFile(
    file: File,
    titleId: string,
    expectedVersion: string,
    policyRevision: string,
  ): Promise<void> {
    this.generation += 1;
    const generation = this.generation;
    const operationId = globalThis.crypto.randomUUID();
    const signal = this.beginPoll();
    this.publish({
      stage: 'transferring',
      transferPercent: 0,
      message: 'Transferring… 0%',
      operationId,
      canCancel: true,
      canResumeCheck: false,
    });
    try {
      const contentBase64 = await this.readFileBase64(file, generation);
      if (generation !== this.generation) return;
      const input: UploadArtworkInput = {
        operationId,
        titleId,
        expectedVersion,
        policyRevision,
        contentBase64,
      };
      const mutation = await uploadArtworkWithProgress(
        this.csrf,
        input,
        (loaded, total) => {
          if (generation !== this.generation) return;
          const percent = transferPercent(loaded, total);
          this.publish({
            stage: 'transferring',
            transferPercent: percent,
            message: `Transferring… ${percent}%`,
            operationId,
            canCancel: true,
            canResumeCheck: false,
          });
        },
        (xhr) => {
          this.activeXhr = xhr;
        },
      );
      this.activeXhr = null;
      if (generation !== this.generation) return;
      if (mutation.problemCode) {
        this.publish({
          stage: 'failed',
          transferPercent: 100,
          message: stageLabel('failed'),
          operationId,
          canCancel: false,
          canResumeCheck: false,
        });
        return;
      }
      this.publish({
        stage: 'processing',
        transferPercent: 100,
        message: stageLabel('processing'),
        operationId,
        canCancel: false,
        canResumeCheck: false,
      });
      const attached = await this.pollUntilSettled(
        generation,
        operationId,
        titleId,
        expectedVersion,
        signal,
      );
      if (generation !== this.generation) return;
      if (attached) {
        this.publish({
          stage: 'saved',
          transferPercent: 100,
          message: stageLabel('saved'),
          operationId,
          canCancel: false,
          canResumeCheck: false,
        });
        return;
      }
      if (this.view.stage === 'uncertain') return;
      this.publish({
        stage: 'failed',
        transferPercent: 100,
        message: stageLabel('failed'),
        operationId,
        canCancel: false,
        canResumeCheck: this.view.canResumeCheck,
      });
    } catch (error) {
      if (generation !== this.generation) return;
      if (error instanceof DOMException && error.name === 'AbortError') {
        this.publish({
          stage: 'failed',
          transferPercent: this.view.transferPercent,
          message: 'Upload cancelled.',
          operationId,
          canCancel: false,
          canResumeCheck: false,
        });
        return;
      }
      this.publish({
        stage: 'failed',
        transferPercent: this.view.transferPercent,
        message: stageLabel('failed'),
        operationId,
        canCancel: false,
        canResumeCheck: Boolean(operationId),
      });
    }
  }

  async cancelActive(uploadOperationId: string | null): Promise<void> {
    if (!uploadOperationId) return;
    this.activeXhr?.abort();
    const cancelId = globalThis.crypto.randomUUID();
    try {
      await cancelArtwork(this.request, this.csrf, cancelId, uploadOperationId);
    } catch {
      // Local transfer is already stopped; server cancel may still be uncertain.
    }
    this.generation += 1;
    this.publish({
      stage: 'failed',
      transferPercent: this.view.transferPercent,
      message: 'Upload cancelled.',
      operationId: uploadOperationId,
      canCancel: false,
      canResumeCheck: true,
    });
  }

  async resumeCheck(
    operationId: string,
    titleId: string,
    expectedVersion: string,
  ): Promise<void> {
    const generation = this.generation;
    const signal = this.beginPoll();
    this.publish({
      ...this.view,
      stage: 'uncertain',
      message: stageLabel('uncertain'),
      operationId,
      canResumeCheck: false,
    });
    const attached = await this.pollUntilSettled(
      generation,
      operationId,
      titleId,
      expectedVersion,
      signal,
      0,
    );
    if (generation !== this.generation) return;
    if (attached) {
      this.publish({
        stage: 'saved',
        transferPercent: 100,
        message: stageLabel('saved'),
        operationId,
        canCancel: false,
        canResumeCheck: false,
      });
      return;
    }
    this.publish({
      stage: 'uncertain',
      transferPercent: 100,
      message: stageLabel('uncertain'),
      operationId,
      canCancel: false,
      canResumeCheck: true,
    });
  }

  private beginPoll(): AbortSignal {
    this.pollAbort?.abort();
    this.pollAbort = new AbortController();
    return this.pollAbort.signal;
  }

  private async pollUntilSettled(
    generation: number,
    operationId: string,
    titleId: string,
    expectedVersion: string,
    signal: AbortSignal,
    pollAttemptStart = 0,
  ): Promise<boolean> {
    const started = Date.now();
    let attempt = pollAttemptStart;
    while (Date.now() - started < FOREGROUND_POLL_CAP_MS) {
      if (generation !== this.generation) return false;
      const receipt = await recoverCatalogOperation(
        this.request,
        operationId,
        this.csrf,
        signal,
      );
      if (generation !== this.generation) return false;
      if (receipt?.state === 'FAILED' || receipt?.state === 'EXPIRED') return false;
      const movie = await fetchManagedMovie(this.request, titleId, this.csrf, signal);
      const hasArtwork = Boolean(movie?.artwork?.assetId);
      if (receipt?.state === 'SUCCEEDED' && hasArtwork) return true;
      if (receipt?.state === 'PENDING' || receipt?.state === 'SUCCEEDED') {
        this.publish({
          stage: 'processing',
          transferPercent: 100,
          message: stageLabel('processing'),
          operationId,
          canCancel: false,
          canResumeCheck: false,
        });
      }
      await sleep(pollDelayMs(attempt), signal);
      attempt += 1;
    }
    if (generation !== this.generation) return false;
    this.publish({
      stage: 'uncertain',
      transferPercent: 100,
      message: stageLabel('uncertain'),
      operationId,
      canCancel: false,
      canResumeCheck: true,
    });
    return false;
  }

  private readFileBase64(file: File, generation: number): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        if (generation !== this.generation) {
          reject(new DOMException('Aborted', 'AbortError'));
          return;
        }
        const value = reader.result;
        if (typeof value !== 'string') {
          reject(new Error('Artwork read failed.'));
          return;
        }
        const comma = value.indexOf(',');
        resolve(comma >= 0 ? value.slice(comma + 1) : value);
      };
      reader.onerror = () => reject(reader.error ?? new Error('Artwork read failed.'));
      reader.readAsDataURL(file);
    });
  }

  private publish(next: ArtworkUploadView): void {
    this.view = Object.freeze(next);
    for (const listener of this.listeners) listener(this.view);
  }
}
