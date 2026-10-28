import { expect, it, vi } from 'vitest';
import { ArtworkReadQueue } from './read-queue';

it('allows only two concurrent downloads and cancels obsolete queued posters', async () => {
  const queue = new ArtworkReadQueue();
  const release: Array<() => void> = [];
  const read = vi.fn(
    () => new Promise<void>((resolve) => release.push(resolve)),
  );
  const a = queue.run(read, new AbortController().signal);
  const b = queue.run(read, new AbortController().signal);
  const abort = new AbortController();
  const c = queue.run(read, abort.signal);
  const canceled = expect(c).rejects.toMatchObject({ name: 'AbortError' });
  await Promise.resolve();
  expect(read).toHaveBeenCalledTimes(2);
  abort.abort();
  await canceled;
  release.forEach((finish) => finish());
  await Promise.all([a, b]);
  expect(read).toHaveBeenCalledTimes(2);
  await queue.run(async () => 'latest', new AbortController().signal);
});
