import { TransportFailure } from './graphql';

let retryAt = 0;
const pauseListeners = new Set<(until: number) => void>();

export function subscribePublicReadPause(
  listener: (until: number) => void,
): () => void {
  pauseListeners.add(listener);
  listener(retryAt > Date.now() ? retryAt : 0);
  return () => {
    pauseListeners.delete(listener);
  };
}

function publishPause(): void {
  pauseListeners.forEach((listener) => listener(retryAt));
}

export function cancelRead(): Error {
  const error = new Error('Read canceled');
  error.name = 'AbortError';
  return error;
}

export function pauseRead(
  milliseconds: number,
  signal?: AbortSignal,
): Promise<void> {
  if (signal?.aborted) return Promise.reject(cancelRead());
  return new Promise((resolve, reject) => {
    const abort = () => {
      globalThis.clearTimeout(timer);
      signal?.removeEventListener('abort', abort);
      reject(cancelRead());
    };
    const timer = globalThis.setTimeout(() => {
      signal?.removeEventListener('abort', abort);
      resolve();
    }, milliseconds);
    signal?.addEventListener('abort', abort, { once: true });
  });
}

export function deferPublicReads(milliseconds: number): void {
  retryAt = Math.max(retryAt, Date.now() + milliseconds);
  publishPause();
}

async function awaitPublicRead(signal?: AbortSignal): Promise<void> {
  while (retryAt > Date.now()) await pauseRead(retryAt - Date.now(), signal);
  if (signal?.aborted) throw cancelRead();
  if (retryAt) {
    retryAt = 0;
    publishPause();
  }
}

function retryableRead(error: unknown): error is TransportFailure {
  return (
    error instanceof TransportFailure &&
    ['RATE_LIMITED', 'UNAVAILABLE'].includes(error.code)
  );
}

/** Only idempotent public reads use this bounded, cancelable recovery. */
export async function recoverPublicRead<T>(
  read: () => Promise<T>,
  signal?: AbortSignal,
): Promise<T> {
  for (let attempt = 0; ; attempt++) {
    await awaitPublicRead(signal);
    try {
      return await read();
    } catch (error) {
      if (signal?.aborted || !retryableRead(error)) throw error;
      if (error.code === 'RATE_LIMITED')
        deferPublicReads(error.retryAfterMs || 1000);
      if (attempt >= 1) throw error;
      if (error.code === 'UNAVAILABLE')
        await pauseRead(250 * (attempt + 1), signal);
    }
  }
}
