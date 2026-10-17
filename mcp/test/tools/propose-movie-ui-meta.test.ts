import { describe, expect, it } from 'vitest';
import {
  buildProposeMovieToolMeta,
  modelVisibleProposeMovieContent,
} from '../../src/tools/propose-movie-ui-meta.js';

describe('propose-movie-ui-meta', () => {
  it('keeps challenge in UI-only _meta, not structured content', () => {
    const structured = {
      proposalId: '00000000-0000-4000-8000-000000000001',
      revision: 1,
      contentHash: 'abc',
      challenge: 'secret-challenge',
      status: 'REVIEW',
    };
    const meta = buildProposeMovieToolMeta({
      proposalId: structured.proposalId,
      revision: structured.revision,
      contentHash: structured.contentHash,
      challenge: structured.challenge,
    });
    const modelVisible = modelVisibleProposeMovieContent(structured);
    expect(modelVisible).not.toHaveProperty('challenge');
    expect(meta.ui).toMatchObject({
      challenge: 'secret-challenge',
      resourceUri: 'spoolin://proposal/00000000-0000-4000-8000-000000000001/1',
    });
  });
});
