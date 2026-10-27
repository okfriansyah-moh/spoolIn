// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest';
import {
  isRecord,
  renderPoster,
  renderReviewLink,
} from '../../src/review/presentation.js';

describe('review presentation boundaries', () => {
  it.each([null, undefined, [], 'text', 1])(
    'does not treat %j as a record',
    (value) => {
      expect(isRecord(value)).toBe(false);
    },
  );
  it.each([
    'javascript:alert(1)',
    'http://example.test',
    'https://user:password@example.test',
    'not a URL',
    null,
  ])('omits unsafe browser review link %s', (value) => {
    const root = document.createElement('div');
    renderReviewLink(root, value);
    expect(root.querySelector('a')).toBeNull();
  });
  it('does not create a poster or mark it ready without a valid preview', () => {
    const root = document.createElement('div');
    const ready = vi.fn();
    renderPoster(root, {}, {}, ready);
    expect(root.children).toHaveLength(0);
    renderPoster(
      root,
      { poster: {} },
      { posterPreview: { data: 'payload', mimeType: 'text/html' } },
      ready,
    );
    expect(root.querySelector('img')).toBeNull();
    expect(root.textContent).toContain('Poster preview unavailable');
    expect(ready).not.toHaveBeenCalled();
  });
});
