import { expect, it } from 'vitest';
import { navigation } from './navigation';
it('public and unverified contexts disclose no private archive links', () => {
  for (const capabilities of [
    { verified: false, manageCatalog: true },
    { verified: true, manageCatalog: false },
  ])
    expect(navigation(capabilities).map((x) => x.href)).toEqual([
      '/',
      '/people',
    ]);
});
it('verified capabilities permit management navigation', () => {
  expect(
    navigation({ verified: true, manageCatalog: true }).map((x) => x.href),
  ).toEqual([
    '/',
    '/people',
    '/manage/movies',
    '/manage/people',
    '/manage/archives',
    '/manage/audit',
  ]);
});
