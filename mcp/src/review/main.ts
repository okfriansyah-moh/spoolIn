import { buildApprovalBody, assertManualApproval } from './approval-binding.js';
import { isTrustedReviewOrigin, parseReviewQuery, renderReview, wireTrustedApproval } from './review-state.js';

const root = document.querySelector('#app');
const query = parseReviewQuery(globalThis.location.search);
if (root && query) {
  let connectorBearer = '';
  let model = {
    proposalId: query.proposalId,
    revision: query.revision,
    contentHash: query.contentHash,
    values: {} as Record<string, unknown>,
    sources: [] as readonly unknown[],
    unknowns: [] as readonly string[],
    challenge: '',
  };
  const approvalHeaders = (): HeadersInit => {
    const headers: Record<string, string> = { 'content-type': 'application/json' };
    if (connectorBearer.trim()) {
      headers.authorization = `Bearer ${connectorBearer.trim()}`;
    }
    return headers;
  };
  renderReview(root, model);
  wireTrustedApproval(root, () => model, async (action, binding) => {
    assertManualApproval(binding.challenge ?? '');
    const body = buildApprovalBody({
      proposalId: binding.proposalId,
      revision: binding.revision,
      contentHash: binding.contentHash,
      challenge: binding.challenge ?? '',
    });
    const path = action === 'approve' ? '/gateway/approve_movie' : '/gateway/reject_movie';
    await fetch(path, { method: 'POST', headers: approvalHeaders(), body: JSON.stringify(body) });
  });
  globalThis.addEventListener('message', (event) => {
    if (!isTrustedReviewOrigin(event.origin)) return;
    const data = event.data;
    if (typeof data !== 'object' || data === null) return;
    if ('values' in data && typeof data.values === 'object' && data.values) {
      model = { ...model, values: data.values as Record<string, unknown> };
      renderReview(root, model);
    }
    if ('sources' in data && Array.isArray(data.sources)) {
      model = { ...model, sources: data.sources as readonly unknown[] };
      renderReview(root, model);
    }
    if ('connectorToken' in data && typeof data.connectorToken === 'string' && data.connectorToken.trim()) {
      connectorBearer = data.connectorToken;
    }
    if ('challenge' in data && typeof data.challenge === 'string' && data.challenge.trim()) {
      model = { ...model, challenge: data.challenge };
      renderReview(root, model);
    }
  });
}
