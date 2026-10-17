import type { GraphqlRequest } from '../../transport/graphql';
import { fetchManagedMovie, recoverCatalogOperation } from '../movie/api';
import { FOREGROUND_POLL_CAP_MS, pollDelayMs } from './polling';
import { stageLabel } from './progress';
import type { ArtworkUploadView } from './upload-controller';

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

export async function pollUntilArtworkAttached(
  request: GraphqlRequest,
  csrf: string,
  operationId: string,
  titleId: string,
  signal: AbortSignal,
  isCurrent: () => boolean,
  onUpdate: (view: Partial<ArtworkUploadView>) => void,
): Promise<boolean> {
  const started = Date.now();
  let attempt = 0;
  while (Date.now() - started < FOREGROUND_POLL_CAP_MS) {
    if (!isCurrent()) return false;
    const receipt = await recoverCatalogOperation(request, operationId, csrf, signal);
    if (!isCurrent()) return false;
    const movie = await fetchManagedMovie(request, titleId, csrf, signal);
    if (receipt?.state === 'FAILED' || receipt?.state === 'EXPIRED') return false;
    if (receipt?.state === 'SUCCEEDED' && movie?.artwork?.assetId) return true;
    onUpdate({
      stage: 'processing',
      transferPercent: 100,
      message: stageLabel('processing'),
      operationId,
      canCancel: false,
      canResumeCheck: false,
    });
    await sleep(pollDelayMs(attempt), signal);
    attempt += 1;
  }
  onUpdate({
    stage: 'uncertain',
    transferPercent: 100,
    message: stageLabel('uncertain'),
    operationId,
    canCancel: false,
    canResumeCheck: true,
  });
  return false;
}
