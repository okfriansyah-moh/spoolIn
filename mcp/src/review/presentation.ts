export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function renderPoster(root: HTMLElement, proposal: Record<string, unknown>, metadata: Record<string, unknown>, onReady: () => void): void {
  if (!isRecord(proposal.poster)) return;
  const figure = document.createElement('figure');
  const caption = document.createElement('figcaption');
  caption.textContent = 'Poster included in this exact revision';
  figure.append(caption);
  const preview = isRecord(metadata.posterPreview) ? metadata.posterPreview : {};
  if (typeof preview.data === 'string' && ['image/jpeg', 'image/png', 'image/webp'].includes(String(preview.mimeType))) {
    const image = document.createElement('img');
    image.alt = `Proposed poster for ${isRecord(proposal.normalizedMovie) ? String(proposal.normalizedMovie.title) : 'this movie'}`;
    image.addEventListener('load', onReady, { once: true });
    image.addEventListener('error', () => { caption.textContent = 'Poster preview unavailable. Use signed-in browser review before approval.'; });
    image.src = `data:${String(preview.mimeType)};base64,${preview.data}`;
    figure.append(image);
  } else caption.textContent = 'Poster preview unavailable or omitted from this queue. Use signed-in browser review before approval.';
  root.append(figure);
}

export function renderReviewLink(root: HTMLElement, value: unknown): void {
  if (typeof value !== 'string') return;
  try {
    const url = new URL(value);
    if (url.protocol !== 'https:' || url.username || url.password) return;
    const link = document.createElement('a');
    link.href = url.href;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.textContent = 'Open signed-in browser review';
    root.append(link);
  } catch { /* Invalid links are omitted. */ }
}
