import type { Locator, Page } from '@playwright/test';
import { test, expect, capture } from './helpers';
import { fullStateInventory } from './state-mapping';

async function tabTo(page: Page, target: Locator) {
  for (let step = 0; step < 100; step++) {
    if (await target.evaluate((element) => element === document.activeElement)) return;
    await page.keyboard.press('Tab');
  }
  throw new Error('Required control was not reachable with keyboard traversal.');
}

async function activate(page: Page, target: Locator, method: string) {
  if (method === 'touch') await target.tap();
  else if (method === 'keyboard') { await tabTo(page, target); await page.keyboard.press('Enter'); }
  else await target.click();
}

for (const method of ['keyboard', 'pointer', 'touch']) {
  test(`ATDD-268/270/273 partial input-${method}: theme action preserves actual search state and focus`, async ({ page }, info) => {
    const enabled = info.project.metadata.inputMethods as string[];
    expect(enabled).toContain(method);
    await page.goto('/');
    const search = page.getByLabel('Search titles', { exact: true });
    const title = await page.locator('.title-card h3 a').first().innerText();
    const term = title.slice(0, 12);
    if (method === 'keyboard') await tabTo(page, search);
    else if (method === 'touch') await search.tap();
    else await search.click();
    await page.keyboard.type(term);
    await expect(search).toHaveValue(term);
    const initialTheme = await page.locator('html').getAttribute('data-theme');
    const toggle = page.getByRole('button', { name: /Switch to (light|dark) theme/ });
    await activate(page, toggle, method);
    await expect(page.locator('html')).toHaveAttribute('data-theme', initialTheme === 'dark' ? 'light' : 'dark');
    await expect(search).toHaveValue(term);
    if (method === 'keyboard') {
      await expect(toggle).toBeFocused();
      expect(await toggle.evaluate((element) => getComputedStyle(element).outlineStyle)).not.toBe('none');
    }
    await activate(page, page.getByRole('button', { name: 'Clear search text', exact: true }), method);
    await expect(search).toHaveValue('');
    await expect(page.locator('.title-card')).not.toHaveCount(0);
    expect(await page.locator('html').evaluate((root) => root.scrollWidth <= innerWidth)).toBe(true);
    await capture(page, info, ['ATDD-268', 'ATDD-270', 'ATDD-273'], `theme-${method}`);
  });
}

test('ATDD-270/271/273 partial presentation-properties: typography, solid contrast and 200 percent CSS layout scaling', async ({ page }, info) => {
  await page.goto('/');
  await expect(page.locator('.title-card')).not.toHaveCount(0);
  const measurements = await page.locator('body').evaluate((body) => {
    const style = getComputedStyle(body);
    const luminance = (color: string) => {
      const channels = (color.match(/[\d.]+/g) ?? []).slice(0, 3).map(Number).map((value) => {
        const channel = value / 255;
        return channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
      });
      return (channels[0] ?? 0) * 0.2126 + (channels[1] ?? 0) * 0.7152 + (channels[2] ?? 0) * 0.0722;
    };
    const text = luminance(style.color);
    const background = luminance(style.backgroundColor);
    return { fontFamily: style.fontFamily, fontSize: style.fontSize, lineHeight: style.lineHeight,
      textColor: style.color, background: style.backgroundColor, ratio: (Math.max(text, background) + 0.05) / (Math.min(text, background) + 0.05) };
  });
  expect(measurements.fontFamily).toContain('Avenir Next');
  expect(Number.parseFloat(measurements.fontSize)).toBeGreaterThanOrEqual(16);
  expect(measurements.ratio).toBeGreaterThanOrEqual(4.5);
  await page.evaluate(() => { document.documentElement.style.zoom = '2'; });
  expect(await page.locator('html').evaluate((root) => root.scrollWidth <= innerWidth)).toBe(true);
  await capture(page, info, ['ATDD-270', 'ATDD-271', 'ATDD-273'], 'text-scale');
  await info.attach('presentation-measurements-and-pending-boundaries', { body: JSON.stringify({ measurements,
    sourceStates: fullStateInventory(), actualBrowserZoom: 'Pending; CSS layout scaling is not native browser zoom or assistive text scaling.',
    readers: 'Pending actual assistive-technology observations; DOM assertions do not satisfy speech.',
    fonts: 'Requested family and fallback recorded. Matching Avenir Next/Didot installations are not established by computed CSS.',
    imageContrast: 'Pending complete actual image-overlay pairs; one solid body pair is partial.' }), contentType: 'application/json' });
});
