import { expect, it, vi } from 'vitest';
import { PersonListController } from './list-controller';
import type { ListState } from './types';

function page(id: string, cursor: string | null = null) {
  return {
    managedPeople: {
      nodes: [{ id, name: id, state: 'ACTIVE', biography: null, version: '1' }],
      pageInfo: { endCursor: cursor, hasNextPage: cursor !== null },
    },
  };
}
it('appends the actual next page and refuses duplicate identities without hiding existing people', async () => {
  const request = vi
    .fn()
    .mockResolvedValueOnce(page('one', 'next'))
    .mockResolvedValueOnce(page('two', 'later'))
    .mockResolvedValue(page('two'));
  const controller = new PersonListController(request);
  let current: ListState = { kind: 'loading' };
  controller.subscribe((next) => {
    current = next;
  });
  const signal = new AbortController().signal;
  await controller.load('csrf', signal);
  await controller.loadMore('csrf', signal);
  expect(request.mock.calls[1]?.[1]).toEqual({
    state: 'ACTIVE',
    after: 'next',
  });
  expect(current).toMatchObject({
    kind: 'ready',
    people: [{ id: 'one' }, { id: 'two' }],
  });
  await controller.loadMore('csrf', signal);
  expect(current).toMatchObject({
    kind: 'ready',
    pageError: expect.stringContaining('Retry'),
    people: [{ id: 'one' }, { id: 'two' }],
  });
  controller.dispose();
});
