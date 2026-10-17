import { describe, expect, it, vi } from 'vitest';
import { fetchAuditPage } from './api';

describe('audit api', () => {
  it('maps empty audit page', async () => {
    const request = vi.fn().mockResolvedValue({ audit: { nodes: [] } });
    const rows = await fetchAuditPage(request, 'csrf', new AbortController().signal);
    expect(rows).toEqual([]);
  });

  it('maps audit nodes', async () => {
    const request = vi.fn().mockResolvedValue({
      audit: {
        nodes: [
          {
            id: '1',
            actorRef: 'account:abc',
            action: 'CREATE_MOVIE',
            subjectKind: 'TITLE',
            subjectId: 't1',
            outcome: 'SUCCEEDED',
            createdAt: '2026-01-01T00:00:00Z',
            changes: '{}',
          },
        ],
      },
    });
    const rows = await fetchAuditPage(request, 'csrf', new AbortController().signal);
    expect(rows[0]?.action).toBe('CREATE_MOVIE');
  });
});
