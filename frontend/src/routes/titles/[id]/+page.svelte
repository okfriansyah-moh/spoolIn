<script lang="ts">
  /**
   * T013 – Title detail page (/titles/[id]).
   *
   * States: loading, movie, tv, unavailable, failure
   * ATDD-257 states: movie, tv, unavailable, identities
   *
   * - Shows full metadata, provenance and public credits.
   * - No episode-tracking controls, no direct TV management, no account information.
   * - Unavailable → safe message, no archived data exposed, no same-name substitution.
   * - Missing artwork falls back to ArtworkSlot placeholder.
   */
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../lib/transport/graphql';
  import { fetchTitle } from '../../../lib/features/catalog/api';
  import { formatReleaseDate } from '../../../lib/features/catalog/filter';
  import ArtworkSlot from '../../../lib/components/ArtworkSlot.svelte';
  import type { CatalogTitleDetail } from '../../../lib/features/catalog/api';
  import { isRecordId } from '../../../lib/components/safe-url';

  type State =
    | { kind: 'loading' }
    | { kind: 'unavailable'; id: string }
    | { kind: 'failure' }
    | { kind: 'movie' | 'tv'; title: CatalogTitleDetail };

  let titleState = $state<State>({ kind: 'loading' });

  onMount(() => {
    const rawId = page.params.id;
    if (!isRecordId(rawId)) {
      titleState = { kind: 'unavailable', id: rawId ?? '' };
      return;
    }
    const id = rawId;

    const transport = createTransport();
    const controller = new AbortController();

    fetchTitle(transport, { id }, controller.signal)
      .then((title) => {
        if (controller.signal.aborted) return;
        if (!title || title.state === 'ARCHIVED') {
          titleState = { kind: 'unavailable', id };
          return;
        }
        titleState = { kind: title.kind === 'TV' ? 'tv' : 'movie', title };
      })
      .catch((err) => {
        if (controller.signal.aborted) return;
        void err;
        titleState = { kind: 'failure' };
      });

    return () => controller.abort();
  });

  function formatRuntime(minutes: number | null): string {
    if (minutes === null) return 'Unknown';
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    if (h === 0) return `${m} min`;
    if (m === 0) return `${h} h`;
    return `${h} h ${m} min`;
  }

  function tvReleaseLabel(title: CatalogTitleDetail): string {
    const first = formatReleaseDate(title.release);
    if (!first) return 'First air date unknown';
    return `First aired: ${first}`;
  }

  function tvEndLabel(title: CatalogTitleDetail): string {
    if (!title.tvEnd || title.tvEnd.year === null) return '';
    const label = formatReleaseDate(title.tvEnd);
    return label ? `Ended: ${label}` : '';
  }

  const documentTitle = $derived(
    titleState.kind === 'loading'
      ? 'Loading… · SpoolIn'
      : titleState.kind === 'movie' || titleState.kind === 'tv'
        ? `${titleState.title.title} · SpoolIn`
        : 'Unavailable · SpoolIn',
  );
</script>

<svelte:head>
  <title>{documentTitle}</title>
</svelte:head>

<nav aria-label="Breadcrumb">
  <a href={resolve('/')}>← Catalog</a>
</nav>

