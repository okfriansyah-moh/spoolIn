// @vitest-environment jsdom
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
  it('shows exact nested publishable values and explicit unknowns as escaped text', () => {
    const root = document.createElement('div');
    renderReview(root, {
      proposalId: '00000000-0000-4000-8000-000000000001',
      revision: 1,
      contentHash: 'ab'.repeat(32),
      values: {
        release: { year: 2024, month: 2, day: null },
        countries: ['ID', 'GB'],
        originalTitle: null,
        synopsis: '<img src=x onerror=alert(1)>',
        genreIds: [],
      },
      sources: [],
      unknowns: [],
    });
    expect(root.textContent).toContain('year: 2024; month: 2; day: Unknown');
    expect(root.textContent).toContain('ID, GB');
    expect(root.textContent).toContain('originalTitleUnknown');
    expect(root.textContent).toContain('genreIdsNone');
    expect(root.textContent).toContain('<img src=x onerror=alert(1)>');
    expect(root.querySelector('img')).toBeNull();
    expect(root.textContent).not.toContain('[object Object]');
  });
});
