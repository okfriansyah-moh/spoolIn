import { describe, expect, it } from 'vitest';
import { pollDelayMs } from './polling';

describe('upload poll schedule', () => {
  it('caps delay at five seconds', () => {
    expect(pollDelayMs(8)).toBe(5000);
  });
});

import { afterEach, vi } from 'vitest';
import { pollUntilArtworkAttached } from './upload-poll';
import type { GraphqlRequest } from '../../transport/graphql';

const upload = '22222222-2222-4222-8222-222222222222';
const title = '33333333-3333-4333-8333-333333333333';
function receipt(state = 'SUCCEEDED', resultExpired = false) {
  return {
    operation: {
      id: upload,
      state,
      resultId: title,
      version: '9007199254740993',
      resultExpired,
      problem: null,
    },
  };
}
function movie(version = '9007199254740993') {
  return {
    managedTitle: {
      id: title,
      kind: 'MOVIE',
      state: 'ACTIVE',
      title: 'Movie',
      version,
      release: { year: 2026 },
      genres: [],
      artwork: { url: '/artwork/current' },
    },
  };
}
function poll(request: GraphqlRequest, onUpdate = vi.fn()) {
  return pollUntilArtworkAttached(
    request,
    'csrf',
    upload,
    title,
    new AbortController().signal,
    () => true,
    onUpdate,
  );
}
afterEach(() => vi.useRealTimers());

it('requires the exact current upload receipt and committed title version before confirming attachment', async () => {
  const request = vi.fn<GraphqlRequest>();
  request.mockResolvedValueOnce(receipt()).mockResolvedValueOnce(movie());
  expect(await poll(request)).toBe(true);
  expect(request.mock.calls[0]?.[1]).toEqual({ id: upload });
  expect(request.mock.calls[0]?.[0]).toContain(
    'resultId version resultExpired',
  );
  expect(request.mock.calls[1]?.[1]).toEqual({ id: title });
});

it('an expired success cannot use existing artwork as proof of this upload', async () => {
  const request = vi.fn<GraphqlRequest>(async () => receipt('SUCCEEDED', true));
  const update = vi.fn();
  expect(await poll(request, update)).toBe(false);
  expect(request).toHaveBeenCalledTimes(1);
  expect(update).toHaveBeenLastCalledWith(
    expect.objectContaining({ stage: 'uncertain', canResumeCheck: true }),
  );
});

it('terminal failure remains failed instead of being converted to unknown or a saved image', async () => {
  const request = vi.fn<GraphqlRequest>(async () => receipt('FAILED'));
  const update = vi.fn();
  expect(await poll(request, update)).toBe(false);
  expect(request).toHaveBeenCalledTimes(1);
  expect(update).toHaveBeenLastCalledWith(
    expect.objectContaining({ stage: 'failed', canResumeCheck: false }),
  );
});

it('refuses mismatched original result bindings', async () => {
  const wrongOperation = receipt();
  wrongOperation.operation.id = title;
  const wrongTarget = receipt();
  wrongTarget.operation.resultId = upload;
  const missingVersion = receipt();
  missingVersion.operation.version = '';
  for (const value of [wrongOperation, wrongTarget, missingVersion]) {
    const request = vi.fn<GraphqlRequest>(async () => value);
    await expect(poll(request)).rejects.toThrow();
    expect(request).toHaveBeenCalledTimes(1);
  }
});

it('a prior movie projection cannot satisfy a newer committed attachment version', async () => {
  vi.useFakeTimers();
  const request = vi.fn<GraphqlRequest>();
  request
    .mockResolvedValueOnce(receipt())
    .mockResolvedValueOnce(movie('9007199254740992'))
    .mockResolvedValueOnce(receipt())
    .mockResolvedValueOnce(movie());
  const result = poll(request);
  await vi.advanceTimersByTimeAsync(500);
  expect(await result).toBe(true);
  expect(request).toHaveBeenCalledTimes(4);
});

it('waits through missing and pending receipts before confirming attachment', async () => {
  vi.useFakeTimers();
  const request = vi.fn<GraphqlRequest>();
  request
    .mockResolvedValueOnce({ operation: null })
    .mockResolvedValueOnce(receipt('PENDING'))
    .mockResolvedValueOnce(receipt())
    .mockResolvedValueOnce(movie());
  const update = vi.fn();
  const result = poll(request, update);
  await vi.advanceTimersByTimeAsync(5000);
  expect(await result).toBe(true);
  expect(update).toHaveBeenCalledWith(
    expect.objectContaining({ stage: 'processing' }),
  );
  expect(request).toHaveBeenCalledTimes(4);
});

it('does not confirm an absent, archived or different movie', async () => {
  const archived = movie();
  archived.managedTitle.state = 'ARCHIVED';
  const different = movie();
  different.managedTitle.id = upload;
  for (const projection of [{ managedTitle: null }, archived, different]) {
    const request = vi.fn<GraphqlRequest>();
    request.mockResolvedValueOnce(receipt()).mockResolvedValueOnce(projection);
    const update = vi.fn();
    expect(await poll(request, update)).toBe(false);
    expect(update).toHaveBeenLastCalledWith(
      expect.objectContaining({ stage: 'failed' }),
    );
  }
});

it('discards results when the current upload changes at either request boundary', async () => {
  for (const changesAfter of [1, 2]) {
    const request = vi.fn<GraphqlRequest>();
    request.mockResolvedValueOnce(receipt()).mockResolvedValueOnce(movie());
    let checks = 0;
    const update = vi.fn();
    expect(
      await pollUntilArtworkAttached(
        request,
        'csrf',
        upload,
        title,
        new AbortController().signal,
        () => ++checks < changesAfter,
        update,
      ),
    ).toBe(false);
    expect(request).toHaveBeenCalledTimes(changesAfter);
    expect(update).not.toHaveBeenCalled();
  }
});

it('stops waiting when cancelled before or during the delay', async () => {
  vi.useFakeTimers();
  for (const alreadyAborted of [true, false]) {
    const controller = new AbortController();
    const reason = new Error('Upload replaced');
    if (alreadyAborted) controller.abort(reason);
    const request = vi.fn<GraphqlRequest>(async () => ({ operation: null }));
    const result = pollUntilArtworkAttached(
      request,
      'csrf',
      upload,
      title,
      controller.signal,
      () => true,
      vi.fn(),
    );
    const rejected = expect(result).rejects.toBe(reason);
    await vi.advanceTimersByTimeAsync(0);
    if (!alreadyAborted) controller.abort(reason);
    await rejected;
    expect(request).toHaveBeenCalledTimes(1);
  }
});

it('offers a resumable check after the foreground polling deadline', async () => {
  vi.useFakeTimers();
  const request = vi.fn<GraphqlRequest>(async () => ({ operation: null }));
  const update = vi.fn();
  const result = poll(request, update);
  await vi.advanceTimersByTimeAsync(310000);
  expect(await result).toBe(false);
  expect(update).toHaveBeenLastCalledWith(
    expect.objectContaining({
      stage: 'uncertain',
      canResumeCheck: true,
      canCancel: false,
    }),
  );
});

it('recognizes an explicitly expired receipt without looking up artwork', async () => {
  const request = vi.fn<GraphqlRequest>(async () => receipt('EXPIRED'));
  const update = vi.fn();
  expect(await poll(request, update)).toBe(false);
  expect(request).toHaveBeenCalledTimes(1);
  expect(update).toHaveBeenLastCalledWith(
    expect.objectContaining({ stage: 'uncertain' }),
  );
});
