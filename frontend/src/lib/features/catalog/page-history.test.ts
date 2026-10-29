import { expect, it } from 'vitest';
import { catalogPageStarts } from './page-history';
import { readCatalogReturn } from './return-context';

it('restores known page starts and discards corrupt browser state', () => {
  expect(
    catalogPageStarts({ catalogPages: [null, 'two', 'three'] }, 'three'),
  ).toEqual([null, 'two', 'three']);
  for (const state of [
    null,
    {},
    { catalogPages: ['two'] },
    { catalogPages: [null, 2] },
    { catalogPages: [null, 'two', 'two'] },
  ]) {
    expect(catalogPageStarts(state, 'two')).toEqual([null, 'two']);
  }
  expect(catalogPageStarts({ catalogPages: [null, 'two'] }, 'missing')).toEqual(
    [null, 'missing'],
  );
});

it('permits only same-origin catalog return destinations and retains sort and cursors', () => {
  const url = new URL('https://localhost/titles/id');
  const context = {
    url: 'https://localhost/?sort=TITLE_ASC',
    catalogCursor: 'two',
    catalogPages: [null, 'two'],
  };
  expect(readCatalogReturn({ catalogReturn: context }, url, '/')).toEqual(
    context,
  );
  for (const bad of [
    'https://elsewhere.test/',
    'https://localhost/manage',
    'javascript:alert(1)',
  ]) {
    expect(
      readCatalogReturn({ catalogReturn: { ...context, url: bad } }, url, '/'),
    ).toBeNull();
  }
  expect(readCatalogReturn({}, url, '/')).toBeNull();
  expect(
    readCatalogReturn(
      { catalogReturn: { ...context, catalogPages: [2] } },
      url,
      '/',
    ),
  ).toBeNull();
});
