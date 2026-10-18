import { describe, expect, it } from 'vitest';
import { contextProjection, dateProjection, movieProjection, outcomeProjection, proposalProjection } from '../../src/gateway/projections.js';
import { ProposalRevision } from '../../src/generated/spoolin/v1/spoolin.js';

describe('approved model output projections', () => {
  it('preserves partial-date precision and explicit unknowns', () => {
    expect(dateProjection(undefined)).toEqual({ year: null, month: null, day: null, precision: 'UNKNOWN' });
    expect(dateProjection({ year: 2024 }).precision).toBe('YEAR');
    expect(dateProjection({ year: 2024, month: 2 }).precision).toBe('MONTH');
    expect(dateProjection({ year: 2024, month: 2, day: 29 }).precision).toBe('DAY');
    expect(movieProjection(undefined)).toMatchObject({ originalTitle: null, runtimeMinutes: null, originalLanguage: null });
  });
  it('projects only declared catalog context fields', () => {
    const projected = contextProjection({ titles: { nodes: [{ id: 'movie', kind: 'MOVIE', state: 'ACTIVE', version: '42', provenance: [],
      values: { title: 'A movie', synopsis: 'Private to detail', genreIds: ['genre'], countries: [], synopsisLanguage: 'en' } }], hasNextPage: false },
      genres: { nodes: [{ id: 'genre', label: 'Drama' }] }, roles: { nodes: [] }, policy: { revision: 'policy', textBounds: [] } });
    expect(projected.titles[0]).toEqual({ id: 'movie', title: 'A movie', genreIds: ['genre'], release: dateProjection(undefined) });
    expect(JSON.stringify(projected)).not.toContain('Private to detail');
    expect(contextProjection({})).toEqual({ titles: [], genres: [], roles: [], policyRevision: '' });
  });
  it('keeps gateway challenges and binding tokens out of model-readable proposals', () => {
    const revision = ProposalRevision.fromPartial({ proposalId: 'proposal', revision: 2, contentHash: 'hash',
      uiChallenge: Buffer.from('private-challenge'), uiBindingToken: 'private-token', expiresAt: { seconds: '1000', nanos: 0 } });
    const projected = proposalProjection(revision, 'policy');
    expect(projected).toMatchObject({ status: 'REVIEW', reviewResourceUri: 'spoolin://proposal/proposal/2', expiresAt: '1970-01-01T00:16:40.000Z' });
    expect(JSON.stringify(projected)).not.toMatch(/private-challenge|private-token/);
  });
  it('preserves persisted receipt identity and maps problems without upstream details', () => {
    expect(outcomeProjection({ operationId: 'operation', state: 'SUCCEEDED', resultId: 'movie', version: '9223372036854775807', resultExpired: false }))
      .toMatchObject({ operationId: 'operation', resultId: 'movie', version: '9223372036854775807' });
    expect(outcomeProjection({ operationId: 'operation', state: 'FAILED', resultExpired: false,
      problem: { code: 'CONFLICT', fields: [{ path: 'title', code: 'CONFLICT', message: 'Review the current value' }] } }).problem)
      .toMatchObject({ code: 'CONFLICT', retryAfterMs: null });
  });
});
