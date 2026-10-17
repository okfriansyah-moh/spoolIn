import type { GraphqlRequest } from '../../transport/graphql';
import { cancelArtwork, uploadArtworkWithProgress, type UploadArtworkInput } from './api';
import { transferPercent } from './polling';
import { stageLabel, type ArtworkStage } from './progress';
import { pollUntilArtworkAttached } from './upload-poll';

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
    this.publishTransferring(operationId, 0);
    try {
      const contentBase64 = await this.readFileBase64(file, generation);
      if (generation !== this.generation) return;
      const mutation = await this.sendUpload(
        { operationId, titleId, expectedVersion, policyRevision, contentBase64 },
        generation,
        operationId,
      );
      this.activeXhr = null;
      if (generation !== this.generation) return;
      if (mutation.problemCode) {
        this.publishFailed(operationId);
        return;
      }
      this.publishProcessing(operationId);
      const attached = await this.pollAttachment(generation, operationId, titleId, signal);
      if (generation !== this.generation) return;
      if (attached) this.publishSaved(operationId);
      else if (this.view.stage !== 'uncertain') this.publishFailed(operationId);
    } catch (error) {
      this.handleUploadError(error, generation, operationId);
    }
  }

  async cancelActive(uploadOperationId: string | null): Promise<void> {
    if (!uploadOperationId) return;
    this.activeXhr?.abort();
    const cancelId = globalThis.crypto.randomUUID();
    try {
      await cancelArtwork(this.request, this.csrf, cancelId, uploadOperationId);
    } catch {
      // Local transfer is already stopped.
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
    _expectedVersion: string,
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
    const attached = await this.pollAttachment(generation, operationId, titleId, signal);
    if (generation !== this.generation) return;
    if (attached) this.publishSaved(operationId);
    else {
      this.publish({
        stage: 'uncertain',
        transferPercent: 100,
        message: stageLabel('uncertain'),
        operationId,
        canCancel: false,
        canResumeCheck: true,
      });
    }
  }

  private beginPoll(): AbortSignal {
    this.pollAbort?.abort();
    this.pollAbort = new AbortController();
    return this.pollAbort.signal;
  }

  private async pollAttachment(
    generation: number,
    operationId: string,
    titleId: string,
    signal: AbortSignal,
  ): Promise<boolean> {
    return pollUntilArtworkAttached(
      this.request,
      this.csrf,
      operationId,
      titleId,
      signal,
      () => generation === this.generation,
      (partial) => this.publish({ ...this.view, ...partial }),
    );
  }

  private async sendUpload(
    input: UploadArtworkInput,
    generation: number,
    operationId: string,
  ) {
    return uploadArtworkWithProgress(
      this.csrf,
      input,
      (loaded, total) => {
        if (generation !== this.generation) return;
        const percent = transferPercent(loaded, total);
        this.publishTransferring(operationId, percent);
      },
      (xhr) => {
        this.activeXhr = xhr;
      },
    );
  }

  private handleUploadError(error: unknown, generation: number, operationId: string): void {
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
    this.publishFailed(operationId);
  }

  private publishTransferring(operationId: string, percent: number): void {
    this.publish({
      stage: 'transferring',
      transferPercent: percent,
      message: `Transferring… ${percent}%`,
      operationId,
      canCancel: true,
      canResumeCheck: false,
    });
  }

  private publishProcessing(operationId: string): void {
    this.publish({
      stage: 'processing',
      transferPercent: 100,
      message: stageLabel('processing'),
      operationId,
      canCancel: false,
      canResumeCheck: false,
    });
  }

  private publishSaved(operationId: string): void {
    this.publish({
      stage: 'saved',
      transferPercent: 100,
      message: stageLabel('saved'),
      operationId,
      canCancel: false,
      canResumeCheck: false,
    });
  }

  private publishFailed(operationId: string): void {
    this.publish({
      stage: 'failed',
      transferPercent: this.view.transferPercent,
      message: stageLabel('failed'),
      operationId,
      canCancel: false,
      canResumeCheck: Boolean(operationId),
    });
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
