import type { MovieContext, MoviePosterReadRequest, MoviePosterImage, PreparedPoster } from '../generated/spoolin/v1/spoolin.js';
import { MoviePosterReadRequest as PosterRead } from '../generated/spoolin/v1/spoolin.js';
import { GatewayFailure } from '../operations/gateway-failure.js';

export type MovieContent = { type: 'text'; text: string } | { type: 'image'; data: string; mimeType: string };
export function publicOrigin(): string {
  const configured = process.env.SPOOLIN_PUBLIC_ORIGIN;
  if (!configured) throw new GatewayFailure('UNAVAILABLE');
  const url = new URL(configured);
  if (url.protocol !== 'https:' || url.username || url.password || url.pathname !== '/' || url.search || url.hash)
    throw new GatewayFailure('UNAVAILABLE');
  return url.origin;
}
export function reviewUrl(id: string, revision: number): string {
  return `${publicOrigin()}/manage/proposals/${encodeURIComponent(id)}?revision=${revision}`;
}
export function posterProjection(poster: PreparedPoster) {
  return {
    posterRef: poster.refId, assetId: poster.assetId, contentHash: poster.contentHash,
    mediaType: poster.mediaType, width: poster.width, height: poster.height,
    sourceUrl: poster.sourceUrl, reuseBasis: poster.reuseBasis, state: poster.state,
    url: `${publicOrigin()}/api/movie-posters/${encodeURIComponent(poster.refId)}`,
  };
}
export async function contextPosters(
  context: MovieContext,
  titles: { id: string; poster: Record<string, unknown> | null }[],
  read: (request: MoviePosterReadRequest) => Promise<MoviePosterImage>,
): Promise<MovieContent[]> {
  const content: MovieContent[] = [];
  let budget = 4 * 1024 * 1024;
  const nodes = context.titles?.nodes ?? [];
  for (let offset = 0; offset < nodes.length; offset += 4) {
    const batch = nodes.slice(offset, offset + 4);
    const images = await Promise.allSettled(batch.map((title) =>
      title.assetId && budget > 0 ? read(PosterRead.fromJSON({ assetId: title.assetId })) : Promise.resolve(null),
    ));
    for (const [index, title] of batch.entries()) {
      if (!title.assetId) continue;
      const target = titles[offset + index]!;
      const url = `${publicOrigin()}/artwork/${encodeURIComponent(title.assetId)}`;
      target.poster = { assetId: title.assetId, url, mediaType: null, width: null, height: null, preview: budget > 0 ? 'UNAVAILABLE' : 'OMITTED_BUDGET' };
      const settled = images[index]!;
      if (settled.status !== 'fulfilled' || !settled.value) continue;
      const image = settled.value;
      const metadata = image.poster;
      if (!metadata || !image.imageBytes.length || !['image/jpeg', 'image/png', 'image/webp'].includes(metadata.mediaType)) continue;
      const encoded = Buffer.from(image.imageBytes).toString('base64');
      target.poster = { assetId: title.assetId, url, mediaType: metadata.mediaType, width: metadata.width, height: metadata.height, preview: 'INCLUDED' };
      if (encoded.length + 256 > budget) { target.poster.preview = 'OMITTED_BUDGET'; continue; }
      budget -= encoded.length + 256;
      content.push({ type: 'text', text: `Poster thumbnail for movie ${title.id}: ${title.values?.title ?? ''}` });
      content.push({ type: 'image', data: encoded, mimeType: metadata.mediaType });
    }
  }
  return content;
}
