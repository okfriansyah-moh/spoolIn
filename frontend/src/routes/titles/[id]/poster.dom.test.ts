import { cleanup, render, screen } from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import TitlePage from './+page.svelte';
import { fetchTitle } from '../../../lib/features/catalog/api';

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
