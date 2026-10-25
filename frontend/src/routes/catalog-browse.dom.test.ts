import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/svelte';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import Title from './titles/[id]/+page.svelte';
import Person from './people/[id]/+page.svelte';
import People from './people/+page.svelte';
import Catalog from './+page.svelte';

const fixture = vi.hoisted(() => ({
  page: { params: { id: '00000000-0000-4000-8000-000000000001' }, url: new URL('https://localhost/'), state: {} },
  goto: vi.fn(async () => {}),
}));
vi.mock('$app/state', () => ({ ...fixture, beforeNavigate: vi.fn(), afterNavigate: vi.fn() }));
vi.mock('$app/paths', () => ({ resolve: (path: string, params: Record<string, string> = {}) =>
  Object.entries(params).reduce((result, [key, value]) => result.replace(`[${key}]`, encodeURIComponent(value)), path) }));

const id = '00000000-0000-4000-8000-000000000001';
const other = '00000000-0000-4000-8000-000000000002';
const third = '00000000-0000-4000-8000-000000000003';
const end = { hasNextPage: false, endCursor: null };
const more = { hasNextPage: true, endCursor: 'page-2' };
function credit(creditId = id, name = 'Alex') {
  return { id: creditId, personId: other, personName: name, titleId: id, titleName: 'River',
    role: { id: third, label: 'Actor', category: 'CAST', allowsCharacters: true }, characters: ['Alice'], version: '1' };
}
function title(overrides: Record<string, unknown> = {}) {
  return { id, kind: 'MOVIE', state: 'ACTIVE', title: 'River', originalTitle: 'Rivière', synopsis: 'A quiet journey.',
    genres: [{ id: third, label: 'Drama' }], release: { year: 2024, month: 2, day: 29, precision: 'DAY' },
    runtimeMinutes: 95, originalLanguage: 'fr', synopsisLanguage: 'en', countries: ['FR', 'US'],
    tvStatus: null, tvEnd: null, releaseContext: 'ORIGINAL_RELEASE', artwork: null,
    provenance: [{ source: 'wikidata', externalId: 'Q42', recordUrl: 'https://www.wikidata.org/wiki/Q42',
      retrievedAt: '2026-01-01T00:00:00Z', reuseNote: 'CC0 facts', fieldSources: {}, locallyChanged: [] }],
    credits: { nodes: [credit()], pageInfo: end }, version: '1', ...overrides };
}
function http(respond: (query: string, variables: Record<string, unknown>) => unknown | Promise<unknown>) {
  const fetcher = vi.fn<typeof fetch>(async (_url, init) => {
    const { query, variables } = JSON.parse(String(init?.body));
    const data = await respond(query, variables);
    return data instanceof Response ? data : new Response(JSON.stringify({ data }));
  });
  vi.stubGlobal('fetch', fetcher);
  return fetcher;
}
const filterOptions = { genres: [{ id: third, label: 'Drama' }], releaseYears: [{ year: 2024, months: [2] }] };
beforeEach(() => { fixture.page.params.id = id; fixture.page.url = new URL('https://localhost/'); fixture.page.state = {}; fixture.goto.mockClear(); });
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

it('shows loading until the requested title arrives and links real person identities', async () => {
  let complete: ((value: unknown) => void) | undefined;
  http(() => new Promise((resolve) => { complete = resolve; }));
  render(Title);
  expect(await screen.findByRole('heading', { name: 'Loading…' })).toBeTruthy();
  await waitFor(() => expect(complete).toBeDefined());
  complete?.({ title: title() });
  expect(await screen.findByRole('heading', { name: 'River' })).toBeTruthy();
  expect(screen.getByText('A quiet journey.')).toHaveProperty('lang', 'en');
  expect(screen.getByText('1 h 35 min')).toBeTruthy();
  expect(screen.getByText('FR, US')).toBeTruthy();
  expect(screen.getByRole('link', { name: 'Alex' }).getAttribute('href')).toBe(`/people/${other}`);
  expect(screen.getByRole('link', { name: 'https://www.wikidata.org/wiki/Q42' }).getAttribute('href')).toBe('https://www.wikidata.org/wiki/Q42');
});

