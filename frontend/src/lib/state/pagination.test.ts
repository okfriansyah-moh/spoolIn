import { expect, it } from 'vitest';
import { appendDistinct, readPageInfo } from './pagination';

it('requires an advancing server cursor and explicit page state', () => {
  expect(
    readPageInfo({ hasNextPage: true, endCursor: 'opaque-next' }, 'prior'),
  ).toEqual({ hasNextPage: true, endCursor: 'opaque-next' });
  for (const value of [
    null,
    {},
    { hasNextPage: true, endCursor: null },
    { hasNextPage: true, endCursor: 'prior' },
  ])
    expect(() => readPageInfo(value, 'prior')).toThrow();
});
it('never silently hides repeated rows from an inconsistent next page', () => {
  expect(appendDistinct([{ id: 'one' }], [{ id: 'two' }])).toEqual([
    { id: 'one' },
    { id: 'two' },
  ]);
  expect(() => appendDistinct([{ id: 'one' }], [{ id: 'one' }])).toThrow(
    'identity',
  );
  expect(() => appendDistinct([], [{ id: 'one' }, { id: 'one' }])).toThrow(
    'identity',
  );
});
