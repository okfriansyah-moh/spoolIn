import { afterEach, describe, expect, it, vi } from 'vitest';
import { TransportFailure } from '../../transport/graphql';
import type { DraftSource } from '../draft/types';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';

vi.mock('./api', () => ({
  fetchMovieGenres: vi.fn(async () =>
    Object.freeze([Object.freeze({ id: '10000000-0000-4000-8000-000000000010', label: 'Drama' })]),
  ),
  fetchManagedMovie: vi.fn(async () => null),
  createMovie: vi.fn(),
  updateMovie: vi.fn(),
  recoverCatalogOperation: vi.fn(),
}));

import * as api from './api';
import { MovieEditorController } from './editor-controller';

const policy = specimenPolicy();

const context = Object.freeze({
  accountId: '10000000-0000-4000-8000-000000000001',
  csrf: 'A'.repeat(43),
  policyRevision: policy.revision,
});

function draftSource(): DraftSource {
  return {
    recoverOperation: async () => null,
    listDrafts: async () => [],
    saveDraft: async () => ({
      receipt: {
        operationId: '11111111-1111-4111-8111-111111111111',
        state: 'SUCCEEDED',
        resultId: '22222222-2222-4222-8222-222222222222',
        version: '1',
        resultExpired: false,
      },
      problemCode: null,
    }),
    discardDraft: async () => ({
      receipt: {
        operationId: '33333333-3333-4333-8333-333333333333',
        state: 'SUCCEEDED',
        resultId: '22222222-2222-4222-8222-222222222222',
        version: '2',
        resultExpired: false,
      },
      discarded: true,
      problemCode: null,
    }),
  };
}

afterEach(() => {
  vi.clearAllMocks();
});

describe('T017 movie editor controller', () => {
  it('keeps the same pending operation id while submit is uncertain', async () => {
    const request = vi.fn();
    const controller = new MovieEditorController(request, draftSource(), 'create', null);
    await controller.bootstrap(policy, context, new AbortController().signal);
    controller.updateDraft({
      title: 'Harbor Story',
      synopsis: 'A pilot returns home.',
      genreIds: ['10000000-0000-4000-8000-000000000010'],
      release: { year: 2024, month: null, day: null },
    });
    vi.mocked(api.createMovie).mockRejectedValueOnce(new TransportFailure('uncertain'));
    await controller.submit();
    const first = controller.snapshot.pendingOperationId;
    expect(first).toBeTruthy();
    expect(controller.snapshot.phase).toBe('uncertain');
    await controller.submit();
    expect(controller.snapshot.pendingOperationId).toBe(first);
    controller.dispose();
  });

  it('enters stale review when update returns STALE_VERSION', async () => {
    const request = vi.fn();
    const movieId = '190db10d-02b8-437e-8e7c-ece9c105fe2a';
    vi.mocked(api.fetchManagedMovie).mockResolvedValue({
      id: movieId,
      kind: 'MOVIE',
      state: 'ACTIVE',
      title: 'Harbor Story',
      originalTitle: null,
      synopsis: 'Saved synopsis',
      genreIds: ['10000000-0000-4000-8000-000000000010'],
      releaseYear: '2024',
      version: '5',
      artworkUrl: null,
      artworkUnavailable: false,
    });
    vi.mocked(api.updateMovie).mockResolvedValueOnce({
      receipt: {
        operationId: '44444444-4444-4444-8444-444444444444',
        state: 'FAILED',
        resultExpired: false,
        problem: { code: 'STALE_VERSION', retryAfterMs: 0 },
      },
      problemCode: 'STALE_VERSION',
      resultMovieId: movieId,
      resultVersion: null,
    });
    const controller = new MovieEditorController(request, draftSource(), 'edit', movieId);
    await controller.bootstrap(policy, context, new AbortController().signal);
    controller.updateDraft({
      title: 'Harbor Story',
      synopsis: 'Revised synopsis',
      genreIds: ['10000000-0000-4000-8000-000000000010'],
      release: { year: 2024, month: null, day: null },
    });
    await controller.submit();
    expect(controller.snapshot.phase).toBe('stale');
    expect(controller.snapshot.message).toMatch(/changed while you were editing/i);
    controller.dispose();
  });
});
