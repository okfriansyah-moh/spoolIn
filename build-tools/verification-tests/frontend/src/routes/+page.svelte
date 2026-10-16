<script lang="ts">
  import { resolve } from '$app/paths';
  import Brand from '../../../../../frontend/src/lib/components/Brand.svelte';
  import Button from '../../../../../frontend/src/lib/components/Button.svelte';
  import Field from '../../../../../frontend/src/lib/components/Field.svelte';
  import ErrorSummary from '../../../../../frontend/src/lib/components/ErrorSummary.svelte';
  import Notice from '../../../../../frontend/src/lib/components/Notice.svelte';
  import OperationFeedback from '../../../../../frontend/src/lib/components/OperationFeedback.svelte';
  import ArtworkSlot from '../../../../../frontend/src/lib/components/ArtworkSlot.svelte';
  import TitleCard from '../../../../../frontend/src/lib/components/TitleCard.svelte';
  import ConfirmDialog from '../../../../../frontend/src/lib/components/ConfirmDialog.svelte';
  import Progress from '../../../../../frontend/src/lib/components/Progress.svelte';
  import Metadata from '../../../../../frontend/src/lib/components/Metadata.svelte';
  import ConflictReview from '../../../../../frontend/src/lib/components/ConflictReview.svelte';
  import ImpactPreview from '../../../../../frontend/src/lib/components/ImpactPreview.svelte';
  import DataTable from '../../../../../frontend/src/lib/components/DataTable.svelte';
  import SessionBoundary from '../../../../../frontend/src/lib/components/SessionBoundary.svelte';
  import ProposalReview from '../../../../../frontend/src/lib/components/ProposalReview.svelte';
  import CatalogFilters from '../../../../../frontend/src/lib/components/CatalogFilters.svelte';
  import SafeLink from '../../../../../frontend/src/lib/components/SafeLink.svelte';
  import { navigation } from '../../../../../frontend/src/lib/state/navigation';
  let title = $state('The Last Light');
  let synopsis = $state(
    'A coastal town waits for its final train.\nAn archivist returns home.',
  );
  let errors = $state<{ id: string; message: string }[]>([]);
  let criteria = $state({
    text: '',
    kind: '',
    genres: ['drama'],
    year: '',
    month: '',
  });
  let open = $state(false);
  let confirmed = $state(0);
  let pending = $state(false);
  let selected = $state(true);
  let sourceText = $state('李 · O’Connor · é');
  let sourceUrl = $state('https://example.test/source');
  function validate(event: SubmitEvent) {
    event.preventDefault();
    errors = title.trim()
      ? []
      : [{ id: 'specimen-title', message: 'Enter a movie title.' }];
  }
</script>

<svelte:head><title>Shared component specimens · SpoolIn</title></svelte:head>
<h1 tabindex="-1">One component set.</h1>
<p class="lead">
  Shared component specimens. Isolated fixtures; no business writes.
