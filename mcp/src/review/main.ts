import { App } from '@modelcontextprotocol/ext-apps';
import { renderReview, wireTrustedApproval } from './review-state.js';
import type { ReviewModel } from './review-state.js';
import { renderPoster, renderReviewLink } from './presentation.js';

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

const root = document.querySelector<HTMLElement>('#app');
const app = new App({ name: 'SpoolIn review', version: '1.0.0' }, {});
let generation = 0;

app.ontoolresult = (result) => {
  if (!root || !isRecord(result.structuredContent)) return;
  const data = result.structuredContent;
  const proposals = Array.isArray(data.proposals) ? data.proposals : [data];
  if (!Array.isArray(data.proposals) && typeof data.proposalId !== 'string') return;
  const selectedGeneration = ++generation;
  root.replaceChildren();
  const heading = document.createElement('h1');
  heading.textContent = proposals.length === 1 ? 'Review movie proposal' : `Review ${proposals.length} movie proposals`;
  root.append(heading);
  const intro = document.createElement('p');
  intro.textContent = proposals.length ? 'Review each poster, movie details and credits. Approve or reject each movie separately.' : 'No proposals are awaiting your review.';
  root.append(intro);
  const grid = document.createElement('div');
  grid.className = 'review-queue';
  root.append(grid);
  const ui = isRecord(result._meta) && isRecord(result._meta.ui) ? result._meta.ui : {};
  const items = isRecord(ui.items) ? ui.items : {};
  for (const proposal of proposals) {
    if (!isRecord(proposal) || typeof proposal.proposalId !== 'string') continue;
    const metadata = Array.isArray(data.proposals) ? items[proposal.proposalId] : ui;
    renderCard(grid, proposal, isRecord(metadata) ? metadata : {}, selectedGeneration);
  }
};

function reviewModel(proposal: Record<string, unknown>, metadata: Record<string, unknown>): ReviewModel | null {
  if (typeof proposal.proposalId !== 'string' || typeof proposal.contentHash !== 'string' ||
      typeof proposal.revision !== 'number' || !isRecord(proposal.normalizedMovie)) return null;
  return {
    proposalId: proposal.proposalId, revision: proposal.revision, contentHash: proposal.contentHash,
    values: proposal.normalizedMovie,
    credits: Array.isArray(proposal.credits) ? proposal.credits : [],
    sources: Array.isArray(proposal.sources) ? proposal.sources : [], unknowns: [],
    ...(typeof metadata.challenge === 'string' ? { challenge: metadata.challenge } : {}),
  };
}

function renderCard(grid: HTMLElement, proposal: Record<string, unknown>, metadata: Record<string, unknown>, selectedGeneration: number): void {
  const model = reviewModel(proposal, metadata);
  if (!model) return;
  const article = document.createElement('article');
  article.className = 'review-card';
  const heading = document.createElement('h2');
  heading.textContent = String(model.values.title ?? 'Movie');
  article.append(heading);
  const revision = document.createElement('p');
  revision.className = 'revision';
  revision.textContent = `Revision ${model.revision} · ${model.credits?.length ?? 0} credits`;
  article.append(revision);
  const details = document.createElement('details');
  const summary = document.createElement('summary');
  summary.textContent = 'Movie details, credits and sources';
  const content = document.createElement('div');
  renderReview(content, model);
  const actions = content.querySelector<HTMLElement>('.review-actions')!;
  actions.remove();
  details.append(summary, content);
  let busy = false;
  let decided = false;
  let posterReady = !isRecord(proposal.poster);
  const updateButtons = () => {
    for (const button of actions.querySelectorAll('button')) {
      button.disabled = busy || decided || !model.challenge || (button.dataset.action === 'approve' && !posterReady);
    }
  };
  renderPoster(article, proposal, metadata, () => {
    posterReady = true;
    updateButtons();
  });
  article.append(details);
  renderReviewLink(article, proposal.reviewUrl);
  article.append(actions);
  const status = document.createElement('p');
  status.setAttribute('role', 'status');
  status.textContent = model.challenge ? 'Awaiting your decision.' : 'Private review controls unavailable. Open signed-in browser review.';
  article.append(status);
  grid.append(article);
  updateButtons();
  wireTrustedApproval(article, () => model, async (action) => {
    if (busy || decided || !model.challenge || (action === 'approve' && !posterReady)) return;
    busy = true;
    updateButtons();
    status.textContent = 'Submitting your decision…';
    try {
      const reply = await app.callServerTool({ name: 'review_movie_proposal', arguments: {
        proposalId: model.proposalId, revision: model.revision, contentHash: model.contentHash,
        challenge: model.challenge, action,
      } });
      if (reply.isError) throw reviewFailure(reply.structuredContent);
      decided = true;
      const initial = isRecord(reply.structuredContent) ? reply.structuredContent : {};
      await showCompletion(initial, action, status, selectedGeneration);
    } catch (error) {
      status.textContent = error instanceof Error ? error.message : 'Review unavailable. Use signed-in browser review.';
    } finally {
      busy = false;
      updateButtons();
    }
  });
}

function reviewFailure(payload: unknown): Error {
  const problem = isRecord(payload) && isRecord(payload.problem) ? payload.problem : {};
  const messages: Record<string, string> = {
    STALE_VERSION: 'This revision or its review controls changed. Reopen the review queue.',
    NOT_FOUND: 'This proposal expired or was already decided. Open browser review for its current state.',
    UNAUTHENTICATED: 'Reconnect SpoolIn to sign in again.',
    FORBIDDEN: 'Use the owning Maintainer account in browser review.',
    POLICY_CHANGED: 'The movie policy changed. Prepare a fresh proposal.',
  };
  return new Error(messages[String(problem.code)] ?? 'The decision could not be submitted. Use signed-in browser review.');
}

async function showCompletion(initial: Record<string, unknown>, action: string, status: HTMLElement, selectedGeneration: number): Promise<void> {
  let receipt = initial;
  const operationId = receipt.operationId;
  for (let attempt = 0; receipt.state === 'PENDING' && typeof operationId === 'string' && attempt < 15; attempt += 1) {
    status.textContent = 'Decision saved. Finishing movie and poster storage…';
    await new Promise((resolve) => setTimeout(resolve, 2000));
    if (generation !== selectedGeneration) return;
    const reply = await app.callServerTool({ name: 'movie_operation', arguments: { operationId } });
    if (reply.isError || !isRecord(reply.structuredContent)) break;
    receipt = reply.structuredContent;
  }
  if (generation !== selectedGeneration) return;
  if (receipt.state === 'SUCCEEDED' && !receipt.problem && receipt.resultExpired !== true) {
    status.textContent = action === 'approve' ? 'Approved. Movie, credits and included poster saved.' : 'Proposal rejected.';
  } else if (receipt.state === 'PENDING') {
    status.textContent = 'Decision saved; storage is still processing. Check browser review for completion.';
  } else status.textContent = `Decision was not completed (${String(isRecord(receipt.problem) ? receipt.problem.code : receipt.state)}). Open browser review.`;
}

app.ontoolcancelled = () => { generation += 1; if (root) root.textContent = 'Proposal review cancelled.'; };
app.onteardown = async () => { generation += 1; return {}; };
void app.connect().catch(() => { if (root) root.textContent = 'The review host connection is unavailable.'; });
