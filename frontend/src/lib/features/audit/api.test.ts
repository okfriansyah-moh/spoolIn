import { describe, expect, it, vi } from 'vitest';
import { fetchAuditPage } from './api';

describe('audit api', () => {
  it('maps empty audit page', async () => {
    const request = vi.fn().mockResolvedValue({ audit: { nodes: [], pageInfo: { endCursor: null, hasNextPage: false } } });
    const rows = await fetchAuditPage(request, 'csrf', new AbortController().signal);
    expect(rows.nodes).toEqual([]);
  });

  it('maps audit nodes', async () => {
    const request = vi.fn().mockResolvedValue({
      audit: {
        pageInfo: { endCursor: null, hasNextPage: false },
        nodes: [
          {
            id: '1',
            actorRef: 'account:abc',
            action: 'CREATE_MOVIE',
            subjectKind: 'TITLE',
            subjectId: 't1',
            outcome: 'SUCCEEDED',
            at: '2026-01-01T00:00:00Z',
            changes: { synopsis: { before: 'Old', after: 'New' } },
          },
        ],
      },
    });
    const rows = await fetchAuditPage(request, 'csrf', new AbortController().signal);
    expect(rows.nodes[0]?.action).toBe('CREATE_MOVIE');
    expect(rows.nodes[0]?.changes.synopsis).toEqual({ before: 'Old', after: 'New' });
    expect(request.mock.calls[0]?.[0]).toContain('outcome at changes');
    expect(request.mock.calls[0]?.[0]).not.toContain('createdAt');
  });
  it('retains pagination and requests the next page cursor', async () => {
    const request = vi.fn().mockResolvedValue({
      audit: { nodes: [], pageInfo: { endCursor: 'next', hasNextPage: true } },
    });
    const page = await fetchAuditPage(request, 'csrf', new AbortController().signal, 'previous');
    expect(page).toMatchObject({ endCursor: 'next', hasNextPage: true });
    expect(request.mock.calls[0]?.[1]).toEqual({ after: 'previous' });
  });

  it('rejects an incomplete pagination response instead of hiding remaining history', async () => {
    const request = vi.fn().mockResolvedValue({ audit: { nodes: [], pageInfo: { hasNextPage: true } } });
    await expect(fetchAuditPage(request, 'csrf', new AbortController().signal)).rejects.toThrow('cursor');
  });
});
