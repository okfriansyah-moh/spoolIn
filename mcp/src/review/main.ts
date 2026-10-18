import { App } from '@modelcontextprotocol/ext-apps';
import { renderReview } from './review-state.js';
import type { ReviewModel } from './review-state.js';

const root = document.querySelector<HTMLElement>('#app');
const app = new App({ name: 'SpoolIn review', version: '1.0.0' }, {});
app.ontoolresult = (result) => {
  if (!root || !result.structuredContent) return;
  const proposal = result.structuredContent;
  if (typeof proposal.proposalId !== 'string' || typeof proposal.contentHash !== 'string' || typeof proposal.revision !== 'number') return;
  const values = proposal.normalizedMovie;
  if (!values || typeof values !== 'object' || Array.isArray(values)) return;
  const model: ReviewModel = {
    proposalId: proposal.proposalId, revision: proposal.revision, contentHash: proposal.contentHash,
    values: values as Record<string, unknown>, sources: Array.isArray(proposal.sources) ? proposal.sources : [],
    unknowns: Array.isArray(proposal.unknowns) ? proposal.unknowns.filter((value): value is string => typeof value === 'string') : [],
  };
  renderReview(root, model);
  // Display capability is separate from authenticated host approval enforcement (G-03).
  // Neither tool metadata nor a parent-window message establishes trusted human intent.
  for (const button of root.querySelectorAll('button')) button.disabled = true;
  const status = document.createElement('p');
  status.setAttribute('role', 'status');
  status.textContent = 'Approval is unavailable until this chat host has verified private review controls.';
  root.append(status);
};
app.ontoolcancelled = () => { if (root) root.textContent = 'Proposal review cancelled.'; };
app.onteardown = async () => ({});
void app.connect().catch(() => { if (root) root.textContent = 'The review host connection is unavailable.'; });
