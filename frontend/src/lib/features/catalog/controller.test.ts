/**
 * T013 – Catalog controller unit tests.
 *
 * Covers:
 * - Generation-bound stale reply suppression (ATDD-303, AC-009, ATDD-256-T1-R05)
 * - Invalid text moves to 'invalid' without querying (AC-159, ATDD-256-T1-R06)
 * - Empty criteria → 'empty' state; filtered criteria → 'no-match' (AC-015, AC-013)
 * - Failure state preserved for retry (AC-009, ATDD-256-T1-R04)
 * - Criteria preserved across all states (AC-007, AC-009, AC-015)
 * - Month cleared when year is cleared (AC-014)
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CatalogController } from './controller.svelte';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function makeRequest(respond: () => Promise<unknown>) {
  return vi.fn((_query: string, _vars: unknown, _opts: unknown) => respond());
}

function titlesResponse(nodes: unknown[] = [], hasNextPage = false) {
  return {
    titles: {
      nodes,
      pageInfo: { endCursor: hasNextPage ? 'next' : null, hasNextPage },
    },
  };
}

function oneTitle() {
  return [
    {
      id: 'uuid-m-a',
      kind: 'MOVIE',
      state: 'ACTIVE',
      title: 'Light',
      originalTitle: null,
      synopsis: '',
      genres: [],
      release: { year: 2024, month: null, day: null, precision: 'YEAR' },
      runtimeMinutes: null,
      originalLanguage: null,
      synopsisLanguage: 'en',
      countries: [],
      tvStatus: null,
      tvEnd: null,
      releaseContext: '2024',
      provenance: [],
      artwork: {
        assetId: 'a',
        url: 'https://img/a.jpg',
        width: 500,
        height: 750,
      },
      credits: { nodes: [], pageInfo: { endCursor: null, hasNextPage: false } },
      version: '1',
    },
  ];
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('CatalogController', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  it('begins in loading state', () => {
    const req = makeRequest(() => new Promise(() => {}));
    const ctrl = new CatalogController(req);
    expect(ctrl.snapshot.kind).toBe('loading');
    ctrl.dispose();
  });

  it('transitions to catalog state on successful response', async () => {
    const req = makeRequest(() => Promise.resolve(titlesResponse(oneTitle())));
    const ctrl = new CatalogController(req);

    ctrl.load({ text: '', kind: '', genres: [], year: '', month: '' });
    await vi.runAllTimersAsync();

    expect(ctrl.snapshot.kind).toBe('catalog');
    const catalog = ctrl.snapshot;
    expect(catalog.titles).toHaveLength(1);
    expect(catalog.titles[0]!.title).toBe('Light');
    ctrl.dispose();
  });

  it('transitions to empty when no filters and no results', async () => {
    const req = makeRequest(() => Promise.resolve(titlesResponse([])));
    const ctrl = new CatalogController(req);

    ctrl.load({ text: '', kind: '', genres: [], year: '', month: '' });
    await vi.runAllTimersAsync();

    expect(ctrl.snapshot.kind).toBe('empty');
    ctrl.dispose();
  });

  it('transitions to no-match when filters active and no results', async () => {
    const req = makeRequest(() => Promise.resolve(titlesResponse([])));
    const ctrl = new CatalogController(req);

    ctrl.load({
      text: 'ZZ-no-match',
      kind: '',
      genres: [],
      year: '',
      month: '',
    });
    await vi.runAllTimersAsync();

    expect(ctrl.snapshot.kind).toBe('no-match');
    ctrl.dispose();
  });

  it('transitions to failure on transport error', async () => {
    const req = makeRequest(() => Promise.reject(new Error('network down')));
    const ctrl = new CatalogController(req);

    ctrl.load({ text: '', kind: '', genres: [], year: '', month: '' });
    await vi.runAllTimersAsync();

    const failure = ctrl.snapshot;
    expect(failure.kind).toBe('failure');
    expect(failure!.criteria.text).toBe('');
    ctrl.dispose();
  });

  it('preserves criteria through failure state for retry', async () => {
    let callCount = 0;
    const req = makeRequest(() => {
      callCount += 1;
      if (callCount === 1) return Promise.reject(new Error('fail'));
      return Promise.resolve(titlesResponse(oneTitle()));
    });
    const ctrl = new CatalogController(req);

    ctrl.load({ text: 'Light', kind: '', genres: [], year: '', month: '' });
    await vi.runAllTimersAsync();

    const failure = ctrl.snapshot;
    expect(failure).toBeDefined();
    expect(failure!.criteria.text).toBe('Light');

    ctrl.retry();
    await vi.runAllTimersAsync();

    expect(ctrl.snapshot.kind).toBe('catalog');
    ctrl.dispose();
  });

  it('moves to invalid state for text > 200 code points without querying', async () => {
    const req = makeRequest(() => Promise.resolve(titlesResponse(oneTitle())));
    const ctrl = new CatalogController(req);

    ctrl.load({
      text: 'a'.repeat(201),
      kind: '',
      genres: [],
      year: '',
      month: '',
    });
    await vi.runAllTimersAsync();

    expect(req).not.toHaveBeenCalled();
    const invalid = ctrl.snapshot;
    expect(invalid.kind).toBe('invalid');
    expect(invalid!.invalidField).toBe('text');
    // Criteria retained
    expect(invalid!.criteria.text).toBe('a'.repeat(201));
    ctrl.dispose();
  });

  it('does NOT replace current state with a stale reply from a prior generation', async () => {
    let resolveLate!: (v: unknown) => void;
    let resolveEarly!: (v: unknown) => void;
    const latePromise = new Promise((r) => {
      resolveLate = r;
    });
    const earlyPromise = new Promise((r) => {
      resolveEarly = r;
    });

    let callCount = 0;
    const req = makeRequest(() => {
      callCount++;
      return callCount === 1 ? latePromise : earlyPromise;
    });

    const ctrl = new CatalogController(req);

    // First search (generation 1) – will respond late
    ctrl.load({ text: 'Light', kind: '', genres: [], year: '', month: '' });

    // Second search (generation 2) – will respond first
    ctrl.load({ text: 'Map', kind: '', genres: [], year: '', month: '' });
    await vi.runAllTimersAsync();

    // Resolve "Map" (early, generation 2)
    resolveEarly(titlesResponse(oneTitle()));
    await vi.runAllTimersAsync();

    const afterEarly = ctrl.snapshot;
    expect(afterEarly.kind).toBe('catalog');
    expect(afterEarly.criteria.text).toBe('Map');

    // Resolve "Light" (late, generation 1 – stale)
    resolveLate(titlesResponse([]));
    await vi.runAllTimersAsync();

    // State must NOT change – stale response is suppressed
    const afterLate = ctrl.snapshot;
    expect(afterLate.kind).toBe('catalog');
    expect(afterLate.criteria.text).toBe('Map');

    ctrl.dispose();
  });

  it('clears month when setCriteria sets year to empty', async () => {
    const req = makeRequest(() => Promise.resolve(titlesResponse(oneTitle())));
    const ctrl = new CatalogController(req);

    // Set year+month first
    ctrl.load({ text: '', kind: '', genres: [], year: '2024', month: '10' });
    await vi.runAllTimersAsync();

    // Clear year
    ctrl.setCriteria({ text: '', kind: '', genres: [], year: '', month: '10' });
    await vi.runAllTimersAsync();

    // Month must be cleared
    expect(ctrl.snapshot.criteria.month).toBe('');
    ctrl.dispose();
  });

  it('applies 250ms debounce on text changes', async () => {
    const req = makeRequest(() => Promise.resolve(titlesResponse(oneTitle())));
    const ctrl = new CatalogController(req);

    // Trigger text change with debounce
    ctrl.setCriteria(
      { text: 'Li', kind: '', genres: [], year: '', month: '' },
      { debounce: true },
    );
    ctrl.setCriteria(
      { text: 'Lig', kind: '', genres: [], year: '', month: '' },
      { debounce: true },
    );
    ctrl.setCriteria(
      { text: 'Light', kind: '', genres: [], year: '', month: '' },
      { debounce: true },
    );

    // Before debounce fires
    await vi.advanceTimersByTimeAsync(100);
    expect(req).not.toHaveBeenCalled();

    // After debounce
    await vi.advanceTimersByTimeAsync(250);
    expect(req).toHaveBeenCalledTimes(1);

    ctrl.dispose();
  });
});

it('loads the actual next cursor and preserves it when retrying or restoring public history', async () => {
  vi.useFakeTimers();
  const first = titlesResponse(oneTitle());
  first.titles.pageInfo = { endCursor: 'next', hasNextPage: true };
  const second = titlesResponse(
    oneTitle().map((row) => ({ ...row, id: 'second' })),
  );
  const request = vi
    .fn()
    .mockResolvedValueOnce(first)
    .mockResolvedValue(second);
  const controller = new CatalogController(request);
  const criteria = { text: 'Light', kind: '', genres: [], year: '', month: '' };
  controller.load(criteria);
  await vi.runAllTimersAsync();
  await controller.loadMore();
  expect(request.mock.calls[1]?.[1]).toMatchObject({
    after: 'next',
    filter: { text: 'Light' },
  });
  expect(controller.snapshot.titles.map((row) => row.id)).toEqual(['second']);
  controller.retry();
  await vi.runAllTimersAsync();
  expect(request.mock.calls[2]?.[1]).toMatchObject({ after: 'next' });
  controller.load(criteria, 'restored');
  await vi.runAllTimersAsync();
  expect(request.mock.calls[3]?.[1]).toMatchObject({ after: 'restored' });
  controller.dispose();
  vi.useRealTimers();
});

it('keeps a failed next page actionable and a newer filter cannot receive its old rows', async () => {
  vi.useFakeTimers();
  const first = titlesResponse(oneTitle());
  first.titles.pageInfo = { endCursor: 'next', hasNextPage: true };
  const request = vi
    .fn()
    .mockResolvedValueOnce(first)
    .mockRejectedValueOnce(new Error('Unavailable'))
    .mockResolvedValue(titlesResponse([]));
  const controller = new CatalogController(request);
  controller.load({ text: '', kind: '', genres: [], year: '', month: '' });
  await vi.runAllTimersAsync();
  await controller.loadMore();
  expect(controller.snapshot.titles).toHaveLength(1);
  expect(controller.snapshot.pageError).toContain('Retry');
  controller.setCriteria({
    text: 'new',
    kind: '',
    genres: [],
    year: '',
    month: '',
  });
  await vi.runAllTimersAsync();
  expect(controller.snapshot.criteria.text).toBe('new');
  expect(controller.snapshot.titles).toHaveLength(0);
  controller.dispose();
  vi.useRealTimers();
});
