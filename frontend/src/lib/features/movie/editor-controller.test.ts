import { afterEach, describe, expect, it, vi } from 'vitest';
import { TransportFailure } from '../../transport/graphql';
import type { DraftSource } from '../draft/types';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';

vi.mock('./api', () => ({
  fetchMovieGenres: vi.fn(async () =>
    Object.freeze([
      Object.freeze({
        id: '10000000-0000-4000-8000-000000000010',
        label: 'Drama',
      }),
    ]),
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
    saveDraft: async (operationId) => ({
      receipt: {
        operationId,
        state: 'SUCCEEDED',
        resultId: '22222222-2222-4222-8222-222222222222',
        version: '1',
        resultExpired: false,
      },
      problemCode: null,
    }),
    discardDraft: async (operationId) => ({
      receipt: {
        operationId,
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
    const controller = new MovieEditorController(
      request,
      draftSource(),
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    controller.updateDraft({
      title: 'Harbor Story',
      synopsis: 'A pilot returns home.',
      genreIds: ['10000000-0000-4000-8000-000000000010'],
      release: { year: 2024, month: null, day: null },
    });
    vi.mocked(api.createMovie).mockRejectedValueOnce(
      new TransportFailure('uncertain'),
    );
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
    const controller = new MovieEditorController(
      request,
      draftSource(),
      'edit',
      movieId,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    controller.updateDraft({
      title: 'Harbor Story',
      synopsis: 'Revised synopsis',
      genreIds: ['10000000-0000-4000-8000-000000000010'],
      release: { year: 2024, month: null, day: null },
    });
    await controller.submit();
    expect(controller.snapshot.phase).toBe('stale');
    expect(controller.snapshot.message).toMatch(
      /changed while you were editing/i,
    );
    controller.dispose();
  });
});

function retainedMovieDraft(
  targetId: string | null,
  baseVersion: string | null = '5',
) {
  return {
    id: '22222222-2222-4222-8222-222222222222',
    targetKind: 'MOVIE',
    targetId,
    baseVersion,
    values: {
      title: 'Retained title',
      synopsis: 'Unsaved synopsis',
      release: { year: 2024, month: null, day: null },
      genreIds: ['10000000-0000-4000-8000-000000000010'],
    },
    operationId: null,
    version: '17',
    expiresAt: new Date(Date.now() + 3600000).toISOString(),
  };
}

function activeMovie() {
  return {
    id: '190db10d-02b8-437e-8e7c-ece9c105fe2a',
    kind: 'MOVIE' as const,
    state: 'ACTIVE' as const,
    title: 'Committed title',
    originalTitle: null,
    synopsis: 'Committed synopsis',
    genreIds: ['10000000-0000-4000-8000-000000000010'],
    releaseYear: '2024',
    version: '5',
    artworkUrl: null,
    artworkUnavailable: false,
  };
}

describe('backed movie draft reload', () => {
  it('loads exact target values without replacing the committed record', async () => {
    const movie = activeMovie();
    vi.mocked(api.fetchManagedMovie).mockResolvedValue(movie);
    const source = draftSource();
    source.listDrafts = async () => [
      retainedMovieDraft(movie.id),
      retainedMovieDraft('another-target'),
    ];
    const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
    source.saveDraft = save;
    const controller = new MovieEditorController(
      vi.fn(),
      source,
      'edit',
      movie.id,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.saved?.title).toBe('Committed title');
    expect(controller.snapshot.draft.title).toBe('Retained title');
    expect(controller.snapshot.phase).toBe('idle');
    controller.updateDraft({
      ...controller.snapshot.draft,
      synopsis: 'Changed again',
    });
    await controller.submit();
    expect(save.mock.calls[0]?.[2]).toBe('17');
    controller.dispose();
  });

  it('retains a stale draft separately and prevents unreviewed publication', async () => {
    const movie = activeMovie();
    vi.mocked(api.fetchManagedMovie).mockResolvedValue(movie);
    const source = draftSource();
    source.listDrafts = async () => [retainedMovieDraft(movie.id, '3')];
    const controller = new MovieEditorController(
      vi.fn(),
      source,
      'edit',
      movie.id,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.phase).toBe('stale');
    expect(controller.snapshot.current?.synopsis).toBe('Committed synopsis');
    expect(controller.snapshot.draft.synopsis).toBe('Unsaved synopsis');
    const before = vi.mocked(api.updateMovie).mock.calls.length;
    await controller.submit();
    expect(vi.mocked(api.updateMovie).mock.calls.length).toBe(before);
    controller.dispose();
  });

  it('does not select an arbitrary draft when multiple create drafts exist', async () => {
    const source = draftSource();
    const second = {
      ...retainedMovieDraft(null),
      id: '33333333-3333-4333-8333-333333333333',
    };
    source.listDrafts = async () => [retainedMovieDraft(null), second];
    const controller = new MovieEditorController(
      vi.fn(),
      source,
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.draft.title).toBe('');
    expect(controller.snapshot.message).toMatch(
      /No saved draft was selected automatically/,
    );
    controller.dispose();
  });

  it('reconciles a retained original catalog operation before enabling a new save', async () => {
    const source = draftSource();
    const operationId = '44444444-4444-4444-8444-444444444444';
    source.listDrafts = async () => [
      { ...retainedMovieDraft(null), operationId },
    ];
    vi.mocked(api.recoverCatalogOperation).mockResolvedValueOnce(null);
    const controller = new MovieEditorController(
      vi.fn(),
      source,
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.phase).toBe('uncertain');
    expect(controller.snapshot.pendingOperationId).toBe(operationId);
    const before = vi.mocked(api.createMovie).mock.calls.length;
    await controller.submit();
    expect(vi.mocked(api.createMovie).mock.calls.length).toBe(before);
    controller.dispose();
  });
});

describe('explicit movie recovery decisions', () => {
  it('resumes the chosen retained draft with its original Core identity', async () => {
    const source = draftSource();
    const first = retainedMovieDraft(null);
    const second = {
      ...first,
      id: '33333333-3333-4333-8333-333333333333',
      version: '23',
      values: { ...first.values, title: 'Chosen draft' },
    };
    source.listDrafts = async () => [first, second];
    const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
    source.saveDraft = save;
    const controller = new MovieEditorController(
      vi.fn(),
      source,
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.draftChoices).toHaveLength(2);
    controller.updateDraft({ title: 'Must not overwrite an unselected draft' });
    expect(controller.snapshot.draft.title).toBe('');
    await controller.selectRetainedDraft(second.id);
    expect(controller.snapshot.draft.title).toBe('Chosen draft');
    expect(controller.snapshot.draftChoices).toHaveLength(0);
    await controller.submit();
    expect(save.mock.calls[0]?.[1]).toBe(second.id);
    expect(save.mock.calls[0]?.[2]).toBe('23');
    controller.dispose();
  });

  it('requires deliberate review and preserves concurrent edits to untouched fields', async () => {
    const original = activeMovie();
    const latest = { ...original, title: 'Concurrent title', version: '6' };
    vi.mocked(api.fetchManagedMovie)
      .mockResolvedValueOnce(original)
      .mockResolvedValue(latest);
    vi.mocked(api.updateMovie).mockResolvedValueOnce({
      receipt: {
        operationId: 'failed',
        state: 'FAILED',
        resultExpired: false,
        problem: { code: 'STALE_VERSION', retryAfterMs: 0 },
      },
      problemCode: 'STALE_VERSION',
      resultMovieId: original.id,
      resultVersion: null,
    });
    const controller = new MovieEditorController(
      vi.fn(),
      draftSource(),
      'edit',
      original.id,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    controller.updateDraft({
      ...controller.snapshot.draft,
      synopsis: 'My synopsis edit',
    });
    await controller.submit();
    expect(controller.snapshot.phase).toBe('stale');
    await controller.rebaseAfterReview(true);
    expect(controller.snapshot.phase).toBe('stale');
    controller.toggleStaleReview();
    await controller.rebaseAfterReview(false);
    expect(controller.snapshot.phase).toBe('stale');
    await controller.rebaseAfterReview(true);
    expect(controller.snapshot.phase).toBe('idle');
    expect(controller.snapshot.draft.title).toBe('Concurrent title');
    expect(controller.snapshot.draft.synopsis).toBe('My synopsis edit');
    expect(controller.snapshot.saved?.version).toBe('6');
    controller.dispose();
  });

  it('refuses a rebase if the reviewed version has already changed again', async () => {
    const movie = activeMovie();
    const source = draftSource();
    source.listDrafts = async () => [retainedMovieDraft(movie.id, '3')];
    vi.mocked(api.fetchManagedMovie)
      .mockResolvedValueOnce(movie)
      .mockResolvedValue({ ...movie, version: '6' });
    const controller = new MovieEditorController(
      vi.fn(),
      source,
      'edit',
      movie.id,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    await controller.rebaseAfterReview(true);
    expect(controller.snapshot.phase).toBe('stale');
    expect(controller.snapshot.current?.version).toBe('6');
    expect(controller.snapshot.saved?.version).toBe('5');
    expect(controller.snapshot.message).toMatch(/changed again/);
    controller.dispose();
  });
});
