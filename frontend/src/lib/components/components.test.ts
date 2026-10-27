import { expect, it } from 'vitest';
import { render } from 'svelte/server';
import { createRawSnippet } from 'svelte';
import Brand from './Brand.svelte';
import Button from './Button.svelte';
import Field from './Field.svelte';
import ErrorSummary from './ErrorSummary.svelte';
import Notice from './Notice.svelte';
import OperationFeedback from './OperationFeedback.svelte';
import SafeLink from './SafeLink.svelte';
import ArtworkSlot from './ArtworkSlot.svelte';
import PosterAttribution from './PosterAttribution.svelte';
import TitleCard from './TitleCard.svelte';
import ConfirmDialog from './ConfirmDialog.svelte';
import Progress from './Progress.svelte';
import Metadata from './Metadata.svelte';
import ConflictReview from './ConflictReview.svelte';
import ImpactPreview from './ImpactPreview.svelte';
import DataTable from './DataTable.svelte';
import SessionBoundary from './SessionBoundary.svelte';
import ProposalReview from './ProposalReview.svelte';
import CatalogFilters from './CatalogFilters.svelte';
import AppShell from './AppShell.svelte';
const children = createRawSnippet(() => ({
  render: () => '<p>Fixture content</p>',
}));
it('brand keeps full identity and compact marks', () => {
  expect(render(Brand).body).toContain('SpoolIn');
  for (const size of [16, 24, 48] as const)
    expect(render(Brand, { props: { compact: true, size } }).body).toContain(
      `width="${size}"`,
    );
});
it('buttons describe pending and unavailable actions', () => {
  expect(render(Button, { props: { children, pending: true } }).body).toContain(
    'Saving…',
  );
  expect(
    render(Button, {
      props: { children, disabledReason: 'Review first', selected: true },
    }).body,
  ).toContain('Review first');
  expect(render(Button, { props: { children } }).body).toContain(
    'Fixture content',
  );
});
it('button default omits an explicit tab index', () => {
  const body = render(Button, { props: { children } }).body;
  const tag = body.match(/<button\b[^>]*>/)?.[0] ?? '';
  expect(tag).toContain('type="button"');
  expect(tag).not.toContain('tabindex=');
  expect(tag).not.toMatch(/\sdisabled(?:[=\s>])/);
});
it('button forwards explicit zero to an enabled native submit control', () => {
  const body = render(Button, {
    props: { children, type: 'submit', tabindex: 0 },
  }).body;
  const tag = body.match(/<button\b[^>]*>/)?.[0] ?? '';
  expect(tag).toContain('type="submit"');
  expect(tag).toContain('tabindex="0"');
  expect(tag).not.toMatch(/\sdisabled(?:[=\s>])/);
});
it('explicit zero preserves pending and unavailable native disabled state', () => {
  for (const state of [{ pending: true }, { disabledReason: 'Review first' }]) {
    const body = render(Button, {
      props: { children, type: 'submit', tabindex: 0, ...state },
    }).body;
    const tag = body.match(/<button\b[^>]*>/)?.[0] ?? '';
    expect(tag).toContain('tabindex="0"');
    expect(tag).toMatch(/\sdisabled(?:[=\s>])/);
    if (state.pending) {
      expect(tag).toContain('aria-busy="true"');
      expect(body).toContain('Saving…');
    } else {
      expect(tag).toContain('aria-describedby=');
      expect(body).toContain('Review first');
    }
  }
});
it('fields associate exact help and errors with native controls', () => {
  const body = render(Field, {
    props: {
      id: 'title',
      label: 'Title',
      value: '<script>alert(1)</script>',
      help: 'Full title',
      error: 'Enter a title',
      required: true,
    },
  }).body;
  expect(body).toContain('id="title-label"');
  expect(body).toContain('aria-labelledby="title-label"');
  expect(body).toContain('aria-hidden="true"');
  expect(body).toContain(' · Required');
  expect(body).toContain('title-help title-error');
  expect(body).toContain('aria-invalid="true"');
  expect(body).toContain('&lt;script>');
  expect(
    render(Field, {
      props: { id: 'bio', label: 'Biography', multiline: true, disabled: true },
    }).body,
  ).toContain('textarea');
});
it('field-linked error summary remains conditional', () => {
  expect(render(ErrorSummary, { props: { errors: [] } }).body).not.toContain(
    'role="alert"',
  );
  expect(
    render(ErrorSummary, {
      props: { errors: [{ id: 'title', message: 'Enter title' }] },
    }).body,
  ).toContain('href="#title"');
});
it('notices and operation feedback expose exact outcome words', () => {
  expect(
    render(Notice, { props: { title: 'Failed', urgent: true, children } }).body,
  ).toContain('role="alert"');
  expect(render(Notice, { props: { title: 'Checking' } }).body).toContain(
    'role="status"',
  );
  expect(
    render(OperationFeedback, {
      props: { state: { kind: 'uncertain', operationId: 'op-1' } },
    }).body,
  ).toContain('Save result not confirmed');
});
it('safe link and artwork never turn disallowed schemes into attributes', () => {
  expect(
    render(SafeLink, {
      props: {
        text: '<img src=x onerror=alert(1)>',
        url: 'javascript:alert(1)',
      },
    }).body,
  ).not.toContain('<a');
  expect(
    render(SafeLink, { props: { text: 'Source', url: 'https://example.test' } })
      .body,
  ).toContain('href="https://example.test"');
  expect(
    render(ArtworkSlot, { props: { src: 'javascript:alert(1)' } }).body,
  ).toContain('Artwork unavailable');
  expect(
    render(ArtworkSlot, {
      props: {
        src: '/artwork/11111111-1111-4111-8111-111111111111',
        alt: 'Railway',
      },
    }).body,
  ).toContain('alt="Railway"');
});
it('cards preserve title identity and honest date precision', () => {
  const body = render(TitleCard, {
    props: {
      id: '11111111-1111-4111-8111-111111111111',
      title: '李 · Long title',
      kind: 'Movie',
    },
  }).body;
  expect(body).toContain('/titles/11111111-1111-4111-8111-111111111111');
  expect(body).toContain('李 · Long title');
  expect(body).toContain('Release date unknown');
  expect(
    render(TitleCard, {
      props: {
        id: 'id',
        title: 'Title',
        kind: 'TV',
        date: '2025 · month unknown',
      },
    }).body,
  ).toContain('2025 · month unknown');
});
it('dialog contains explicit cancel and consequence action', () => {
  expect(
    render(ConfirmDialog, {
      props: {
        open: false,
        title: 'Delete movie?',
        action: 'Delete movie',
        children,
        onconfirm: () => {},
      },
    }).body,
  ).toContain('Cancel');
  expect(
    render(ConfirmDialog, {
      props: {
        title: 'Delete movie?',
        action: 'Delete movie',
        children,
        onconfirm: () => {},
        pending: true,
      },
    }).body,
  ).toContain('Working…');
});
it('progress separates transfer from processing and attachment', () => {
  for (const stage of ['loading', 'processing'] as const)
    expect(render(Progress, { props: { stage } }).body).toContain(
      'role="status"',
    );
  for (const value of [-1, 63, 100, 101, NaN])
    expect(
      render(Progress, { props: { stage: 'transfer', value } }).body,
    ).toContain('<progress');
  expect(render(Progress, { props: { stage: 'transfer' } }).body).not.toContain(
    'NaN',
  );
  expect(
    render(Progress, { props: { stage: 'transfer', value: 100 } }).body,
  ).toContain('Waiting for processing and attachment');
});
it('metadata distinguishes zero and unknown; provenance is disclosed', () => {
  const body = render(Metadata, {
    props: {
      values: [
        { label: 'Count', value: 0 },
        { label: 'Runtime', value: null },
      ],
      source: 'Local fixture',
    },
  }).body;
  expect(body).toContain('>0<');
  expect(body).toContain('Unknown');
  expect(body).toContain('Source context');
  expect(render(Metadata, { props: { values: [] } }).body).not.toContain(
    'details',
  );
});
it('conflict and impact retain current values without force submission', () => {
  expect(
    render(ConflictReview, {
      props: { current: 'Current', draft: 'Draft', children },
    }).body,
  ).toContain('Your retained draft');
  expect(
    render(ConflictReview, { props: { current: 'Current', draft: 'Draft' } })
      .body,
  ).toContain('Current');
  expect(
    render(ImpactPreview, {
      props: {
        target: 'Person',
        activeMovies: 2,
        archivedMovies: 1,
        tv: 0,
        changed: true,
      },
    }).body,
  ).toContain('impact changed');
  expect(
    render(ImpactPreview, {
      props: { target: 'Person', activeMovies: 0, archivedMovies: 0, tv: 0 },
    }).body,
  ).not.toContain('role="alert"');
});
it('history preserves stable IDs and table headings', () => {
  const body = render(DataTable, {
    props: {
      caption: 'History',
      rows: [{ id: 'stable-id', context: 'Movie', outcome: 'APPROVED' }],
    },
  }).body;
  expect(body).toContain('scope="row"');
  expect(body).toContain('stable-id');
  expect(body).toContain('APPROVED');
});
it('session boundary conceals children until verified', () => {
  for (const state of ['checking', 'expired', 'unavailable', 'denied'] as const)
    expect(
      render(SessionBoundary, { props: { state, children } }).body,
    ).not.toContain('Fixture content');
  expect(
    render(SessionBoundary, { props: { state: 'verified', children } }).body,
  ).toContain('Fixture content');
});
it('proposal remains a revision-specific read-only view', () => {
  for (const state of [
    'review',
    'approved',
    'rejected',
    'uncertain',
  ] as const) {
    const body = render(ProposalReview, {
      props: {
        revision: 'r1',
        values: [
          { label: 'Title', value: 'Film' },
          { label: 'Source', value: null },
        ],
        state,
      },
    }).body;
    expect(body).toContain('Revision: r1');
    expect(body).toContain('Unknown');
    expect(body).not.toContain('<button');
  }
});
it('filters use native choices and disable month without year', () => {
  const props = {
    criteria: { text: '', kind: '', genres: [], year: '', month: '' },
    genres: [{ id: 'drama', name: 'Drama' }],
    years: ['2025'],
  };
  expect(render(CatalogFilters, { props }).body).toContain(
    'Choose a year first',
  );
  expect(
    render(CatalogFilters, {
      props: {
        ...props,
        criteria: { ...props.criteria, year: '2025', month: '10' },
      },
    }).body,
  ).toContain('October');
});
it('shell defaults to public navigation with no demo role selection', () => {
  const body = render(AppShell, { props: { children } }).body;
  expect(body).toContain('Skip to content');
  expect(body).toContain('Switch theme');
  expect(body).not.toContain('/manage/archives');
  expect(
    render(AppShell, {
      props: {
        children,
        capabilities: { verified: true, manageCatalog: true },
        currentPath: '/manage/movies',
      },
    }).body,
  ).toContain('aria-current="page"');
  expect(
    render(AppShell, {
      props: {
        children,
        capabilities: { verified: true, manageCatalog: true },
        currentPath: '/manage/movies/new',
      },
    }).body,
  ).toMatch(/href="\/manage\/movies"[^>]*aria-current="page"/);
});

