import { describe, expect, it } from 'vitest';
import type { GraphqlRequest } from '../../transport/graphql';
import { fetchArchived, fetchDeletionImpact, runMovieLifecycle } from './api';

const operationId = '20000000-0000-4000-8000-000000000001';
const input = {
  operationId,
  id: '30000000-0000-4000-8000-000000000001',
  expectedVersion: '9007199254740993',
  action: 'RESTORE' as const,
  confirmed: true,
};
const request =
  (value: unknown): GraphqlRequest =>
  async () =>
    value;

describe('archive response boundaries', () => {
  it('rejects malformed lists instead of reporting an empty archive', async () => {
    await expect(
      fetchArchived(
        request({ managedTitles: { nodes: null } }),
        'movies',
        'csrf',
      ),
    ).rejects.toThrow();
  });
  it('requires a complete, valid impact preview', async () => {
    await expect(
      fetchDeletionImpact(
        request({
          deletionImpact: {
            token: null,
            affectedCreditCount: -1,
            affectedTitles: [],
          },
        }),
        input.id,
        'csrf',
      ),
    ).rejects.toThrow();
  });
  it('does not accept a mismatched or expired lifecycle receipt', async () => {
    const receipt = {
      id: operationId,
      state: 'SUCCEEDED',
      resultExpired: true,
      problem: null,
    };
    await expect(
      runMovieLifecycle(
        request({ movieLifecycle: { operation: receipt, problem: null } }),
        'csrf',
        input,
      ),
    ).rejects.toThrow();
    receipt.resultExpired = false;
    receipt.id = input.id;
    await expect(
      runMovieLifecycle(
        request({ movieLifecycle: { operation: receipt, problem: null } }),
        'csrf',
        input,
      ),
    ).rejects.toThrow();
  });
  it('preserves the exact version and requires the mutation CSRF boundary', async () => {
    const checked: GraphqlRequest = async (_document, variables, options) => {
      expect(variables.expectedVersion).toBe('9007199254740993');
      expect(options).toMatchObject({ csrf: 'csrf', mutation: true });
      return {
        movieLifecycle: {
          operation: {
            id: operationId,
            state: 'SUCCEEDED',
            resultExpired: false,
            problem: null,
          },
          problem: null,
        },
      };
    };
    await expect(runMovieLifecycle(checked, 'csrf', input)).resolves.toBe(
      'SUCCEEDED',
    );
  });
});

it('preserves the actual archive cursor and rejects nonadvancing pages', async () => {
  const { fetchArchivedPage } = await import('./api');
  const calls: unknown[] = [];
  const read: GraphqlRequest = async (document, variables) => {
    calls.push({ document, variables });
    return { managedTitles: { nodes: [], pageInfo: { hasNextPage: true, endCursor: 'next' } } };
  };
  expect(await fetchArchivedPage(read, 'movies', 'csrf', 'previous')).toMatchObject({ pageInfo: { endCursor: 'next' } });
  expect(calls[0]).toHaveProperty('variables', { after: 'previous' });
  await expect(fetchArchivedPage(read, 'movies', 'csrf', 'next')).rejects.toThrow('cursor');
});
