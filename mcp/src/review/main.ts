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
  void fetch(
    `/gateway/review_ui_binding?proposalId=${encodeURIComponent(query.proposalId)}&revision=${query.revision}&contentHash=${encodeURIComponent(query.contentHash)}&bindingToken=${encodeURIComponent(query.bindingToken)}`,
  )
    .then(async (response) => {
      if (!response.ok) return;
      const payload = (await response.json()) as { challenge?: string };
      if (typeof payload.challenge === 'string' && payload.challenge.trim()) {
        model = { ...model, challenge: payload.challenge };
      }
    })
    .catch(() => undefined);
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
    if ('bindingToken' in data && typeof data.bindingToken === 'string' && data.bindingToken.trim()) {
      void fetch(
        `/gateway/review_ui_binding?proposalId=${encodeURIComponent(model.proposalId)}&revision=${model.revision}&contentHash=${encodeURIComponent(model.contentHash)}&bindingToken=${encodeURIComponent(data.bindingToken)}`,
      )
        .then(async (response) => {
          if (!response.ok) return;
          const payload = (await response.json()) as { challenge?: string };
          if (typeof payload.challenge === 'string' && payload.challenge.trim()) {
            model = { ...model, challenge: payload.challenge };
            renderReview(root, model);
          }
        })
        .catch(() => undefined);
    }
  });
}
