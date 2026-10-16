import { expect, it } from 'vitest';
import { safeDestination } from './routing';
it.each([
  'https://other.test/',
  '//other.test/',
  '/\\other.test/',
  '/graphql',
  '/sign-in',
  '/register',
  '/manage/movies',
])(
  'Viewer cannot acquire an external or restricted destination through %s',
  (value) => {
    expect(safeDestination(value, 'VIEWER')).toBe('/');
  },
);
it('preserves an allowed public destination and gives only verified Maintainer its workspace', () => {
  expect(safeDestination('/people?q=Owl', 'VIEWER')).toBe('/people?q=Owl');
  expect(safeDestination('/manage/movies', 'MAINTAINER')).toBe(
    '/manage/movies',
  );
  expect(safeDestination('/internal/alerts', 'MAINTAINER')).toBe('/');
});
