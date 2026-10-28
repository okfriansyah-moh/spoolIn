import { safeArtwork } from '../../components/safe-url';
import { TransportFailure } from '../../transport/graphql';
import { recoverPublicRead } from '../../transport/read-recovery';
import { ArtworkReadQueue } from './read-queue';

const queue = new ArtworkReadQueue();
const MAX_ARTWORK_BYTES = 8 * 1024 * 1024;

function rateDelay(response: Response): number {
  const seconds = Number(response.headers.get('Retry-After'));
  return Number.isFinite(seconds) && seconds > 0
    ? Math.min(seconds, 86400) * 1000
    : 1000;
}

async function validatedArtwork(response: Response): Promise<Blob> {
  if (response.status === 429)
    throw new TransportFailure('rejected', 'RATE_LIMITED', rateDelay(response));
  if (response.status >= 500) throw new TransportFailure('unavailable');
  if (!response.ok) throw new Error('Artwork unavailable');
  const blob = await response.blob();
  if (
    blob.size > MAX_ARTWORK_BYTES ||
    !['image/jpeg', 'image/png', 'image/webp'].includes(blob.type)
  )
    throw new Error('Unsupported artwork');
  return blob;
}

async function download(url: string, signal: AbortSignal): Promise<Blob> {
  const attempt = new AbortController();
  const cancel = () => attempt.abort();
  signal.addEventListener('abort', cancel, { once: true });
  if (signal.aborted) attempt.abort();
  const deadline = globalThis.setTimeout(cancel, 10000);
  try {
    const response = await globalThis.fetch(url, {
      signal: attempt.signal,
      credentials: 'same-origin',
      cache: 'no-store',
    });
    return await validatedArtwork(response);
  } catch (error) {
    if (signal.aborted || error instanceof TransportFailure) throw error;
    if (attempt.signal.aborted || error instanceof TypeError)
      throw new TransportFailure('unavailable');
    throw error;
  } finally {
    globalThis.clearTimeout(deadline);
    signal.removeEventListener('abort', cancel);
  }
}

export async function readArtwork(
  url: string,
  signal: AbortSignal,
): Promise<string> {
  if (!safeArtwork(url)) throw new Error('Invalid artwork URL');
  const blob = await queue.run(
    () => recoverPublicRead(() => download(url, signal), signal),
    signal,
  );
  if (signal.aborted) throw new Error('Read canceled');
  return URL.createObjectURL(blob);
}
