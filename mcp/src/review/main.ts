import { App } from '@modelcontextprotocol/ext-apps';
import { renderReview, wireTrustedApproval } from './review-state.js';
import type { ReviewModel } from './review-state.js';

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

const root = document.querySelector<HTMLElement>('#app');
let current: ReviewModel | undefined;
let submitting = false;
let posterReady = true;
let stopped = false;
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
    ...(isRecord(result._meta) && isRecord(result._meta.ui) && typeof result._meta.ui.challenge === 'string'
      ? { challenge: result._meta.ui.challenge } : {}),
    sources: Array.isArray(proposal.sources) ? proposal.sources : [],
    unknowns: Array.isArray(proposal.unknowns)
      ? proposal.unknowns.filter(
          (value): value is string => typeof value === 'string',
        )
      : [],
  };
  current = model;
  renderReview(root, model);
  const poster = isRecord(proposal.poster) ? proposal.poster : undefined;
  const ui = isRecord(result._meta) && isRecord(result._meta.ui) ? result._meta.ui : undefined;
  const preview = ui && isRecord(ui.posterPreview) ? ui.posterPreview : undefined;
  posterReady = !poster;
  if (poster) {
    const figure = document.createElement('figure');
    const caption = document.createElement('figcaption');
    caption.textContent = 'Poster included in this exact revision';
    figure.append(caption);
    if (preview && typeof preview.data === 'string' &&
        ['image/jpeg', 'image/png', 'image/webp'].includes(String(preview.mimeType))) {
      const image = document.createElement('img');
      image.alt = `Proposed poster for ${String(values.title ?? 'this movie')}`;
      image.src = `data:${String(preview.mimeType)};base64,${preview.data}`;
      image.addEventListener('load', () => {
        if (current !== model) return;
        posterReady = true;
        const approve = root.querySelector<HTMLButtonElement>('[data-action="approve"]');
        if (approve) approve.disabled = !model.challenge || submitting;
      });
      image.addEventListener('error', () => { caption.textContent = 'Poster preview unavailable. Inspect it in signed-in browser review before approval.'; });
      figure.append(image);
    } else caption.textContent = 'Poster preview unavailable. Inspect it in signed-in browser review before approval.';
    root.append(figure);
  }
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
  for (const button of root.querySelectorAll('button')) button.disabled = !model.challenge || (button.dataset.action === 'approve' && !posterReady);
  const status = document.createElement('p');
  status.setAttribute('role', 'status');
  status.textContent = model.challenge
    ? 'Review this exact revision, then approve or reject below.'
    : 'This host did not provide private review metadata. Use the signed-in browser review link.';
  root.append(status);
};
if (root) wireTrustedApproval(root, () => current!, async (action, model) => {
  if (submitting || !model.challenge) return;
  submitting = true;
  for (const button of root.querySelectorAll('button')) button.disabled = true;
  const status = root.querySelector('[role="status"]');
  if (status) status.textContent = 'Submitting your decision…';
  try {
    const reply = await app.callServerTool({ name: 'review_movie_proposal', arguments: {
      proposalId: model.proposalId, revision: model.revision, contentHash: model.contentHash,
      challenge: model.challenge, action,
    } });
    if (reply.isError) {
      const payload = isRecord(reply.structuredContent) ? reply.structuredContent : {};
      const problem = isRecord(payload.problem) ? payload.problem : {};
      const code = String(problem.code ?? 'UNAVAILABLE');
      const messages: Record<string, string> = {
        STALE_VERSION: 'This review changed or was reopened in the browser. Open browser review for the current decision controls.',
        NOT_FOUND: 'This proposal expired or was already decided. Open browser review to see its current state.',
        UNAUTHENTICATED: 'Reconnect SpoolIn to sign in again, or use signed-in browser review.',
        FORBIDDEN: 'This connection cannot decide this proposal. Use its owning Maintainer account in browser review.',
        POLICY_CHANGED: 'The movie policy changed. Prepare a fresh proposal before approval.',
      };
      throw new Error(messages[code] ?? 'The decision could not be submitted. Retry when SpoolIn is available, or use browser review.');
    }
    const initialReceipt = isRecord(reply.structuredContent) ? reply.structuredContent : {};
    const receipt = await waitForCompletion(initialReceipt, model, status);
    const state = String(receipt.state ?? 'UNKNOWN');
    const problem = isRecord(receipt.problem) ? receipt.problem : undefined;
    if (problem || receipt.resultExpired === true || state === 'FAILED' || state === 'EXPIRED') {
      throw new Error(`Decision was not completed (${String(problem?.code ?? state)}). Use browser review for the current proposal.`);
    }
    if (state === 'SUCCEEDED') {
      if (status) status.textContent = action === 'approve' ? 'Approved. Movie and included poster saved.' : 'Proposal rejected.';
    } else if (state === 'PENDING') {
      if (status) status.textContent = 'Your decision is still processing. Check the operation or browser review for completion.';
    } else throw new Error(`Unexpected operation status (${state}). Check browser review before trying again.`);
  } catch (error) {
    if (status) status.textContent = error instanceof Error ? error.message : 'Review unavailable. Use browser review.';
    for (const button of root.querySelectorAll('button')) button.disabled = button.dataset.action === 'approve' && !posterReady;
  } finally { submitting = false; }
});
app.ontoolcancelled = () => {
  stopped = true;
  if (root) root.textContent = 'Proposal review cancelled.';
};
app.onteardown = async () => { stopped = true; return {}; };
void app.connect().catch(() => {
  if (root) root.textContent = 'The review host connection is unavailable.';
});

async function waitForCompletion(
  initial: Record<string, unknown>, model: ReviewModel, status: Element | null,
): Promise<Record<string, unknown>> {
  let receipt = initial;
  if (typeof receipt.operationId !== 'string') return receipt;
  const operationId = receipt.operationId;
  for (let attempt = 0; receipt.state === 'PENDING' && attempt < 15; attempt += 1) {
    if (status) status.textContent = 'Your decision was saved. Finishing movie and poster storage…';
    await new Promise((resolve) => setTimeout(resolve, 2000));
    if (stopped || current !== model) return receipt;
    const reply = await app.callServerTool({ name: 'movie_operation', arguments: { operationId } });
    if (reply.isError || !isRecord(reply.structuredContent)) return receipt;
    receipt = reply.structuredContent;
  }
  return receipt;
}