it.each([[45, '45 min'], [120, '2 h'], [null, 'Runtime unknown']])('preserves movie runtime formatting %s', async (minutes, expected) => {
  http(() => ({ title: title({ runtimeMinutes: minutes, originalTitle: null, countries: [], provenance: [], credits: { nodes: [], pageInfo: end } }) }));
  render(Title);
  expect(await screen.findByText(expected)).toBeTruthy();
  expect(screen.getByText('No credits known.')).toBeTruthy();
  expect(screen.queryByText('Original title:')).toBeNull();
});

it('renders TV first-air and end dates without offering movie editing controls', async () => {
  http(() => ({ title: title({ kind: 'TV', tvStatus: 'ENDED', tvEnd: { year: 2025, month: null, day: null, precision: 'YEAR' },
    releaseContext: 'FIRST_AIR', credits: { nodes: [credit()], pageInfo: end } }) }));
  render(Title);
  expect(await screen.findByText('TV Series')).toBeTruthy();
  expect(screen.getByText(/First aired:/)).toBeTruthy();
  expect(screen.getByText('Ended: 2025')).toBeTruthy();
  expect(screen.getByText('ENDED')).toBeTruthy();
  expect(screen.queryByRole('button', { name: /edit|save|track episode/i })).toBeNull();
});

it('preserves unknown TV release runtime and missing end precision', async () => {
  http(() => ({ title: title({ kind: 'TV', release: null, runtimeMinutes: null, tvEnd: null, originalTitle: null,
    tvStatus: null, countries: [], genres: [], provenance: [], credits: { nodes: [], pageInfo: end } }) }));
  render(Title);
  expect(await screen.findByText('First air date unknown')).toBeTruthy();
  expect(screen.getByText('Episode runtime unknown')).toBeTruthy();
  expect(screen.getByText('No credits known.')).toBeTruthy();
  expect(screen.queryByText(/^Ended:/)).toBeNull();
});

it.each([null, title({ state: 'ARCHIVED' })])('keeps absent or archived title details unavailable', async (record) => {
  http(() => ({ title: record }));
  render(Title);
  expect(await screen.findByRole('heading', { name: 'This title is unavailable' })).toBeTruthy();
  expect(screen.queryByText('A quiet journey.')).toBeNull();
});

it('rejects malformed title identity before fetching and retries failed title reads', async () => {
  fixture.page.params.id = 'invalid';
  const fetcher = http(() => ({ title: title() }));
  const view = render(Title);
  expect(await screen.findByText('This title is unavailable')).toBeTruthy();
  expect(fetcher).not.toHaveBeenCalled();
  view.unmount(); fixture.page.params.id = id;
  fetcher.mockResolvedValueOnce(new Response('{}', { status: 503 }));
  render(Title);
  expect(await screen.findByRole('heading', { name: 'Could not load title' })).toBeTruthy();
  await fireEvent.click(screen.getByRole('button', { name: 'Retry title' }));
  expect(await screen.findByRole('heading', { name: 'River' })).toBeTruthy();
});

it('retries a failed title credit page and appends distinct identities without losing existing credits', async () => {
  let fail = true;
  http((query, variables) => {
    if (query.includes('query TitleCredits')) {
      expect(variables.after).toBe('page-2');
      if (fail) { fail = false; return new Response('{}', { status: 503 }); }
      return { titleCredits: { nodes: [credit(), credit(third, 'Bea')], pageInfo: end } };
    }
    return { title: title({ credits: { nodes: [credit()], pageInfo: more } }) };
  });
  render(Title);
  await fireEvent.click(await screen.findByRole('button', { name: 'Load more credits' }));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', expect.stringContaining('More credits could not be loaded'));
  expect(screen.getByRole('link', { name: 'Alex' })).toBeTruthy();
  await fireEvent.click(screen.getByRole('button', { name: 'Retry more credits' }));
  expect(await screen.findByRole('link', { name: 'Bea' })).toBeTruthy();
  expect(screen.getAllByRole('link', { name: 'Alex' })).toHaveLength(1);
  expect(screen.queryByRole('button', { name: 'Load more credits' })).toBeNull();
});

