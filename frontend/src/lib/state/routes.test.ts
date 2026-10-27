import { it, expect } from 'vitest';
import { render } from 'svelte/server';
import { createRawSnippet } from 'svelte';
import Layout from '../../routes/+layout.svelte';
import Home from '../../routes/+page.svelte';
import { ssr, prerender } from '../../routes/+layout';

it('keeps the static client fallback without server rendering or prerendered protected projections', () => {
  expect(ssr).toBe(false);
  expect(prerender).toBe(false);
});
it('composes the production root layout with its public shell and route content', () => {
  const children = createRawSnippet(() => ({
    render: () => '<h1>Route content</h1>',
  }));
  const body = render(Layout, { props: { children } }).body;
  expect(body).toContain('SpoolIn');
  expect(body).toContain('<h1>Route content</h1>');
  expect(body).not.toContain('/manage/archives');
});
it('renders the adopted discovery identity and document title', () => {
  const result = render(Home);
  const visibleText = result.body
    .replace(/<[^>]*>/g, ' ')
    .replace(/&#x2019;/g, '’')
    .replace(/\s+/g, ' ');
  expect(visibleText).toContain('Every reel holds a story.');
  expect(visibleText).toContain(
    'Unspooling stories and discovering what’s behind them.',
  );
  expect(result.head).toContain('SpoolIn · Every reel holds a story.');
});
