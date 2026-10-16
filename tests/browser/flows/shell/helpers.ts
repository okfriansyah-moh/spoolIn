import { expect, type Page, type TestInfo } from '@playwright/test';
import { mkdir, writeFile, readFile } from 'node:fs/promises';
import { resolve, extname } from 'node:path';
export const evidence = resolve('../../.ai/evidence/numbering-v1/g02-shell');
export async function setup(page: Page) {
  await page.route('https://example.test/**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'text/plain',
      body: 'Controlled URL target',
    }),
  );
  await page.route('**/artwork/**', (route) =>
    route.fulfill({ status: 404, body: 'Artwork unavailable' }),
  );
  await page.route(
    '**/artwork/33333333-3333-4333-8333-333333333333',
    async (route) =>
      route.fulfill({
        status: 200,
        contentType: 'image/png',
        body: await readFile(
          resolve('../../docs/design-system/mockup/assets/hero.png'),
        ),
      }),
  );
  await page.goto('/');
  await expect(
    page.getByRole('heading', { name: 'One component set.' }),
  ).toBeVisible();
}
export async function capture(page: Page, info: TestInfo, state: string) {
  await mkdir(evidence, { recursive: true });
  await page.screenshot({
    path: resolve(evidence, `${info.project.name}-${state}.png`),
    fullPage: true,
  });
  if (state === 'specimens')
    for (const id of [
      'identity',
      'typography',
      'buttons',
      'fields',
      'artwork',
      'dialog',
      'progress',
      'metadata',
      'conflict',
      'impact',
      'session',
      'proposal',
    ])
      await page.locator(`#${id}`).screenshot({
        path: resolve(evidence, `${info.project.name}-${id}.png`),
      });
}
export async function assertNoOverflow(page: Page) {
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
}
export async function assertGeometry(page: Page) {
  const heights = await page
    .locator('button,input:not([type="checkbox"]),select,textarea,nav a')
    .evaluateAll((elements) =>
      elements
        .filter((e) => e.getBoundingClientRect().width > 0)
        .map((e) => e.getBoundingClientRect().height),
    );
  expect(heights.every((height) => height >= 44)).toBe(true);
}
export async function recordEnvironment(page: Page, info: TestInfo) {
  const environment = {
    project: info.project.name,
    browser: page.context().browser()?.version(),
    viewport: page.viewportSize(),
    theme: await page.locator('html').getAttribute('data-theme'),
    reducedMotion: await page.evaluate(
      () => matchMedia('(prefers-reduced-motion: reduce)').matches,
    ),
    firefoxRuntime:
      info.project.name.startsWith('firefox') &&
      process.env.PLAYWRIGHT_FIREFOX_WS
        ? 'Official Microsoft Linux Playwright1.64.1 image; loopback forwarded by Playwright'
        : 'macOS native browser process',
    boundary:
      'real shared components with controlled data; no backend or authenticated-session claim',
  };
  await writeFile(
    resolve(evidence, `${info.project.name}-environment.json`),
    JSON.stringify(environment, null, 2),
  );
}
export async function reference(page: Page, info: TestInfo) {
  const root = resolve('../../docs/design-system/mockup');
  await page.route('**/__reference/**', async (route) => {
    const name =
      new URL(route.request().url()).pathname.split('/__reference/')[1] ?? '';
    const path = resolve(root, name);
    if (!path.startsWith(`${root}/`)) return route.abort();
    const types: Record<string, string> = {
      '.html': 'text/html',
      '.css': 'text/css',
      '.js': 'application/javascript',
      '.svg': 'image/svg+xml',
      '.png': 'image/png',
    };
    return route.fulfill({
      body: await readFile(path),
      contentType: types[extname(path)] ?? 'application/octet-stream',
    });
  });
  await page.goto('/__reference/components.html');
  await page.locator('html').evaluate((element, theme) => {
    (element as HTMLElement).dataset.designTarget = theme;
  }, info.project.use.colorScheme ?? 'dark');
  await page.screenshot({
    path: resolve(evidence, `${info.project.name}-reference.png`),
    fullPage: true,
  });
  for (const id of [
    'identity',
    'typography',
    'buttons',
    'fields',
    'artwork',
    'dialog',
    'progress',
    'navigation',
  ])
    await page.locator(`#${id}`).screenshot({
      path: resolve(evidence, `${info.project.name}-reference-${id}.png`),
    });
}

export async function assertTextSinks(page: Page) {
  const payloads = [
    '<script>window.__injected=1</script>',
    '<img src=x onerror="window.__injected=1">',
    '</p><script>window.__injected=1</script>',
    '" autofocus onfocus="window.__injected=1',
    '李 · O’Connor · é',
  ];
  for (const payload of payloads) {
    await page.locator('#source-text').fill(payload);
    await page.locator('#source-url').fill('https://example.test/source');
    expect(
      await page
        .getByTestId('safe-link')
        .locator('a')
        .evaluate((element) => element.textContent),
    ).toBe(payload);
    await page.locator('#source-url').fill('javascript:window.__injected=1');
    expect(
      await page
        .getByTestId('safe-link')
        .locator('span')
        .evaluate((element) => element.textContent),
    ).toBe(payload);
    await expect(page.getByTestId('safe-text')).toHaveText(payload, {
      useInnerText: false,
    });
    expect(
      await page
        .getByTestId('safe-text')
        .evaluate((element) => element.textContent),
    ).toBe(payload);
    expect(await page.getByTestId('safe-text').locator('*').count()).toBe(0);
    expect(
      await page
        .getByTestId('safe-link')
        .locator('[onerror],[onfocus],script,img')
        .count(),
    ).toBe(0);
  }
}

