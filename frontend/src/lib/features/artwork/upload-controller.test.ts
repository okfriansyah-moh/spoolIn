import { describe, expect, it, vi } from 'vitest';
import { ArtworkUploadController } from './upload-controller.svelte';
import type { GraphqlRequest } from '../../transport/graphql';

const upload = '22222222-2222-4222-8222-222222222222';
function response(id: unknown, cancelled: boolean) {
  return {
    cancelArtwork: {
      cancelled,
      operation: {
        id,
        state: 'SUCCEEDED',
        resultExpired: false,
        problem: null,
      },
      uploadOperation: {
        id: upload,
        state: 'FAILED',
        resultExpired: false,
        problem: { code: 'CANCELLED' },
      },
    },
  };
}

describe('truthful upload cancellation', () => {
  it('retries an uncertain cancellation using its original operation ID and only then confirms its effect', async () => {
    const request = vi.fn<GraphqlRequest>();
    request.mockRejectedValueOnce(new Error('Lost reply'));
    request.mockImplementation(async (_query, variables) =>
      response(variables.operationId, true),
    );
    const controller = new ArtworkUploadController(request, 'csrf');
    await controller.cancelActive(upload);
    expect(controller.snapshot.stage).toBe('uncertain');
    expect(controller.snapshot.message).not.toBe('Upload cancelled.');
    expect(controller.snapshot.canCancel).toBe(true);
    await controller.cancelActive(upload);
    expect(request.mock.calls[1]?.[1]).toEqual(request.mock.calls[0]?.[1]);
    expect(controller.snapshot.message).toBe('Upload cancelled.');
    expect(controller.snapshot.canResumeCheck).toBe(false);
    controller.dispose();
  });

  it('shows a rejected cancellation separately from server-confirmed cancellation', async () => {
    const request = vi.fn<GraphqlRequest>(async (_query, variables) =>
      response(variables.operationId, false),
    );
    const controller = new ArtworkUploadController(request, 'csrf');
    await controller.cancelActive(upload);
    expect(controller.snapshot.message).toContain(
      'Cancellation was not applied',
    );
    expect(controller.snapshot.stage).toBe('uncertain');
    expect(controller.snapshot.canResumeCheck).toBe(true);
    controller.dispose();
  });

  it('restores an available result check when the original upload lookup fails', async () => {
    const request = vi.fn<GraphqlRequest>(async () => {
      throw new Error('Unavailable');
    });
    const controller = new ArtworkUploadController(request, 'csrf');
    await controller.resumeCheck(upload, 'title', '1');
    expect(controller.snapshot.stage).toBe('uncertain');
    expect(controller.snapshot.canResumeCheck).toBe(true);
    controller.dispose();
  });
});

it('refreshes attached artwork before resume resolves and drops the callback on disposal', async () => {
  const poll = await import('./upload-poll');
  const attachment = vi
    .spyOn(poll, 'pollUntilArtworkAttached')
    .mockResolvedValueOnce(true);
  const controller = new ArtworkUploadController(vi.fn(), 'csrf');
  const saved = vi.fn(() => expect(controller.snapshot.stage).toBe('saved'));
  controller.onSaved = saved;
  try {
    await controller.resumeCheck(upload, 'title', '1');
    expect(saved).toHaveBeenCalledTimes(1);
    expect(saved).toHaveBeenCalledWith(controller.snapshot);
    controller.dispose();
    expect(controller.onSaved).toBeUndefined();
    expect(controller.snapshot.stage).toBe('idle');
  } finally {
    controller.dispose();
    attachment.mockRestore();
  }
});
