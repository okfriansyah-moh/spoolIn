import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  MovieContext,
  MoviePosterImage,
  PreparedPoster,
} from '../../src/generated/spoolin/v1/spoolin.js';
import {
  contextPosters,
  posterProjection,
  publicOrigin,
  reviewUrl,
} from '../../src/tools/poster-output.js';

afterEach(() => vi.unstubAllEnvs());

describe('public poster links', () => {
  it.each([
    '',
    'http://example.test',
    'https://user:password@example.test',
    'https://example.test/path',
    'https://example.test/?private=1',
    'https://example.test/#private',
  ])('rejects invalid public origin %s', (origin) => {
    vi.stubEnv('SPOOLIN_PUBLIC_ORIGIN', origin);
    expect(() => publicOrigin()).toThrow();
  });

  it('builds escaped review and poster URLs without leaking attribution into the path', () => {
    vi.stubEnv('SPOOLIN_PUBLIC_ORIGIN', 'https://spoolin.example/');
    expect(reviewUrl('proposal/1', 2)).toBe(
      'https://spoolin.example/manage/proposals/proposal%2F1?revision=2',
    );
    const poster = PreparedPoster.fromPartial({
      refId: 'poster/1',
      assetId: 'asset',
      mediaType: 'image/png',
      width: 10,
      height: 20,
      state: 'READY',
      sourceUrl: 'https://source.test',
    });
    expect(posterProjection(poster)).toMatchObject({
      url: 'https://spoolin.example/api/movie-posters/poster%2F1',
      sourceUrl: 'https://source.test',
      mediaType: 'image/png',
      width: 10,
      height: 20,
    });
  });
});

describe('context thumbnail bounds', () => {
  it('includes valid thumbnails, keeps unavailable metadata safe and preserves title order', async () => {
    vi.stubEnv('SPOOLIN_PUBLIC_ORIGIN', 'https://spoolin.example');
    const context = MovieContext.fromPartial({
      titles: {
        nodes: [
          { id: 'first', assetId: 'asset-1', values: { title: 'First' } },
          { id: 'no-poster' },
          { id: 'unavailable', assetId: 'asset-2' },
        ],
      },
    });
    const titles = context.titles!.nodes.map((title) => ({
      id: title.id,
      poster: null as Record<string, unknown> | null,
    }));
    const read = vi.fn(async (request: { assetId?: string }) => {
      if (request.assetId === 'asset-2')
        throw new Error('private upstream detail');
      return MoviePosterImage.fromPartial({
        imageBytes: Uint8Array.from([1, 2, 3]),
        poster: { mediaType: 'image/png', width: 10, height: 20 },
      });
    });
    const content = await contextPosters(context, titles, read);
    expect(content).toEqual([
      { type: 'text', text: 'Poster thumbnail for movie first: First' },
      { type: 'image', data: 'AQID', mimeType: 'image/png' },
    ]);
    expect(titles[0]!.poster).toMatchObject({
      preview: 'INCLUDED',
      width: 10,
      height: 20,
    });
    expect(titles[1]!.poster).toBeNull();
    expect(titles[2]!.poster).toMatchObject({
      preview: 'UNAVAILABLE',
      mediaType: null,
    });
    expect(JSON.stringify(content)).not.toContain('private');
  });

  it('omits invalid formats and images exceeding the shared response budget', async () => {
    vi.stubEnv('SPOOLIN_PUBLIC_ORIGIN', 'https://spoolin.example');
    const context = MovieContext.fromPartial({
      titles: {
        nodes: [
          { id: 'invalid', assetId: 'invalid' },
          { id: 'large', assetId: 'large' },
        ],
      },
    });
    const titles = context.titles!.nodes.map((title) => ({
      id: title.id,
      poster: null as Record<string, unknown> | null,
    }));
    const read = async (request: { assetId?: string }) =>
      MoviePosterImage.fromPartial({
        imageBytes: new Uint8Array(
          request.assetId === 'large' ? 4 * 1024 * 1024 : 1,
        ),
        poster: {
          mediaType: request.assetId === 'large' ? 'image/jpeg' : 'text/html',
        },
      });
    expect(await contextPosters(context, titles, read)).toEqual([]);
    expect(titles[0]!.poster?.preview).toBe('UNAVAILABLE');
    expect(titles[1]!.poster?.preview).toBe('OMITTED_BUDGET');
  });

  it('handles an empty catalog without calling the image reader', async () => {
    const read = vi.fn();
    expect(
      await contextPosters(MovieContext.fromPartial({}), [], read),
    ).toEqual([]);
    expect(read).not.toHaveBeenCalled();
  });
});