it('person detail retries the initial read then retries a failed distinct credit continuation', async () => {
  let personFailure = true;
  let continuationFailure = true;
  http((query, variables) => {
    if (query.includes('query Person(')) {
      if (personFailure) { personFailure = false; return new Response('{}', { status: 503 }); }
      return { person: { id, name: 'Alex', biography: 'Director and performer.', state: 'ACTIVE', version: '1' } };
    }
    if (variables.after) {
      if (continuationFailure) { continuationFailure = false; return new Response('{}', { status: 503 }); }
      return { personCredits: { nodes: [credit(), { ...credit(third), titleId: third, titleName: 'Mountain' }], pageInfo: end } };
    }
    return { personCredits: { nodes: [credit()], pageInfo: more } };
  });
  render(Person);
  expect(await screen.findByRole('heading', { name: 'Could not load person' })).toBeTruthy();
  await fireEvent.click(screen.getByRole('button', { name: 'Retry person' }));
  expect(await screen.findByText('Director and performer.')).toBeTruthy();
  await fireEvent.click(screen.getByRole('button', { name: 'Load more credits' }));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', expect.stringContaining('More credits could not be loaded'));
  await fireEvent.click(screen.getByRole('button', { name: 'Retry more credits' }));
  const link = await screen.findByRole('link', { name: 'Mountain' });
  expect(link.getAttribute('href')).toBe(`/titles/${third}`);
  expect(screen.getAllByRole('link', { name: 'River' })).toHaveLength(1);
});

it('public people pagination retains biography text and advances using returned cursor', async () => {
  const fetcher = http((_query, variables) => ({ people: { nodes: [{ id: variables.after ? other : id,
    name: variables.after ? 'Bea' : 'Alex', biography: 'A'.repeat(140), state: 'ACTIVE', version: '1' }],
    pageInfo: variables.after ? end : more } }));
  fixture.page.url = new URL('https://localhost/people');
  render(People);
  expect(await screen.findByRole('link', { name: 'Alex' })).toBeTruthy();
  expect(screen.getByText('Read full biography')).toBeTruthy();
  await fireEvent.click(screen.getByRole('button', { name: 'Next people page' }));
  expect(await screen.findByRole('link', { name: 'Bea' })).toBeTruthy();
  expect(fetcher.mock.calls.some(([, init]) => JSON.parse(String(init?.body)).variables.after === 'page-2')).toBe(true);
});

it('catalog initializes URL criteria and filter choices then appends the next distinct title page', async () => {
  fixture.page.url = new URL(`https://localhost/?q=River&kind=MOVIE&year=2024&month=2&genre=${third}`);
  const fetcher = http((query, variables) => query.includes('CatalogFilterOptions')
    ? { catalogFilterOptions: filterOptions }
    : { titles: { nodes: variables.after ? [title(), title({ id: other, title: 'Mountain' })] : [title()], pageInfo: variables.after ? end : more } });
  render(Catalog);
  expect(await screen.findByRole('link', { name: /River/ })).toBeTruthy();
  const query = fetcher.mock.calls.map(([, init]) => JSON.parse(String(init?.body))).find((body) => body.query.includes('query Titles'));
  expect(query.variables.filter).toMatchObject({ text: 'River', kind: 'MOVIE', year: 2024, month: 2, genreIds: [third] });
  await fireEvent.click(screen.getByRole('button', { name: 'Next titles page' }));
  expect(await screen.findByRole('link', { name: /Mountain/ })).toBeTruthy();
  expect(screen.getAllByRole('link', { name: /River/ })).toHaveLength(1);
});

it.each([['', 'The catalog is empty.'], ['?q=Missing', 'No matching titles — try changing your filters.']])('distinguishes empty browse from empty search %s', async (query, message) => {
  fixture.page.url = new URL(`https://localhost/${query}`);
  http((operation) => operation.includes('CatalogFilterOptions') ? { catalogFilterOptions: filterOptions } : { titles: { nodes: [], pageInfo: end } });
  render(Catalog);
  expect(await screen.findByText(message)).toBeTruthy();
});

it('catalog recovers repeated unavailable reads through retry and retains filter choices', async () => {
  let failures = 2;
  http((operation) => {
    if (operation.includes('CatalogFilterOptions')) return { catalogFilterOptions: filterOptions };
    if (failures-- > 0) return new Response('{}', { status: 503 });
    return { titles: { nodes: [title()], pageInfo: end } };
  });
  render(Catalog);
  expect(await screen.findByText('The catalog could not load.')).toBeTruthy();
  await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
  expect(await screen.findByRole('link', { name: /River/ })).toBeTruthy();
});
