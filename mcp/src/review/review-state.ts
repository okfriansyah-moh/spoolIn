export type ReviewModel = Readonly<{
  proposalId: string;
  revision: number;
  contentHash: string;
  values: Record<string, unknown>;
  sources: readonly unknown[];
  unknowns: readonly string[];
  challenge?: string;
}>;

export type ReviewQuery = Readonly<{
  proposalId: string;
  revision: number;
  contentHash: string;
}>;

function escapeText(value: string): Text {
  return document.createTextNode(value);
}

export function parseReviewQuery(search: string): ReviewQuery | null {
  const params = new URLSearchParams(search);
  const proposalId = params.get('proposalId') ?? '';
  const revision = Number(params.get('revision') ?? '0');
  const contentHash = params.get('contentHash') ?? '';
  if (!proposalId || !Number.isFinite(revision) || revision < 1 || !contentHash) return null;
  return Object.freeze({ proposalId, revision, contentHash });
}

export function renderReview(root: HTMLElement, model: ReviewModel): void {
  root.replaceChildren();
  const heading = document.createElement('h1');
  heading.textContent = `Review proposal revision ${model.revision}`;
  root.append(heading);
  const list = document.createElement('dl');
  for (const [key, value] of Object.entries(model.values)) {
    const term = document.createElement('dt');
    term.append(escapeText(key));
    const detail = document.createElement('dd');
    detail.append(escapeText(String(value ?? '')));
    list.append(term, detail);
  }
  root.append(list);
  if (model.unknowns.length > 0) {
    const unknownHeading = document.createElement('h2');
    unknownHeading.textContent = 'Explicit unknowns';
    root.append(unknownHeading);
    const unknownList = document.createElement('ul');
    for (const item of model.unknowns) {
      const li = document.createElement('li');
      li.append(escapeText(item));
      unknownList.append(li);
    }
    root.append(unknownList);
  }
  const actions = document.createElement('div');
  actions.className = 'review-actions';
  const approve = document.createElement('button');
  approve.type = 'button';
  approve.textContent = 'Approve exact revision';
  approve.dataset.action = 'approve';
  const reject = document.createElement('button');
  reject.type = 'button';
  reject.textContent = 'Reject';
  reject.dataset.action = 'reject';
  actions.append(approve, reject);
  root.append(actions);
}

export function wireTrustedApproval(
  root: HTMLElement,
  model: ReviewModel,
  submit: (action: 'approve' | 'reject', binding: ReviewModel) => Promise<void>,
): void {
  root.addEventListener('click', (event) => {
    const target = event.target;
    if (!(target instanceof HTMLButtonElement)) return;
    const action = target.dataset.action;
    if (action !== 'approve' && action !== 'reject') return;
    if (!model.challenge?.trim()) {
      globalThis.alert('Approval challenge unavailable in this review context.');
      return;
    }
    void submit(action, model);
  });
}
