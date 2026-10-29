import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { readArtwork } from './read';

const url = '/artwork/00000000-0000-4000-8000-000000000001';
let clock = Date.now();
beforeEach(() => {
  clock += 60000;
  vi.useFakeTimers();
  vi.setSystemTime(clock);
});
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

it('recovers a throttled poster after the server delay and returns its validated image', async () => {
  vi.useFakeTimers();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      new Response('', { status: 429, headers: { 'Retry-After': '2' } }),
    )
    .mockResolvedValue(
      new Response(new Blob(['poster'], { type: 'image/png' })),
    );
  vi.stubGlobal('fetch', fetcher);
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:poster');
  const result = readArtwork(url, new AbortController().signal);
  await vi.advanceTimersByTimeAsync(1);
  expect(fetcher).toHaveBeenCalledTimes(1);
  await vi.advanceTimersByTimeAsync(1999);
  await expect(result).resolves.toBe('blob:poster');
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it('does not retry missing or unsafe artwork', async () => {
  const fetcher = vi.fn().mockResolvedValue(new Response('', { status: 404 }));
  vi.stubGlobal('fetch', fetcher);
  await expect(readArtwork(url, new AbortController().signal)).rejects.toThrow(
    'Artwork unavailable',
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
  await expect(
    readArtwork(
      'https://elsewhere.test/image.png',
      new AbortController().signal,
    ),
  ).rejects.toThrow('Invalid artwork');
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it('bounds stalled fetches and releases queue slots for later posters', async () => {
  const fetcher = vi.fn(
    (_url: string, init: RequestInit) =>
      new Promise<Response>((_resolve, reject) => {
        init.signal?.addEventListener('abort', () =>
          reject(new DOMException('Aborted', 'AbortError')),
        );
      }),
  );
  vi.stubGlobal('fetch', fetcher);
  const result = readArtwork(url, new AbortController().signal);
  const rejected = expect(result).rejects.toThrow();
  await vi.advanceTimersByTimeAsync(20250);
  await rejected;
  expect(fetcher).toHaveBeenCalledTimes(2);
  fetcher.mockImplementation(
    async () => new Response(new Blob(['poster'], { type: 'image/png' })),
  );
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:next');
  await expect(readArtwork(url, new AbortController().signal)).resolves.toBe(
    'blob:next',
  );
});
