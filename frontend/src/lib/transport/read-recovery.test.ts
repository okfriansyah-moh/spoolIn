import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { TransportFailure } from './graphql';
import { recoverPublicRead } from './read-recovery';

let clock = Date.now();
beforeEach(() => {
  clock += 60000;
  vi.useFakeTimers();
  vi.setSystemTime(clock);
});
afterEach(() => {
  vi.useRealTimers();
});

it('honors Retry-After and shares the pause with the next public read', async () => {
  vi.useFakeTimers();
  const limited = vi
    .fn()
    .mockRejectedValueOnce(
      new TransportFailure('rejected', 'RATE_LIMITED', 5000),
    )
    .mockResolvedValue('page');
  const first = recoverPublicRead(limited);
  await vi.advanceTimersByTimeAsync(1);
  const other = vi.fn().mockResolvedValue('poster');
  const second = recoverPublicRead(other);
  await vi.advanceTimersByTimeAsync(4998);
  expect(limited).toHaveBeenCalledTimes(1);
  expect(other).not.toHaveBeenCalled();
  await vi.advanceTimersByTimeAsync(1);
  await expect(first).resolves.toBe('page');
  await expect(second).resolves.toBe('poster');
});

it('cancels obsolete sorts during the server pause without retrying them', async () => {
  vi.useFakeTimers();
  const abort = new AbortController();
  const read = vi
    .fn()
    .mockRejectedValue(new TransportFailure('rejected', 'RATE_LIMITED', 5000));
  const result = recoverPublicRead(read, abort.signal);
  const rejected = expect(result).rejects.toMatchObject({ name: 'AbortError' });
  await vi.advanceTimersByTimeAsync(1);
  abort.abort();
  await rejected;
  await vi.advanceTimersByTimeAsync(5000);
  expect(read).toHaveBeenCalledTimes(1);
});

it('keeps retries bounded and never retries permission or missing records', async () => {
  vi.useFakeTimers();
  const failing = vi
    .fn()
    .mockRejectedValue(new TransportFailure('unavailable'));
  const result = recoverPublicRead(failing);
  const rejected = expect(result).rejects.toBeInstanceOf(TransportFailure);
  await vi.runAllTimersAsync();
  await rejected;
  expect(failing).toHaveBeenCalledTimes(2);
  const denied = vi
    .fn()
    .mockRejectedValue(new TransportFailure('rejected', 'FORBIDDEN'));
  await expect(recoverPublicRead(denied)).rejects.toMatchObject({
    code: 'FORBIDDEN',
  });
  expect(denied).toHaveBeenCalledTimes(1);
});
