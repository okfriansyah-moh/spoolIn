import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';

/** Self-contained Apps resource, with hashes for the exact bundled code and style. */
export function reviewHtml(): string {
  const script = readFileSync(
    new URL('../review/review.js', import.meta.url),
    'utf8',
  ).replaceAll('</script', '<\\/script');
  const style = readFileSync(
    new URL('../review/review.css', import.meta.url),
    'utf8',
  );
  const hash = (value: string) =>
    createHash('sha256').update(value).digest('base64');
  const policy = `default-src 'none'; img-src data:; script-src 'sha256-${hash(script)}'; style-src 'sha256-${hash(style)}'; base-uri 'none'; form-action 'none'`;
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="${policy}"><title>Review movie proposal</title><style>${style}</style></head><body><main id="app" aria-live="polite"><p>Waiting for the proposal from your chat host.</p></main><script>${script}</script></body></html>`;
}
