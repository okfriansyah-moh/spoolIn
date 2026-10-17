import { describe, expect, it } from 'vitest';
import { renderReview } from '../../src/review/review-state.js';

describe('review-state', () => {
  it('renders publishable fields without challenge text', () => {
    const root = document.createElement('div');
    renderReview(root, {
      revision: 1,
      values: { title: 'Example' },
      sources: [],
      unknowns: [],
    });
    expect(root.textContent).toContain('Example');
    expect(root.textContent).not.toContain('challenge');
  });
});
