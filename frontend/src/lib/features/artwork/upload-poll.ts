import type { GraphqlRequest } from '../../transport/graphql';
import { fetchManagedMovie } from '../movie/api';
import { record } from '../session/parse';
import { operation } from '../session/outcome';
import { FOREGROUND_POLL_CAP_MS, pollDelayMs } from './polling';
import { stageLabel } from './progress';
import type { ArtworkUploadView } from './upload-controller';

function sleep(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) {
      reject(signal.reason ?? new DOMException('Aborted', 'AbortError'));
      return;
    }
    const abort = () => { clearTimeout(timer); reject(signal.reason ?? new DOMException('Aborted', 'AbortError')); };
    const timer = setTimeout(() => { signal.removeEventListener('abort', abort); resolve(); }, ms);
    signal.addEventListener('abort', abort, { once: true });
  });
}

type ArtworkPollStep = 'continue' | 'success' | 'failure' | 'stale' | 'expired';

async function artworkPollStep(
  request: GraphqlRequest,
  operationId: string,
  titleId: string,
  csrf: string,
  signal: AbortSignal,
  isCurrent: () => boolean,
): Promise<ArtworkPollStep> {
  const data = record(await request(`query ArtworkOperation($id: UUID!) {
    operation(id: $id, owner: CATALOG) { id state resultId version resultExpired problem { code retryAfterMs } }
  }`, { id: operationId }, { csrf, signal }));
  if (!isCurrent()) return 'stale';
  if (data.operation === null) return 'continue';
  const receipt = operation(data.operation, operationId);
  if (receipt.resultExpired || receipt.state === 'EXPIRED') return 'expired';
  if (receipt.state === 'FAILED') return 'failure';
  if (receipt.state !== 'SUCCEEDED') return 'continue';
  const committedVersion = attachedVersion(data.operation, titleId);
  const movie = await fetchManagedMovie(request, titleId, csrf, signal);
  if (!isCurrent()) return 'stale';
  return currentAttachment(movie, committedVersion);
}

function attachedVersion(value: unknown, titleId: string): bigint {
  const receipt = record(value);
  if (receipt.resultId !== titleId || typeof receipt.version !== 'string' || !/^[1-9][0-9]*$/.test(receipt.version))
    throw new Error('Confirmed attachment binding unavailable.');
  return BigInt(receipt.version);
}

function currentAttachment(movie: Awaited<ReturnType<typeof fetchManagedMovie>>, committedVersion: bigint): ArtworkPollStep {
  if (!movie || movie.state !== 'ACTIVE') return 'failure';
  return movie.artworkUrl && BigInt(movie.version) >= committedVersion ? 'success' : 'continue';
}

function terminalPollFailure(step: ArtworkPollStep, onUpdate: (view: Partial<ArtworkUploadView>) => void): boolean {
  if (step === 'failure') {
    onUpdate({ stage: 'failed', message: 'Upload failed', canCancel: false, canResumeCheck: false });
    return true;
  }
  if (step === 'expired') {
    onUpdate({ stage: 'uncertain', message: 'The original upload result expired. Inspect the current movie before further changes.',
      canCancel: false, canResumeCheck: true });
    return true;
  }
  return false;
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
    const step = await artworkPollStep(
      request,
      operationId,
      titleId,
      csrf,
      signal,
      isCurrent,
    );
    if (step === 'stale') return false;
    if (terminalPollFailure(step, onUpdate)) return false;
    if (step === 'success') return true;
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
