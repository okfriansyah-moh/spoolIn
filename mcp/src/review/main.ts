import { App } from '@modelcontextprotocol/ext-apps';
import { renderReview } from './review-state.js';
import type { ReviewModel } from './review-state.js';

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

const root = document.querySelector<HTMLElement>('#app');
const app = new App({ name: 'SpoolIn review', version: '1.0.0' }, {});
app.ontoolresult = (result) => {
  if (!root || !isRecord(result.structuredContent)) return;
  const proposal = result.structuredContent;
  if (
    typeof proposal.proposalId !== 'string' ||
    typeof proposal.contentHash !== 'string' ||
    typeof proposal.revision !== 'number'
  )
    return;
  const values = proposal.normalizedMovie;
  if (!isRecord(values)) return;
  const model: ReviewModel = {
    proposalId: proposal.proposalId,
    revision: proposal.revision,
    contentHash: proposal.contentHash,
    values,
    sources: Array.isArray(proposal.sources) ? proposal.sources : [],
    unknowns: Array.isArray(proposal.unknowns)
      ? proposal.unknowns.filter(
          (value): value is string => typeof value === 'string',
        )
      : [],
  };
  renderReview(root, model);
  if (typeof proposal.reviewUrl === 'string') {
    try {
      const url = new URL(proposal.reviewUrl);
      if (url.protocol === 'https:' && !url.username && !url.password) {
        const link = document.createElement('a');
        link.href = url.href;
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
        link.textContent = 'Open signed-in browser review';
        root.append(link);
      }
    } catch { /* Invalid review links are not rendered. */ }
  }
  // Display capability is separate from authenticated host approval enforcement (G-03).
  // Neither tool metadata nor a parent-window message establishes trusted human intent.
  for (const button of root.querySelectorAll('button')) button.disabled = true;
  const status = document.createElement('p');
  status.setAttribute('role', 'status');
  status.textContent =
    'Open the browser review link to approve or reject this exact revision with your Maintainer account.';
  root.append(status);
};
app.ontoolcancelled = () => {
  if (root) root.textContent = 'Proposal review cancelled.';
};
app.onteardown = async () => ({});
void app.connect().catch(() => {
  if (root) root.textContent = 'The review host connection is unavailable.';
});
