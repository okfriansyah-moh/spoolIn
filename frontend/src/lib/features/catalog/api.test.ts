import { afterEach, expect, it, vi } from 'vitest';
import { fetchTitle } from './api';
import { TransportFailure } from '../../transport/graphql';
afterEach(() => vi.useRealTimers());
it('recovers title details after a temporary read limit', async () => {
  vi.useFakeTimers();
  const request = vi
    .fn()
    .mockRejectedValueOnce(
      new TransportFailure('rejected', 'RATE_LIMITED', 500),
    )
    .mockResolvedValue({ title: null });
  const result = fetchTitle(request, {
    id: '00000000-0000-4000-8000-000000000001',
  });
  await vi.advanceTimersByTimeAsync(499);
  expect(request).toHaveBeenCalledTimes(1);
  await vi.advanceTimersByTimeAsync(1);
  await expect(result).resolves.toBeNull();
  expect(request).toHaveBeenCalledTimes(2);
});
