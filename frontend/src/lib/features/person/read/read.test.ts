/**
 * T013 – Public person read unit tests.
 *
 * Covers:
 * - fetchPeople: successful response shape extraction
 * - fetchPerson: null when person absent (archived/deleted → unavailable)
 * - fetchPersonCredits: credit shape
 * - Archived person not returned (AC-022, AC-004)
 */
import { describe, it, expect, vi } from 'vitest';
import { fetchPeople, fetchPerson, fetchPersonCredits } from './api';
import type { GraphqlRequest } from '../../../transport/graphql';

function mockRequest(data: unknown): GraphqlRequest {
  return vi.fn(async () => data) as unknown as GraphqlRequest;
}

describe('fetchPeople', () => {
  it('extracts people nodes and pageInfo from response', async () => {
    const payload = {
      people: {
        nodes: [
          {
            id: 'uuid-p-a',
            name: 'Alex',
            biography: 'Bio',
            state: 'ACTIVE',
            version: '1',
          },
        ],
        pageInfo: { endCursor: null, hasNextPage: false },
      },
    };
    const req = mockRequest(payload);
    const result = await fetchPeople(req, {});
    expect(result.nodes).toHaveLength(1);
    expect(result.nodes[0]!.name).toBe('Alex');
    expect(result.pageInfo.hasNextPage).toBe(false);
  });

  it('handles empty nodes list', async () => {
    const payload = {
      people: { nodes: [], pageInfo: { endCursor: null, hasNextPage: false } },
    };
    const req = mockRequest(payload);
    const result = await fetchPeople(req, {});
    expect(result.nodes).toHaveLength(0);
  });
});

describe('fetchPerson', () => {
  it('returns person detail when found', async () => {
    const payload = {
      person: {
        id: 'uuid-p-a',
        name: 'Alex',
        biography: null,
        state: 'ACTIVE',
        version: '1',
      },
    };
    const req = mockRequest(payload);
    const result = await fetchPerson(req, { id: 'uuid-p-a' });
    expect(result).not.toBeNull();
    expect(result?.name).toBe('Alex');
  });

  it('returns null when person is absent (archived/deleted)', async () => {
    const payload = { person: null };
    const req = mockRequest(payload);
    const result = await fetchPerson(req, { id: 'uuid-archived' });
    expect(result).toBeNull();
  });

  it('passes id correctly to request', async () => {
    const payload = {
      person: {
        id: 'uuid-p-c',
        name: 'Chris',
        biography: null,
        state: 'ACTIVE',
        version: '1',
      },
    };
    const req = mockRequest(payload) as ReturnType<typeof vi.fn>;
    await fetchPerson(req as unknown as GraphqlRequest, { id: 'uuid-p-c' });
    const [, vars] = req.mock.calls[0]!;
    expect((vars as Record<string, unknown>).id).toBe('uuid-p-c');
  });
});

describe('fetchPersonCredits', () => {
  it('extracts credit nodes from response', async () => {
    const payload = {
      personCredits: {
        nodes: [
          {
            id: 'cred-1',
            personId: 'uuid-p-a',
            personName: 'Alex',
            titleId: 'uuid-m-a',
            titleName: 'Light',
            role: {
              id: 'role-1',
              label: 'Actor',
              category: 'CAST',
              allowsCharacters: true,
            },
            characters: ['Hero'],
            version: '1',
          },
        ],
        pageInfo: { endCursor: null, hasNextPage: false },
      },
    };
    const req = mockRequest(payload);
    const result = await fetchPersonCredits(req, { id: 'uuid-p-a' });
    expect(result.nodes).toHaveLength(1);
    expect(result.nodes[0]!.titleName).toBe('Light');
    expect(result.nodes[0]!.role.category).toBe('CAST');
  });

  it('returns empty credits when person has none', async () => {
    const payload = {
      personCredits: {
        nodes: [],
        pageInfo: { endCursor: null, hasNextPage: false },
      },
    };
    const req = mockRequest(payload);
    const result = await fetchPersonCredits(req, { id: 'uuid-no-creds' });
    expect(result.nodes).toHaveLength(0);
  });
});
