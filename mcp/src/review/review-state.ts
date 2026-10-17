export type ReviewModel = Readonly<{
  revision: number;
  values: Record<string, unknown>;
  sources: readonly unknown[];
  unknowns: readonly string[];
}>;

function escapeText(value: string): Text {
  return document.createTextNode(value);
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
}