export async function assertDeniedUrls(page: Page) {
  const denied = [
    'javascript:window.__injected=1',
    'data:text/html,<script>window.__injected=1</script>',
    ' javascript:window.__injected=1',
    'java\nscript:window.__injected=1',
    '%6aavascript:window.__injected=1',
    '&#106;avascript:window.__injected=1',
    'https%3a//example.test',
    ' https://example.test',
  ];
  for (const url of denied) {
    await page.locator('#source-url').fill(url);
    await expect(page.getByTestId('safe-link').locator('a')).toHaveCount(0);
    await expect(page.getByTestId('safe-art').locator('img')).toHaveCount(0);
    const before = page.url();
    await page.getByTestId('safe-link').locator('span').click();
    expect(page.url()).toBe(before);
  }
}

export async function stressReading(page: Page, info: TestInfo) {
  await page.locator('body').evaluate((element) => {
    element.style.fontFamily = 'Arial, sans-serif';
  });
  await page.locator('h1,h2,h3').evaluateAll((elements) => {
    for (const element of elements)
      (element as HTMLElement).style.fontFamily = 'Times New Roman, serif';
  });
  await assertNoOverflow(page);
  await capture(page, info, 'font-fallback');
  await page
    .locator('body')
    .evaluate((element) => (element.style.fontFamily = ''));
  await page.locator('h1,h2,h3').evaluateAll((elements) => {
    for (const element of elements)
      (element as HTMLElement).style.fontFamily = '';
  });
}
export async function inspectFocus(page: Page) {
  const button = page.getByRole('button', { name: /Switch to .* theme/ });
  await button.focus();
  expect(
    await button.evaluate((element) => getComputedStyle(element).outlineWidth),
  ).toBe('3px');
}
export async function touchTheme(page: Page, info: TestInfo) {
  if (!info.project.use.hasTouch) return;
  const before = await page.locator('html').getAttribute('data-theme');
  await page.getByRole('button', { name: /Switch to .* theme/ }).tap();
  await expect(page.locator('html')).not.toHaveAttribute(
    'data-theme',
    before ?? '',
  );
  await page.getByRole('button', { name: /Switch to .* theme/ }).tap();
  await expect(page.locator('html')).toHaveAttribute(
    'data-theme',
    before ?? '',
  );
}

export async function checkNavigation(page: Page) {
  await page
    .getByRole('link', { name: 'Navigation fixture', exact: true })
    .click();
  await expect(page).toHaveTitle('Destination fixture · SpoolIn');
  await expect(
    page.getByRole('heading', { name: 'Destination heading' }),
  ).toBeFocused();
}

export async function inspectArtworkAndMotion(page: Page, info: TestInfo) {
  const image = page.locator('#artwork img').first();
  await expect(image).toBeVisible();
  await expect(image).toHaveAttribute('alt', 'A railway platform at dusk');
  expect(
    await image.evaluate(
      (element) => (element as HTMLImageElement).naturalWidth,
    ),
  ).toBeGreaterThan(0);
  expect(
    await image.evaluate((element) => getComputedStyle(element).objectFit),
  ).toBe('cover');
  const dimensions = await image.boundingBox();
  expect(
    dimensions && Math.abs(dimensions.width / dimensions.height - 2 / 3) < 0.01,
  ).toBe(true);
  if (info.project.use.reducedMotion === 'reduce') {
    expect(
      await page
        .locator('body')
        .evaluate((element) => getComputedStyle(element).scrollBehavior),
    ).toBe('auto');
    expect(
      await page
        .getByRole('button', { name: /Switch to .* theme/ })
        .evaluate((element) => getComputedStyle(element).transitionDuration),
    ).toBe('0s');
  }
}
export async function externalReferences(page: Page, info: TestInfo) {
  for (const [file, id, label] of [
    ['management', 'editor', 'conflict'],
    ['archives', 'impact-current', 'impact'],
    ['session', 'expired', 'session'],
    ['ai', 'chatgpt', 'proposal'],
  ]) {
    await page.goto(`/__reference/${file}.html#${id}`);
    await page.locator('html').evaluate((element, theme) => {
      (element as HTMLElement).dataset.designTarget = theme;
    }, info.project.use.colorScheme ?? 'dark');
    await expect(page.locator(`#${id}`)).toBeVisible();
    await page.locator(`#${id}`).screenshot({
      path: resolve(evidence, `${info.project.name}-reference-${label}.png`),
    });
  }
}

export async function assertValidation(page: Page, info: TestInfo) {
  await page.getByLabel('Title · Required', { exact: true }).fill('');
  await page.getByRole('button', { name: 'Validate fields' }).click();
  await expect(
    page.locator('[role="alert"]').filter({ hasText: 'Check these fields' }),
  ).toBeFocused();
  await expect(page.locator('#specimen-title')).toHaveAttribute(
    'aria-describedby',
    'specimen-title-help specimen-title-error',
  );
  await expect(page.locator('#specimen-synopsis')).toHaveValue(
    'A coastal town waits for its final train.\nAn archivist returns home.',
  );
  await capture(page, info, 'invalid');
  await page
    .locator('#specimen-title')
    .fill('A very long complete title · 李 · O’Connor · é');
  await page.getByRole('button', { name: 'Validate fields' }).click();
  await expect(
    page.getByText('Check these fields', { exact: true }),
  ).toHaveCount(0);
}
