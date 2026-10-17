import { describe, expect, it } from 'vitest';
import {
  buildProposeMovieToolMeta,
  modelVisibleProposeMovieContent,
} from '../../src/tools/propose-movie-ui-meta.js';
import { composeProposeMovieToolResult } from '../../src/tools/propose-movie-result.js';

describe('propose-movie-ui-meta', () => {
  it('keeps binding token in UI-only _meta, not structured content', () => {
    const structured = {
      proposalId: '00000000-0000-4000-8000-000000000001',
      revision: 1,
      contentHash: 'abc',
      challenge: 'secret-challenge',
      bindingToken: 'secret-binding-token',
      status: 'REVIEW',
    };
    const meta = buildProposeMovieToolMeta({
      proposalId: structured.proposalId,
      revision: structured.revision,
      contentHash: structured.contentHash,
      bindingToken: structured.bindingToken,
    });
    const modelVisible = modelVisibleProposeMovieContent(structured);
    expect(modelVisible).not.toHaveProperty('challenge');
    expect(modelVisible).not.toHaveProperty('bindingToken');
    expect(meta.ui).toMatchObject({
      bindingToken: 'secret-binding-token',
      resourceUri: 'spoolin://proposal/00000000-0000-4000-8000-000000000001/1',
    });
  });

  it('composeProposeMovieToolResult strips gateway secrets from structured content', () => {
    const result = composeProposeMovieToolResult(
      {
        proposalId: '00000000-0000-4000-8000-000000000002',
        revision: 2,
        contentHash: 'def',
        ui_binding_token: 'gateway-token',
      },
      {
        proposalId: '00000000-0000-4000-8000-000000000002',
        revision: 2,
        contentHash: 'def',
        bindingToken: 'gateway-token',
      },
    );
    expect(result.structuredContent).not.toHaveProperty('ui_binding_token');
    expect(result._meta.ui).toMatchObject({ bindingToken: 'gateway-token' });
  });
});