it('large genre collections retain every native choice and selection inside a labelled region', () => {
  const genres = Array.from({ length: 120 }, (_, index) => ({
    id: `genre-${index}`,
    name: `Genre ${index}`,
  }));
  const body = render(CatalogFilters, {
    props: {
      criteria: {
        text: '',
        kind: '',
        genres: ['genre-119'],
        year: '',
        month: '',
      },
      genres,
      years: ['2025'],
    },
  }).body;
  expect(body).toContain('role="region"');
  expect(body).toContain('aria-label="Genre choices"');
  expect(body).toContain(
    'Genres with available titles are listed. Scroll or search by name.',
  );
  expect(body.match(/type="checkbox"/g)).toHaveLength(120);
  for (const genre of genres) expect(body).toContain(`value="${genre.id}"`);
  expect(
    body.match(/<input\b[^>]*type="checkbox"[^>]* checked\b/g),
  ).toHaveLength(1);
});

it('marks the catalog as current on a title detail route', () => {
  const body = render(AppShell, {
    props: {
      children,
      currentPath: '/titles/seeded-title-id',
    },
  }).body;
  expect(body).toMatch(/href="\/" aria-current="page">Catalog/);
});

it('poster attribution follows the selected asset and hides removed artwork', () => {
  const provenance = [
    {
      source: 'poster',
      externalId: 'asset-one',
      recordUrl: 'https://www.themoviedb.org/movie/1',
      reuseNote: 'Poster source: TMDB',
      retrievedAt: '2026-10-18T00:00:00Z',
      fieldSources: {},
      locallyChanged: [],
    },
  ];
  const artwork = {
    assetId: 'asset-one',
    url: '/artwork/asset-one',
    width: 500,
    height: 750,
  };
  const body = render(PosterAttribution, {
    props: { title: { artwork, provenance } },
  }).body;
  expect(body).toContain('Poster source: TMDB');
  expect(body).toContain('https://www.themoviedb.org/movie/1');
  expect(
    render(PosterAttribution, {
      props: { title: { artwork: null, provenance } },
    }).body,
  ).not.toContain('<a');
  expect(
    render(PosterAttribution, {
      props: {
        title: { artwork: { ...artwork, assetId: 'user-upload' }, provenance },
      },
    }).body,
  ).not.toContain('<a');
});
it('poster attribution rejects unsafe source links', () => {
  const artwork = {
    assetId: 'asset-one',
    url: '/artwork/asset-one',
    width: 500,
    height: 750,
  };
  const provenance = [
    {
      source: 'poster',
      externalId: 'asset-one',
      recordUrl: 'javascript:alert(1)',
      reuseNote: 'Poster source: TMDB',
      retrievedAt: '2026-10-18T00:00:00Z',
      fieldSources: {},
      locallyChanged: [],
    },
  ];
  expect(
    render(PosterAttribution, { props: { title: { artwork, provenance } } })
      .body,
  ).not.toContain('<a');
});
