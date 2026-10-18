import {
  test,
  expect,
  signInMaintainer,
  createMovie,
  createPerson,
  capture,
} from './helpers';

test('Responsive route review: production pages reflow across approved widths and themes', async ({
  page,
}, info) => {
  test.setTimeout(600_000);
  await signInMaintainer(page);
  const movie = await createMovie(page);
  const person = await createPerson(page);
  const routes = [
    { path: '/', heading: 'Every reel holds a story.' },
    { path: `/titles/${movie.id}`, heading: movie.title },
    { path: '/people', heading: 'People' },
    { path: `/people/${person.id}`, heading: person.name },
    { path: '/sign-in', heading: 'Signed in as Maintainer' },
    { path: '/register', heading: 'Signed in as Maintainer' },
    { path: '/manage', heading: 'Management' },
    {
      path: '/manage/unavailable-review-route',
      heading: 'This management section is unavailable',
    },
    { path: '/manage/movies', heading: 'Movies' },
    { path: '/manage/movies/new', heading: 'Create movie' },
    { path: `/manage/movies/${movie.id}`, heading: movie.title },
    { path: '/manage/people', heading: 'People' },
    { path: '/manage/people/new', heading: 'Create person' },
    { path: `/manage/people/${person.id}`, heading: person.name },
    { path: '/manage/archives', heading: 'Archives' },
    { path: '/manage/audit', heading: 'Audit history' },
  ];
  const widths = [320, 390, 768, 1024, 1440];
  const themes = ['dark', 'light'] as const;
  const observations: Array<{
    route: string;
    heading: string;
    viewport: number;
    theme: (typeof themes)[number];
    result: 'pass';
  }> = [];

  for (const width of widths) {
    await page.setViewportSize({ width, height: 900 });
    for (const theme of themes) {
      await page.goto('/');
      await expect(page.locator('html')).toHaveAttribute(
        'data-theme',
        /^(dark|light)$/,
      );
      const currentTheme = await page
        .locator('html')
        .getAttribute('data-theme');
      if (currentTheme !== theme) {
        await page
          .getByRole('button', {
            name: `Switch to ${currentTheme === 'dark' ? 'light' : 'dark'} theme`,
          })
          .click();
      }
      await expect(page.locator('html')).toHaveAttribute('data-theme', theme);

      for (const route of routes) {
        await page.goto(route.path);
        await expect(
          page.getByRole('heading', { level: 1, name: route.heading }),
        ).toBeVisible();
        await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
        const layout = await page.locator('html').evaluate((root) => {
          const width = window.innerWidth;
          const controls = Array.from(
            document.querySelectorAll<HTMLElement>(
              'a, button, input, select, textarea, summary',
            ),
          )
            .filter((element) => element.getClientRects().length > 0)
            .map((element) => {
              const bounds = element.getBoundingClientRect();
              return {
                label:
                  element.getAttribute('aria-label') ||
                  element.textContent?.trim() ||
                  element.tagName,
                left: bounds.left,
                right: bounds.right,
              };
            })
            .filter((item) => item.left < -1 || item.right > width + 1);
          const shortTargets = Array.from(
            document.querySelectorAll<HTMLElement>(
              'button, input, select, textarea, summary, a.button, .primary-navigation a',
            ),
          )
            .filter((element) => element.getClientRects().length > 0)
            .filter((element) => element.getBoundingClientRect().height < 43)
            .map((element) => ({
              label:
                element.getAttribute('aria-label') ||
                element.textContent?.trim() ||
                element.tagName,
              height: element.getBoundingClientRect().height,
            }));
          return {
            viewport: width,
            document: root.scrollWidth,
            controls,
            shortTargets,
          };
        });
        expect(
          layout.document,
          `${route.path} page-level horizontal overflow at ${width}px`,
        ).toBeLessThanOrEqual(width);
        expect(
          layout.controls,
          `${route.path} clipped controls at ${width}px`,
        ).toEqual([]);
        expect(
          layout.shortTargets,
          `${route.path} touch target below 44px at ${width}px`,
        ).toEqual([]);
        const currentNavigation = page
          .getByRole('navigation', { name: 'Main navigation' })
          .locator('[aria-current="page"]');
        if (route.path.startsWith('/manage/movies/'))
          await expect(currentNavigation).toHaveText('Movies');
        else if (route.path.startsWith('/manage/people/'))
          await expect(currentNavigation).toHaveText('Manage people');
        observations.push({
          route: route.path,
          heading: route.heading,
          viewport: width,
          theme,
          result: 'pass',
        });

        if (
          (width === 320 &&
            theme === 'dark' &&
            (route.path === '/' ||
              route.path === `/manage/movies/${movie.id}`)) ||
          (width === 768 &&
            theme === 'light' &&
            route.path === '/manage/movies/new') ||
          (width === 1440 &&
            theme === 'dark' &&
            route.path === '/manage/archives')
        ) {
          await capture(
            page,
            info,
            ['ATDD-270', 'ATDD-273'],
            `responsive-${width}-${theme}-${route.path === '/' ? 'catalog' : route.path.split('/').filter(Boolean).join('-') || 'home'}`,
          );
        }
      }
    }
  }

  await page.setViewportSize({ width: 320, height: 900 });
  await page.goto('/manage/movies/new');
  await page.evaluate(() => {
    document.documentElement.style.zoom = '2';
  });
  expect(
    await page
      .locator('html')
      .evaluate((root) => root.scrollWidth <= innerWidth),
  ).toBe(true);
  await page.evaluate(() => {
    document.documentElement.style.zoom = '';
  });

  await info.attach('responsive-route-observations', {
    body: JSON.stringify({
      testedSha:
        process.env.TESTED_REF ??
        process.env.TESTED_SHA ??
        process.env.GITHUB_SHA,
      browser: info.project.use.browserName,
      browserVersion: page.context().browser()?.version(),
      runnerOS: process.env.RUNNER_OS,
      imageOS: process.env.ImageOS,
      imageVersion: process.env.ImageVersion,
      observations,
      result:
        'All listed production routes rendered with expected headings and no horizontal page overflow or clipped controls.',
      references:
        'Comparisons use approved textual requirements and design tokens; no exact pixel identity claim.',
    }),
    contentType: 'application/json',
  });
});