{#if titleState.kind === 'loading'}
  <div role="status" aria-live="polite">
    <h1 tabindex="-1">Loading…</h1>
  </div>
{:else if titleState.kind === 'unavailable'}
  <div class="unavailable">
    <h1 tabindex="-1">This title is unavailable</h1>
    <p>The title you requested is no longer available.</p>
    <a href={resolve('/')}>Return to catalog</a>
  </div>
{:else if titleState.kind === 'failure'}
  <div role="alert" class="failure">
    <h1 tabindex="-1">Could not load title</h1>
    <p>The title could not be loaded. Please try again.</p>
    <a href={resolve('/')}>Return to catalog</a>
  </div>
{:else if titleState.kind === 'movie'}
  <article id="title-detail" aria-label={titleState.title.title}>
    <div id="artwork">
      <ArtworkSlot
        src={titleState.title.artwork?.url ?? null}
        alt={titleState.title.title}
      />
      <h1 tabindex="-1">{titleState.title.title}</h1>
    </div>

    <section id="metadata" aria-label="Title metadata">
      <p class="title-kind">Movie</p>

      {#if titleState.title.originalTitle}
        <p><strong>Original title:</strong> {titleState.title.originalTitle}</p>
      {/if}

      <p>
        <strong>Release:</strong>
        {formatReleaseDate(titleState.title.release) ?? 'Release date unknown'}
      </p>

      <p>
        <strong>Runtime:</strong>
        {formatRuntime(titleState.title.runtimeMinutes)}
      </p>

      {#if titleState.title.genres.length > 0}
        <p>
          <strong>Genres:</strong>
          {titleState.title.genres.map((g) => g.label).join(', ')}
        </p>
      {/if}

      {#if titleState.title.countries.length > 0}
        <p>
          <strong>Countries:</strong>
          {titleState.title.countries.join(', ')}
        </p>
      {/if}

      <p class="synopsis" lang={titleState.title.synopsisLanguage || undefined}>
        {titleState.title.synopsis || ''}
      </p>

      <details>
        <summary>Source provenance</summary>
        {#each titleState.title.provenance as prov (prov.source + prov.externalId)}
          <dl class="provenance">
            <dt>Source</dt>
            <dd>{prov.source}</dd>
            <dt>External ID</dt>
            <dd>{prov.externalId}</dd>
            {#if prov.recordUrl}
              <dt>Record</dt>
              <dd>
                <!-- eslint-disable-next-line svelte/no-navigation-without-resolve -- External provenance URL, not an app route. -->
                <a href={prov.recordUrl}>{prov.recordUrl}</a>
              </dd>
            {/if}
            <dt>Retrieved</dt>
            <dd>{prov.retrievedAt}</dd>
            {#if prov.reuseNote}
              <dt>Reuse note</dt>
              <dd>{prov.reuseNote}</dd>
            {/if}
          </dl>
        {/each}
      </details>
    </section>

    {#if titleState.title.credits.nodes.length > 0}
      <!-- eslint-disable svelte/no-navigation-without-resolve -- dynamic person id segments in credits -->
      <section aria-label="Credits">
        <h2>Credits</h2>
        <ul>
          {#each titleState.title.credits.nodes as credit (credit.id)}
            <li>
              <a
                href={`${resolve('/')}people/${encodeURIComponent(credit.personId)}`}
              >
                {credit.personName}
              </a>
              — {credit.role.label}
              {#if credit.characters.length > 0}
                as {credit.characters.join(', ')}
              {/if}
            </li>
          {/each}
        </ul>
      </section>
    {:else}
      <section aria-label="Credits">
        <h2>Credits</h2>
        <p>No credits known.</p>
      </section>
    {/if}
  </article>
{:else if titleState.kind === 'tv'}
  <article id="title-detail" aria-label={titleState.title.title}>
    <div id="artwork">
      <ArtworkSlot
        src={titleState.title.artwork?.url ?? null}
        alt={titleState.title.title}
      />
      <h1 tabindex="-1">{titleState.title.title}</h1>
    </div>

    <section id="metadata" aria-label="Title metadata">
      <p class="title-kind">TV Series</p>

      {#if titleState.title.originalTitle}
        <p><strong>Original title:</strong> {titleState.title.originalTitle}</p>
      {/if}

      <p>{tvReleaseLabel(titleState.title)}</p>

      {#if titleState.title.tvStatus}
        <p><strong>Status:</strong> {titleState.title.tvStatus}</p>
      {/if}

      {#if tvEndLabel(titleState.title)}
        <p>{tvEndLabel(titleState.title)}</p>
      {/if}

      {#if titleState.title.runtimeMinutes !== null}
        <p>
          <strong>Episode runtime:</strong>
          {formatRuntime(titleState.title.runtimeMinutes)}
        </p>
      {:else}
        <p><strong>Episode runtime:</strong> Unknown</p>
      {/if}

      {#if titleState.title.genres.length > 0}
        <p>
          <strong>Genres:</strong>
          {titleState.title.genres.map((g) => g.label).join(', ')}
        </p>
      {/if}

      <p class="synopsis" lang={titleState.title.synopsisLanguage || undefined}>
        {titleState.title.synopsis || ''}
      </p>

      <details>
        <summary>Source provenance</summary>
        {#each titleState.title.provenance as prov (prov.source + prov.externalId)}
          <dl class="provenance">
            <dt>Source</dt>
            <dd>{prov.source}</dd>
            <dt>External ID</dt>
            <dd>{prov.externalId}</dd>
            {#if prov.recordUrl}
              <dt>Record</dt>
              <dd>
                <!-- eslint-disable-next-line svelte/no-navigation-without-resolve -- External provenance URL, not an app route. -->
                <a href={prov.recordUrl}>{prov.recordUrl}</a>
              </dd>
            {/if}
            <dt>Retrieved</dt>
            <dd>{prov.retrievedAt}</dd>
            {#if prov.reuseNote}
              <dt>Reuse note</dt>
              <dd>{prov.reuseNote}</dd>
            {/if}
          </dl>
        {/each}
      </details>
    </section>

    {#if titleState.title.credits.nodes.length > 0}
      <!-- eslint-disable svelte/no-navigation-without-resolve -- dynamic person id segments in credits -->
      <section aria-label="Credits">
        <h2>Credits</h2>
        <ul>
          {#each titleState.title.credits.nodes as credit (credit.id)}
            <li>
              <a
                href={`${resolve('/')}people/${encodeURIComponent(credit.personId)}`}
              >
                {credit.personName}
              </a>
              — {credit.role.label}
              {#if credit.characters.length > 0}
                as {credit.characters.join(', ')}
              {/if}
            </li>
          {/each}
        </ul>
      </section>
    {:else}
      <section aria-label="Credits">
        <h2>Credits</h2>
        <p>No credits known.</p>
      </section>
    {/if}
  </article>
{/if}
