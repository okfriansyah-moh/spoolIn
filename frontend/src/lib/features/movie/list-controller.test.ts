import { expect, it, vi } from 'vitest';
import { MovieListController } from './list-controller';

function page(id: string, cursor: string | null = null) {
  return {
    managedTitles: {
      nodes: [
        {
          id,
          kind: 'MOVIE',
          state: 'ACTIVE',
          title: id,
          version: '1',
          release: { year: 2024 },
        },
      ],
      pageInfo: { endCursor: cursor, hasNextPage: cursor !== null },
    },
  };
}
it('uses actual cursor and retains existing rows when a repeated page must be retried', async () => {
  const request = vi
    .fn()
    .mockResolvedValueOnce(page('one', 'next'))
    .mockResolvedValueOnce(page('two', 'later'))
    .mockResolvedValueOnce(page('two'))
    .mockResolvedValueOnce(page('three'));
  const controller = new MovieListController(request);
  const signal = new AbortController().signal;
  await controller.load('csrf', signal);
  await controller.loadMore('csrf', signal);
  expect(request.mock.calls[1]?.[1]).toEqual({
    state: 'ACTIVE',
    after: 'next',
  });
  expect(controller.snapshot).toMatchObject({
    kind: 'ready',
    titles: [{ id: 'one' }, { id: 'two' }],
    next: 'later',
  });
  await controller.loadMore('csrf', signal);
  expect(controller.snapshot).toMatchObject({
    kind: 'ready',
    pagingError: true,
    next: 'later',
    titles: [{ id: 'one' }, { id: 'two' }],
  });
  await controller.loadMore('csrf', signal);
  expect(request.mock.calls[3]?.[1]).toEqual({
    state: 'ACTIVE',
    after: 'later',
  });
  expect(controller.snapshot).toMatchObject({
    kind: 'ready',
    pagingError: false,
    next: null,
    titles: [{ id: 'one' }, { id: 'two' }, { id: 'three' }],
  });
  controller.dispose();
});

it('rejects claimed next-page availability without a usable cursor', async () => {
  const request = vi
    .fn()
    .mockResolvedValue({
      managedTitles: {
        nodes: [],
        pageInfo: { endCursor: null, hasNextPage: true },
      },
    });
  const controller = new MovieListController(request);
  await controller.load('csrf', new AbortController().signal);
  expect(controller.snapshot).toEqual({ kind: 'failure' });
});
