import { describe, expect, it } from 'vitest';
import { renderReview } from '../../src/review/review-state.js';

describe('review-state', () => {
  it('renders publishable fields without challenge text', () => {
    const root = document.createElement('div');
    renderReview(root, {
      proposalId: '00000000-0000-4000-8000-000000000001',
      revision: 1,
      contentHash: 'abc',
      values: { title: 'Example' },
      sources: [],
      unknowns: [],
    });
    expect(root.textContent).toContain('Example');
    expect(root.textContent).not.toContain('challenge');
  });

  it('renders source references when provided', () => {
    const root = document.createElement('div');
    renderReview(root, {
      proposalId: '00000000-0000-4000-8000-000000000001',
      revision: 1,
      contentHash: 'abc',
      values: { title: 'Example' },
      sources: [{ field: 'title', url: 'https://example.test/source' }],
      unknowns: [],
    });
    expect(root.textContent).toContain('Source references');
    expect(root.textContent).toContain('https://example.test/source');
  });
});
