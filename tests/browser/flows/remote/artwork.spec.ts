import { resolve } from 'node:path';
import { test, expect, signInMaintainer, createMovie, capture } from './helpers';

test('Artwork production upload confirms attachment and serves actual bytes after reload', async ({ page }, info) => {
  await signInMaintainer(page);
  await createMovie(page);
  await page.getByLabel('Poster artwork').setInputFiles(resolve('../../docs/design-system/mockup/assets/letters.png'));
  await expect(page.locator('.artwork-upload [role="status"]')).toHaveText('Saved', { timeout: 60000 });
  await capture(page, info, ['ATDD-038'], 'artwork-confirmed-partial');
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  const artwork = page.locator('main .artwork img').first();
  await expect(artwork).toBeVisible();
  const src = await artwork.getAttribute('src');
  expect(src).toBeTruthy();
  const image = await page.request.get(src!);
  expect(image.ok()).toBe(true);
  expect(image.headers()['content-type']).toMatch(/^image\/(png|jpeg|webp)/);
  expect((await image.body()).length).toBeGreaterThan(0);
  await expect.poll(() => artwork.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0)).toBe(true);
  await capture(page, info, ['ATDD-038'], 'artwork-persisted-partial');
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
