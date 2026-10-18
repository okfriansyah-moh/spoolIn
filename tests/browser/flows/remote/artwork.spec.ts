import { resolve } from 'node:path';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { test, expect, signInMaintainer, createMovie, capture, readApi } from './helpers';

test('Artwork production upload confirms attachment and serves actual bytes after reload', async ({ page }, info) => {
  await signInMaintainer(page);
  const movie = await createMovie(page);
  await page.getByLabel('Poster artwork').setInputFiles(resolve('../../docs/design-system/mockup/assets/letters.png'));
  await expect(page.locator('.artwork-upload [role="status"]')).toHaveText('Saved', { timeout: 60000 });
  await capture(page, info, ['ATDD-038'], 'artwork-confirmed-partial');
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  const artwork = page.locator('main .artwork img').first();
  await expect(artwork).toBeVisible();
  await expect(artwork).toHaveAttribute('alt', movie.title);
  const metadata = await readApi(page, 'query UploadedArtwork($id: UUID!) { title(id: $id) { id artwork { assetId url width height } } }', { id: movie.id });
  expect(metadata.title.id).toBe(movie.id);
  expect(metadata.title.artwork.assetId).toMatch(/^[a-f0-9-]{36}$/i);
  const src = await artwork.getAttribute('src');
  expect(new URL(src!, page.url()).href).toBe(new URL(metadata.title.artwork.url, page.url()).href);
  expect(src).toBeTruthy();
  const image = await page.request.get(src!);
  expect(image.ok()).toBe(true);
  expect(image.headers()['content-type']).toMatch(/^image\/(png|jpeg|webp)/);
  const served = await image.body();
  expect(served.length).toBeGreaterThan(0);
  const dimensions = await artwork.evaluate((img: HTMLImageElement) => ({ width: img.naturalWidth, height: img.naturalHeight }));
  expect(dimensions).toEqual({ width: metadata.title.artwork.width, height: metadata.title.artwork.height });
  expect(dimensions.width).toBeGreaterThan(0);
  expect(dimensions.height).toBeGreaterThan(0);
  const input = readFileSync(resolve('../../docs/design-system/mockup/assets/letters.png'));
  await info.attach('uploaded-artwork-storage-projection', { body: JSON.stringify({ titleId: movie.id,
    assetId: metadata.title.artwork.assetId, dimensions, type: image.headers()['content-type'],
    inputSource: 'Approved mockup fixture letters.png, synthetic user upload; not production seed provenance proof',
    inputSha256: createHash('sha256').update(input).digest('hex'), servedSha256: createHash('sha256').update(served).digest('hex'),
    sha: process.env.TESTED_REF, boundary: 'Actual server metadata and HTTPS bytes rendered by the browser; internal database/blob storage requires its separate owner proof.' }), contentType: 'application/json' });
  await expect.poll(() => artwork.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0)).toBe(true);
  await page.reload();
  const after = await readApi(page, 'query ReloadedArtwork($id: UUID!) { title(id: $id) { artwork { assetId url width height } } }', { id: movie.id });
  expect(after.title.artwork).toEqual(metadata.title.artwork);
  await capture(page, info, ['ATDD-038', 'ATDD-274'], 'artwork-persisted-partial');
});

test('ATDD-039 partial: malformed file fails and does not create a saved attachment', async ({ page }, info) => {
  await signInMaintainer(page);
  await createMovie(page);
  await page.getByLabel('Poster artwork').setInputFiles({ name: 'malformed.png', mimeType: 'image/png', buffer: Buffer.from('invalid image content') });
  await expect(page.locator('.artwork-upload [role="status"]')).toHaveText('Upload failed', { timeout: 60000 });
  await expect(page.locator('.artwork-upload [role="status"]')).not.toHaveText('Saved');
  await capture(page, info, ['ATDD-039'], 'invalid-artwork');
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  await expect(page.locator('main .artwork img')).toHaveCount(0);
});
