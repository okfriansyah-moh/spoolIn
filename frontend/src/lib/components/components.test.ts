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
});
