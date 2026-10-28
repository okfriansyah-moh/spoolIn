import { cleanup, render, screen, waitFor } from '@testing-library/svelte';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import CatalogArtwork from './CatalogArtwork.svelte';

const download = vi.hoisted(() => vi.fn());
vi.mock('../features/artwork/read', () => ({ readArtwork: download }));
const src = '/artwork/00000000-0000-4000-8000-000000000001';
const revoke = vi.fn();
beforeEach(() => {
  download.mockReset();
  revoke.mockClear();
  vi.stubGlobal('URL', { revokeObjectURL: revoke });
  vi.stubGlobal('IntersectionObserver', undefined);
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

it('starts a poster only when visible, then releases its temporary URL on unmount', async () => {
  let visible: ((entries: { isIntersecting: boolean }[]) => void) | undefined;
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      constructor(callback: (entries: { isIntersecting: boolean }[]) => void) {
        visible = callback;
      }
      observe() {}
      disconnect() {}
    },
  );
  download.mockResolvedValue('blob:poster');
  const view = render(CatalogArtwork, { src });
  expect(download).not.toHaveBeenCalled();
  visible?.([{ isIntersecting: true }]);
  await waitFor(() =>
    expect(view.container.querySelector('img')?.getAttribute('src')).toBe(
      'blob:poster',
    ),
  );
  view.unmount();
  expect(revoke).toHaveBeenCalledWith('blob:poster');
  expect(download.mock.calls[0]?.[1].aborted).toBe(true);
});

it('cancels removed cards and revokes late replies instead of showing stale posters', async () => {
  let finish: ((url: string) => void) | undefined;
  download.mockImplementation(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  const view = render(CatalogArtwork, { src });
  await waitFor(() => expect(download).toHaveBeenCalledTimes(1));
  view.unmount();
  finish?.('blob:obsolete');
  await waitFor(() => expect(revoke).toHaveBeenCalledWith('blob:obsolete'));
  expect(document.querySelector('img')).toBeNull();
});

it('keeps missing or rejected artwork distinct from loading', async () => {
  download.mockRejectedValue(new Error('Missing'));
  render(CatalogArtwork, { src });
  expect(await screen.findByText('Artwork unavailable')).toBeTruthy();
});

it('cancels pending artwork outside the viewport margin', async () => {
  let intersect: ((entries: { isIntersecting: boolean }[]) => void) | undefined;
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      constructor(callback: (entries: { isIntersecting: boolean }[]) => void) {
        intersect = callback;
      }
      observe() {}
      disconnect() {}
    },
  );
  download.mockImplementation(() => new Promise(() => {}));
  render(CatalogArtwork, { src });
  intersect?.([{ isIntersecting: true }]);
  await waitFor(() => expect(download).toHaveBeenCalledTimes(1));
  intersect?.([{ isIntersecting: false }]);
  await waitFor(() => expect(download.mock.calls[0]?.[1].aborted).toBe(true));
});
