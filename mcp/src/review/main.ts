import { buildApprovalBody, assertManualApproval } from './approval-binding.js';
import { parseReviewQuery, renderReview, wireTrustedApproval } from './review-state.js';

const root = document.querySelector('#app');
const query = parseReviewQuery(globalThis.location.search);
if (root && query) {
  let model = {
    proposalId: query.proposalId,
    revision: query.revision,
    contentHash: query.contentHash,
    values: {} as Record<string, unknown>,
    sources: [] as readonly unknown[],
    unknowns: [] as readonly string[],
    challenge: '',
  };
  renderReview(root, model);
  wireTrustedApproval(root, model, async (action, binding) => {
    assertManualApproval(binding.challenge ?? '');
    const body = buildApprovalBody({
      proposalId: binding.proposalId,
      revision: binding.revision,
      contentHash: binding.contentHash,
      challenge: binding.challenge ?? '',
    });
    const path = action === 'approve' ? '/gateway/approve_movie' : '/gateway/reject_movie';
    await fetch(path, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });
  });
  globalThis.addEventListener('message', (event) => {
    const data = event.data;
    if (typeof data !== 'object' || data === null) return;
    if ('values' in data && typeof data.values === 'object' && data.values) {
      model = { ...model, values: data.values as Record<string, unknown> };
      renderReview(root, model);
    }
    if ('challenge' in data && typeof data.challenge === 'string') {
      model = { ...model, challenge: data.challenge };
    }
  });
}