</p>
<p><a href={resolve('/next')}>Navigation fixture</a></p>
<div class="component-grid">
  <section class="panel" id="identity">
    <h2>Wordmark and browser mark</h2>
    <Brand />
    <div class="actions">
      <Brand compact size={16} /><Brand compact size={24} /><Brand
        compact
        size={48}
      />
    </div>
  </section>
  <section class="panel" id="typography">
    <h2>Type in real content</h2>
    <h3>The Space Between Every Place We Have Ever Called Home</h3>
    <p>V · Aruna Sari · 李 · O’Connor · é</p>
    <p>
      After fifteen years away, a railway archivist returns to her coastal
      hometown to record its last overnight train.
    </p>
  </section>
  <section class="panel" id="buttons">
    <h2>Buttons and links</h2>
    <div class="actions">
      <Button {pending} onclick={() => (pending = true)}>Save changes</Button
      ><Button variant="secondary" onclick={() => (pending = false)}
        >Cancel</Button
      ><Button variant="danger" onclick={() => (open = true)}
        >Delete archived movie</Button
      ><Button disabledReason="Review the newer saved version before saving."
        >Save draft</Button
      ><Button pending={true}>Save changes</Button><Button
        {selected}
        onclick={() => (selected = !selected)}>Selected</Button
      >
    </div>
    <OperationFeedback
      state={pending
        ? { kind: 'pending', stage: 'Saving changes' }
        : { kind: 'confirmed', recordId: 'fixture-movie-1' }}
    />
  </section>
  <section class="panel" id="fields">
    <h2>Field anatomy and error</h2>
    <Field
      id="specimen-disabled"
      label="Saved title"
      value="The Last Light"
      disabled
      help="Review the current version before editing."
    />
    <form onsubmit={validate} novalidate>
      <ErrorSummary {errors} /><Field
        id="specimen-title"
        label="Title"
        bind:value={title}
        required
        help="Use the full title. Do not truncate retained input."
        error={errors[0]?.message ?? ''}
      /><Field
        id="specimen-synopsis"
        label="Synopsis"
        multiline
        bind:value={synopsis}
      /><Button type="submit">Validate fields</Button>
    </form>
  </section>
  <section class="panel" id="filters">
    <h2>Selection and date controls</h2>
    <CatalogFilters
      bind:criteria
      genres={[
        { id: 'drama', name: 'Drama' },
        { id: 'documentary', name: 'Documentary' },
      ]}
      years={['2025', '2026']}
    /><output aria-label="Current filters">{JSON.stringify(criteria)}</output>
  </section>
  <section class="panel" id="feedback">
    <h2>Consequential feedback</h2>
    <OperationFeedback
      state={{ kind: 'confirmed', recordId: 'fixture-movie-1' }}
    /><OperationFeedback
      state={{
        kind: 'failed',
        correction: 'Your permitted draft remains. Correct the title.',
      }}
    /><OperationFeedback
      state={{ kind: 'uncertain', operationId: 'fixture-operation-1' }}
    /><OperationFeedback
      state={{
        kind: 'denied',
        reason: 'Your access changed. Review your session.',
      }}
    /><Notice title="Loading current results"
      ><p>Please wait for this search.</p></Notice
    >
  </section>
  <section class="panel" id="progress">
    <h2>Loading and upload stages</h2>
    <Progress stage="loading" /><Progress
      stage="transfer"
      value={63}
    /><Progress stage="transfer" value={100} /><Progress stage="processing" />
  </section>
  <section class="panel" id="artwork">
    <h2>Poster and missing artwork</h2>
    <ArtworkSlot
      src="/artwork/33333333-3333-4333-8333-333333333333"
      alt="A railway platform at dusk"
    />
    <TitleCard
      id="11111111-1111-4111-8111-111111111111"
      title="The Space Between Every Place We Have Ever Called Home · 李"
      kind="Movie"
    /><ArtworkSlot
      src="/artwork/22222222-2222-4222-8222-222222222222"
      alt="An empty railway platform"
    />
  </section>
  <section class="panel" id="metadata">
    <h2>Navigation and metadata</h2>
    <Metadata
      values={[
        { label: 'Original release', value: 'October 2025 · day unknown' },
        { label: 'Runtime', value: null },
        { label: 'Credit count', value: 0 },
      ]}
      source="local/movie-last-light · fixture retrieved 10 October 2026"
    />
    <nav aria-label="Verified Maintainer navigation fixture">
      {#each navigation( { verified: true, manageCatalog: true } ) as item (item.href)}
        <!-- eslint-disable-next-line svelte/no-navigation-without-resolve -- Specimen renders declared downstream destinations before their route tasks. -->
        <a href={`${resolve('/')}${item.href.slice(1)}`}>{item.label}</a>{/each}
    </nav>
  </section>
  <section class="panel" id="dialog">
    <h2>Confirmation dialog</h2>
    <Button variant="danger" onclick={() => (open = true)}
      >Open irreversible confirmation</Button
    >
    <p role="status">Confirmed actions: {confirmed}</p>
    <ConfirmDialog
      bind:open
      title="Permanently delete The Last Light?"
      action="Permanently delete movie"
      onconfirm={() => {
        confirmed += 1;
        open = false;
      }}
      ><p>
        This archived movie and its remaining credits will be removed. Shared
        people and other titles remain. This cannot be undone.
      </p></ConfirmDialog
    >
  </section>
  <section id="conflict">
    <ConflictReview current="A newer saved synopsis." draft={synopsis}
      ><Button
        disabledReason="Review the newer version and check access before saving."
        >Save changes</Button
      ></ConflictReview
    >
  </section>
  <section id="impact">
    <ImpactPreview
      target="Aruna Sari"
      activeMovies={2}
      archivedMovies={1}
      tv={1}
      changed
    />
  </section>
  <section class="panel" id="history">
    <h2>Read-only history</h2>
    <DataTable
      caption="Catalog events"
      rows={[
        {
          id: '11111111-1111-4111-8111-111111111111',
          context: 'The Last Light · Movie',
          outcome: 'Approval recorded; creation not confirmed',
        },
      ]}
    />
  </section>
  <section class="panel" id="session">
    <h2>Session boundary</h2>
    <SessionBoundary state="checking"
      ><p>Private content must be concealed</p></SessionBoundary
    ><SessionBoundary state="expired"
      ><p>Private content must be concealed</p></SessionBoundary
    ><SessionBoundary state="unavailable"
      ><p>Private content must be concealed</p></SessionBoundary
    ><SessionBoundary state="denied"
      ><p>Private content must be concealed</p></SessionBoundary
    >
  </section>
  <section id="proposal">
    <ProposalReview
      revision="fixture-revision-1"
      values={[
        { label: 'Title', value: 'The Last Light' },
        { label: 'Source', value: null },
      ]}
      state="approved"
    />
  </section>
  <section class="panel" id="safety">
    <h2>Safe text and URLs</h2>
    <Field
      id="source-text"
      label="Untrusted text"
      bind:value={sourceText}
    /><Field id="source-url" label="Untrusted URL" bind:value={sourceUrl} />
    <p data-testid="safe-text" class="preserve">{sourceText}</p>
    <div data-testid="safe-link">
      <SafeLink url={sourceUrl} text={sourceText} />
    </div>
    <div data-testid="safe-art"><ArtworkSlot src={sourceUrl} /></div>
  </section>
</div>
