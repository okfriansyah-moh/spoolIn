import { describe, expect, it, vi } from 'vitest';
import { cancelArtwork } from './api';
import type { GraphqlRequest } from '../../transport/graphql';

const cancellation = '11111111-1111-4111-8111-111111111111';
const upload = '22222222-2222-4222-8222-222222222222';
function result(cancelled: boolean) {
  return {
    cancelArtwork: {
      cancelled,
      operation: {
        id: cancellation,
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

describe('actual cancellation receipt binding', () => {
  it('requires the original cancellation and upload outcomes before reporting cancellation', async () => {
    const request = vi.fn<GraphqlRequest>(async () => result(true));
    expect(await cancelArtwork(request, 'csrf', cancellation, upload)).toBe(
      true,
    );
    expect(request.mock.calls[0]?.[1]).toEqual({
      operationId: cancellation,
      uploadOperationId: upload,
    });
    expect(request.mock.calls[0]?.[0]).toContain('resultExpired');
  });

  it('retains a false effect and a hidden/unknown upload without inventing cancellation', async () => {
    const value = result(false);
    const request = vi.fn<GraphqlRequest>(async () => value);
    expect(await cancelArtwork(request, 'csrf', cancellation, upload)).toBe(
      false,
    );
    request.mockResolvedValue({
      cancelArtwork: {
        cancelled: false,
        uploadOperation: null,
        operation: {
          id: cancellation,
          state: 'FAILED',
          resultExpired: false,
          problem: { code: 'NOT_FOUND' },
        },
      },
    });
    expect(await cancelArtwork(request, 'csrf', cancellation, upload)).toBe(
      false,
    );
  });

  it('refuses pending, expired, mismatched and contradictory successful cancellation bodies', async () => {
    const pending = result(true);
    pending.cancelArtwork.operation.state = 'PENDING';
    const expired = result(true);
    expired.cancelArtwork.operation.resultExpired = true;
    const mismatch = result(true);
    mismatch.cancelArtwork.uploadOperation.id = cancellation;
    const contradictory = result(true);
    contradictory.cancelArtwork.uploadOperation.state = 'SUCCEEDED';
    for (const value of [pending, expired, mismatch, contradictory]) {
      const request = vi.fn<GraphqlRequest>(async () => value);
      await expect(
        cancelArtwork(request, 'csrf', cancellation, upload),
      ).rejects.toThrow();
    }
  });
});

describe('artwork HTTP response classification', () => {
  it.each([
    {
      status: 429,
      errors: [{ extensions: { code: 'RATE_LIMITED' } }],
      kind: 'rejected',
      code: 'RATE_LIMITED',
    },
    {
      status: 503,
      errors: [{ extensions: { code: 'FORBIDDEN' } }],
      kind: 'uncertain',
      code: 'FORBIDDEN',
    },
    {
      status: 200,
      errors: [{ extensions: { code: 'UNEXPECTED' } }],
      kind: 'uncertain',
      code: 'UNAVAILABLE',
    },
  ])(
    'preserves $status/$code rejection certainty',
    async ({ status, errors, kind, code }) => {
      const { uploadArtworkWithProgress } = await import('./api');
      vi.stubGlobal(
        'XMLHttpRequest',
        class {
          status = status;
          responseText = JSON.stringify({ errors });
          upload = { onprogress: null };
          onload: (() => void) | null = null;
          open() {}
          setRequestHeader() {}
          send() {
            this.onload?.();
          }
        },
      );
      try {
        await expect(
          uploadArtworkWithProgress(
            'csrf',
            {
              operationId: upload,
              titleId: cancellation,
              expectedVersion: '1',
              policyRevision: 'revision',
              contentBase64: 'AA==',
            },
            vi.fn(),
          ),
        ).rejects.toMatchObject({ kind, code });
      } finally {
        vi.unstubAllGlobals();
      }
    },
  );
});
