import { timingSafeEqual } from 'node:crypto';
import { fromJsonSchema } from '@modelcontextprotocol/server';
import type { McpServer } from '@modelcontextprotocol/server';
import { ApprovalInput } from '../generated/spoolin/v1/spoolin.js';
import { GatewayFailure } from '../operations/gateway-failure.js';
import { outcomeProjection } from './projections.js';
import type { SessionState } from './session.js';

function equalChallenge(expected: string, actual: unknown): boolean {
  if (!expected || typeof actual !== 'string') return false;
  const left = Buffer.from(expected);
  const right = Buffer.from(actual);
  return left.length === right.length && timingSafeEqual(left, right);
}

/** Only the rendered app receives the challenge; identity comes from authenticated Core. */
export function registerReviewAction(server: McpServer, state: SessionState): void {
  const tool = server.registerTool('review_movie_proposal', {
    description: 'Submit an explicit human approval or rejection from the proposal review app.',
    _meta: { ui: { visibility: ['app'] } },
    inputSchema: fromJsonSchema<Record<string, unknown>>(state.schemas.review_movie_proposal!),
  }, async (args) => {
    try {
      if (!state.signer) throw new GatewayFailure('FORBIDDEN');
      const uri = `spoolin://proposal/${String(args.proposalId)}/${Number(args.revision)}`;
      const entry = state.proposals.get(uri);
      if (!entry || entry.expires <= Date.now() || entry.content.contentHash !== args.contentHash ||
          !equalChallenge(entry.challenge, args.challenge)) throw new GatewayFailure('FORBIDDEN');
      const identity = await state.gateway.reviewIdentity();
      if (identity.clientKind !== 'CHATGPT' && identity.clientKind !== 'CLAUDE') throw new GatewayFailure('FORBIDDEN');
      const method = args.action === 'approve' ? 'ApproveMovie' : 'RejectMovie';
      const hash = Buffer.from(String(args.contentHash), 'hex').toString('base64');
      const assertion = state.signer.sign({ connector: identity.credentialId, client: identity.clientKind,
        session: state.sessionId, proposal: String(args.proposalId), revision: Number(args.revision),
        hash, method: `/spoolin.v1.McpGatewayService/${method}` });
      const input = ApprovalInput.fromJSON({ proposalId: args.proposalId, revision: args.revision,
        contentHash: hash, challenge: entry.challenge });
      const outcome = await (args.action === 'approve' ? state.gateway.approve(input, assertion) : state.gateway.reject(input, assertion));
      const projection = outcomeProjection(outcome);
      return { content: [{ type: 'text' as const, text: JSON.stringify(projection) }], structuredContent: projection };
    } catch (failure) {
      const problem = failure instanceof GatewayFailure ? failure.problem : new GatewayFailure('UNAVAILABLE').problem;
      return { isError: true, content: [{ type: 'text' as const, text: problem.message }], structuredContent: { problem } };
    }
  });
  state.tools.set('review_movie_proposal', tool);
}
