import { expect, it, vi } from 'vitest';
import { render } from 'svelte/server';
import EditorPageHeading from './EditorPageHeading.svelte';

vi.mock('$app/paths', () => ({
  resolve: (path: string) => `/screenings/${path}`,
}));

it('resolves editor links once with a configured base path', () => {
  const { body } = render(EditorPageHeading, {
    props: {
      sectionLabel: 'Movies',
      sectionHref: 'manage/movies',
      title: 'Edit movie',
      description: 'Update the movie.',
      actionHref: 'titles/movie%2Fid',
      actionLabel: 'Review public record',
    },
  });
  expect(body).toContain('href="/screenings/manage/movies"');
  expect(body).toContain('href="/screenings/titles/movie%2Fid"');
  expect(body).not.toContain('/screenings/screenings/');
  expect(body).toContain('Review public record');
});

it('omits the optional action for a new person', () => {
  const { body } = render(EditorPageHeading, {
    props: {
      sectionLabel: 'People',
      sectionHref: 'manage/people',
      title: 'Create person',
      description: 'Add a person.',
    },
  });
  expect(body).toContain('href="/screenings/manage/people"');
  expect(body).not.toContain('class="button secondary"');
});
