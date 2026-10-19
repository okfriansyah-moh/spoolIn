import { cleanup, render, screen } from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import TitlePage from './+page.svelte';
import TitleCard from '../../../lib/components/TitleCard.svelte';
import { fetchTitle } from '../../../lib/features/catalog/api';

vi.mock('$app/paths', () => ({
  resolve: (path: string, params: Record<string, string> = {}) =>
    Object.entries(params).reduce(
      (result, [key, value]) =>
        result.replace(`[${key}]`, encodeURIComponent(value)),
      path,
    ),
}));
vi.mock('$app/state', () => ({
  page: { params: { id: '07b0fb38-32f0-5478-92cc-57e1315430cf' } },
}));
vi.mock('../../../lib/features/catalog/api', () => ({
  fetchTitle: vi.fn(),
  fetchTitleCredits: vi.fn(),
}));
vi.mock('../../../lib/transport/graphql', () => ({
  createTransport: () => vi.fn(),
}));
afterEach(() => cleanup());

it.each(['MOVIE', 'TV'] as const)(
  'renders %s poster attribution through the title page',
  async (kind) => {
    vi.mocked(fetchTitle).mockResolvedValue({
      id: '07b0fb38-32f0-5478-92cc-57e1315430cf',
      kind,
      state: 'ACTIVE',
      version: '1',
      title: 'Verified title',
      originalTitle: null,
      synopsis: 'A factual synopsis.',
      synopsisLanguage: 'en',
      originalLanguage: null,
      countries: [],
      release: { year: 2000, month: null, day: null, precision: 'YEAR' },
      tvEnd: { year: null, month: null, day: null, precision: 'UNKNOWN' },
      tvStatus: kind === 'TV' ? 'ENDED' : null,
      releaseContext: kind === 'TV' ? 'FIRST_AIR' : 'ORIGINAL_RELEASE',
      runtimeMinutes: null,
      genres: [],
      artwork: {
        assetId: '8f0ad541-68e7-5fad-bc86-61f234d82870',
        url: '/artwork/8f0ad541-68e7-5fad-bc86-61f234d82870',
        width: 500,
        height: 750,
      },
      credits: { nodes: [], pageInfo: { endCursor: null, hasNextPage: false } },
      provenance: [
        {
          source: 'poster',
          externalId: '8f0ad541-68e7-5fad-bc86-61f234d82870',
          recordUrl:
            'https://www.themoviedb.org/' +
            (kind === 'TV' ? 'tv/1' : 'movie/1'),
          retrievedAt: '2026-10-18T00:00:00Z',
          reuseNote: 'Poster source: TMDB',
          fieldSources: {},
          locallyChanged: [],
        },
      ],
    });
    render(TitlePage);
    const link = await screen.findByRole('link', {
      name: 'Poster source: TMDB',
    });
    expect(link.getAttribute('href')).toContain('https://www.themoviedb.org/');
    expect(screen.queryByRole('button', { name: /edit|upload/i })).toBeNull();
  },
);

it('keeps poster and title links on the same resolved detail route', () => {
  const id = '07b0fb38-32f0-5478-92cc-57e1315430cf';
  render(TitleCard, { props: { id, title: 'Card title', kind: 'Movie' } });
  const links = screen.getAllByRole('link', { name: 'Card title' });
  expect(links.map((link) => link.getAttribute('href'))).toEqual([
    '/titles/' + id,
    '/titles/' + id,
  ]);
});
it('shows an invalid record identity without creating a detail link', () => {
  render(TitleCard, {
    props: { id: 'invalid', title: 'Unlinked title', kind: 'Movie' },
  });
  expect(screen.getByRole('heading', { name: 'Unlinked title' })).toBeTruthy();
  expect(screen.queryByRole('link')).toBeNull();
});
