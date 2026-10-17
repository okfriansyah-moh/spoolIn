import { describe, expect, it } from 'vitest';
import {
  buildProposeMovieToolMeta,
  modelVisibleProposeMovieContent,
} from '../../src/tools/propose-movie-ui-meta.js';
import { composeProposeMovieFromGatewayPayload } from '../../src/tools/propose-movie-from-gateway.js';
import { composeProposeMovieToolResult } from '../../src/tools/propose-movie-result.js';
import { buildProposeMovieCallToolResult } from '../../src/transport/propose-tool-result.js';

describe('propose-movie-ui-meta', () => {
  it('keeps the challenge in UI-only _meta, not structured content or a binding token', () => {
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
      challenge: structured.challenge,
    });
    const modelVisible = modelVisibleProposeMovieContent(structured);
    expect(modelVisible).not.toHaveProperty('challenge');
    expect(modelVisible).not.toHaveProperty('bindingToken');
    expect(JSON.stringify(modelVisible)).not.toContain('secret-challenge');
    expect(meta.ui).toMatchObject({
      challenge: 'secret-challenge',
      visibility: ['app'],
    });
    expect(meta.ui).not.toHaveProperty('bindingToken');
    expect(meta.ui).not.toHaveProperty('proposalId');
  });

  it('composeProposeMovieToolResult strips gateway secrets from structured content', () => {
    const result = composeProposeMovieToolResult(
      {
        proposalId: '00000000-0000-4000-8000-000000000002',
        revision: 2,
        contentHash: 'def',
        ui_binding_token: 'gateway-token',
        ui_challenge: 'raw-challenge',
      },
      {
        proposalId: '00000000-0000-4000-8000-000000000002',
        revision: 2,
        contentHash: 'def',
        challenge: 'raw-challenge',
      },
    );
    expect(result.structuredContent).not.toHaveProperty('ui_binding_token');
    expect(result.structuredContent).not.toHaveProperty('ui_challenge');
    expect(JSON.stringify(result.structuredContent)).not.toContain('raw-challenge');
    expect(result._meta.ui).toMatchObject({ challenge: 'raw-challenge', visibility: ['app'] });
    expect(JSON.stringify(result)).not.toContain('gateway-token');
  });

  it('composeProposeMovieFromGatewayPayload maps snake_case challenge into UI meta only', () => {
    const result = composeProposeMovieFromGatewayPayload({
      proposal_id: '00000000-0000-4000-8000-000000000003',
      revision: 3,
      content_hash: 'ghi',
      ui_challenge: 'snake-challenge',
      ui_binding_token: 'snake-token',
      status: 'REVIEW',
    });
    expect(result.structuredContent).not.toHaveProperty('ui_binding_token');
    expect(result.structuredContent).not.toHaveProperty('ui_challenge');
    expect(result._meta.ui).toMatchObject({ challenge: 'snake-challenge' });
    expect(JSON.stringify(result.structuredContent)).not.toContain('snake-token');
    expect(JSON.stringify(result._meta)).not.toContain('snake-token');
  });

  it('buildProposeMovieCallToolResult is the transport entrypoint', () => {
    const result = buildProposeMovieCallToolResult({
      proposalId: '00000000-0000-4000-8000-000000000004',
      revision: 1,
      contentHash: 'hash',
      uiChallenge: 'meta-only',
      uiBindingToken: 'must-not-leak',
    });
    expect(result.structuredContent).not.toHaveProperty('uiBindingToken');
    expect(result._meta.ui).toMatchObject({ challenge: 'meta-only' });
    expect(JSON.stringify(result)).not.toContain('must-not-leak');
  });
});
