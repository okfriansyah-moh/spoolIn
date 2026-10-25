import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  parseDecision,
  parseProposal,
  posterStillProcessing,
  proposalRequest,
  proposalValue,
} from './api';

const id = '00000000-0000-4000-8000-000000000001';
const proposal = () => ({
  proposalId: id,
  revision: 1,
  contentHash: 'a'.repeat(64),
  challenge: 'review-challenge',
  expiresAt: '2026-10-25T12:00:00Z',
  state: 'READY',
  operationId: id,
  values: { title: 'A title' },
  credits: [{ personId: id }],
  sources: [
    {
      field: 'title',
      url: 'https://example.test/title',
      note: 'Reviewed',
      reuseBasis: 'Licensed',
    },
  ],
  poster: null,
});

afterEach(() => vi.unstubAllGlobals());

describe('proposal review responses', () => {
  it('retains review identity, exact values and source attribution', () => {
    const result = parseProposal(proposal());
    expect(result).toMatchObject({
      proposalId: id,
      revision: 1,
      values: { title: 'A title' },
    });
    expect(result.credits).toEqual([{ personId: id }]);
    expect(result.sources[0]?.note).toBe('Reviewed · Licensed');
    expect(result.poster).toBeNull();
  });

  it.each([0, -1, 1.5, '1', null])(
    'rejects invalid review revision %s',
    (revision) => {
      expect(() => parseProposal({ ...proposal(), revision })).toThrow();
    },
  );

  it.each([`/artwork/${id}`, `/api/movie-posters/${id}`])(
    'accepts the server artwork route %s',
    (url) => {
      expect(
        parseProposal({
          ...proposal(),
          poster: { url, width: 100, height: 200, mediaType: 'image/jpeg' },
        }).poster,
      ).toEqual({ url, width: 100, height: 200, mediaType: 'image/jpeg' });
    },
  );

  it.each([
    'javascript:alert(1)',
    'https://example.test/private',
    '/api/movie-posters/not-an-id',
  ])('rejects unsafe poster route %s', (url) => {
    expect(() =>
      parseProposal({
        ...proposal(),
        poster: { url, width: 100, height: 200 },
      }),
    ).toThrow();
  });

  it('rejects malformed review values, attribution and poster dimensions', () => {
    for (const changed of [
      { values: null },
      { sources: null },
      { contentHash: 1 },
      { credits: [null] },
      { sources: [{ field: 'title', url: 1 }] },
      { poster: { url: `/artwork/${id}`, width: '100', height: 200 } },
    ]) {
      expect(() => parseProposal({ ...proposal(), ...changed })).toThrow();
    }
  });

  it('formats unknowns, empty lists, partial dates and structured values for review', () => {
    expect(proposalValue(null)).toBe('Unknown');
    expect(proposalValue('')).toBe('Unknown');
    expect(proposalValue([])).toBe('None');
    expect(proposalValue(['Drama', 'Comedy'])).toBe('Drama, Comedy');
    expect(proposalValue({ year: 2024, month: 2, day: 3 })).toBe('2024-02-03');
    expect(proposalValue({ year: 2024, month: null, day: 0 })).toBe('2024');
    expect(proposalValue({ title: 'Title' })).toBe('{"title":"Title"}');
    expect(proposalValue(false)).toBe('false');
  });

  it('accepts known decision states and retains problems without inventing a success', () => {
    for (const state of ['PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED']) {
      expect(parseDecision({ state, problem: null })).toEqual({
        state,
        problemCode: null,
      });
    }
    expect(
      parseDecision({ state: 'FAILED', problem: { code: 'CONFLICT' } })
        .problemCode,
    ).toBe('CONFLICT');
    expect(() => parseDecision({ state: 'UNKNOWN' })).toThrow();
    expect(() =>
      parseDecision({ state: 'FAILED', problem: { code: null } }),
    ).toThrow();
    expect(posterStillProcessing({ state: 'PROCESSING' })).toBe(true);
    expect(posterStillProcessing({ state: 'READY' })).toBe(false);
  });
});

describe('proposal HTTP boundary', () => {
  it('reads an escaped proposal identity with same-origin credentials and cancellation', async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValue(new Response('{"state":"READY"}'));
    vi.stubGlobal('fetch', fetcher);
    const signal = new AbortController().signal;
    await expect(
      proposalRequest('id/with spaces', '', { signal }),
    ).resolves.toEqual({ state: 'READY' });
    expect(fetcher).toHaveBeenCalledWith(
      '/api/movie-proposals/id%2Fwith%20spaces',
      expect.objectContaining({
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-store',
        signal,
      }),
    );
  });

  it('requires CSRF before sending a decision and retains its exact body', async () => {
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(new Response('{}'));
    vi.stubGlobal('fetch', fetcher);
    const body = { revision: 2, challenge: 'exact challenge' };
    await expect(
      proposalRequest(id, '/publish', { body }),
    ).rejects.toMatchObject({ code: 'FORBIDDEN' });
    expect(fetcher).not.toHaveBeenCalled();
    await proposalRequest(id, '/publish', { body, csrf: 'memory-only' });
    expect(fetcher).toHaveBeenCalledWith(
      `/api/movie-proposals/${id}/publish`,
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(body),
        headers: expect.objectContaining({ 'X-CSRF-Token': 'memory-only' }),
      }),
    );
  });

  it.each([
    [401, 'UNAUTHENTICATED'],
    [403, 'FORBIDDEN'],
    [409, 'CONFLICT'],
    [503, 'UNAVAILABLE'],
  ])(
    'classifies read HTTP %s without exposing response text',
    async (status, code) => {
      vi.stubGlobal(
        'fetch',
        vi
          .fn<typeof fetch>()
          .mockResolvedValue(new Response('private detail', { status })),
      );
      await expect(proposalRequest(id)).rejects.toMatchObject({
        kind: 'rejected',
        code,
      });
    },
  );

  it('keeps a server failure after a decision uncertain', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn<typeof fetch>()
        .mockResolvedValue(new Response('', { status: 500 })),
    );
    await expect(
      proposalRequest(id, '/publish', { body: {}, csrf: 'memory-only' }),
    ).rejects.toMatchObject({ kind: 'uncertain', code: 'UNAVAILABLE' });
  });
});
