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
  import { readPageInfo, appendDistinct } from '../../../lib/state/pagination';
  import {
    fetchTitle,
    fetchTitleCredits,
  } from '../../../lib/features/catalog/api';
  import { formatReleaseDate } from '../../../lib/features/catalog/filter';
  import ArtworkSlot from '../../../lib/components/ArtworkSlot.svelte';
  import PosterAttribution from '../../../lib/components/PosterAttribution.svelte';
  import type { CatalogTitleDetail } from '../../../lib/features/catalog/api';
  import { isRecordId } from '../../../lib/components/safe-url';

  type State =
    | { kind: 'loading' }
    | { kind: 'unavailable'; id: string }
    | { kind: 'failure' }
    | { kind: 'movie' | 'tv'; title: CatalogTitleDetail };

  let titleState = $state<State>({ kind: 'loading' });

  let mounted = $state(false);
  let active: AbortController | null = null;
  let loadingCredits = $state(false);
  let creditError = $state('');
  onMount(() => {
    mounted = true;
    return () => active?.abort();
  });
  $effect(() => {
    if (mounted) loadTitle(page.params.id);
  });

  function loadTitle(rawId: string | undefined) {
    active?.abort();
    loadingCredits = false;
    creditError = '';
    titleState = { kind: 'loading' };
    if (!isRecordId(rawId)) {
      titleState = { kind: 'unavailable', id: rawId ?? '' };
      return;
    }
    const id = rawId;

    const transport = createTransport();
    const controller = new AbortController();
    active = controller;

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
  }

  async function moreCredits() {
    const current = titleState;
    if (loadingCredits || (current.kind !== 'movie' && current.kind !== 'tv'))
      return;
    const after = current.title.credits.pageInfo.endCursor;
    if (!after || !active) return;
    const signal = active.signal;
    loadingCredits = true;
    creditError = '';
    try {
      const next = await fetchTitleCredits(
        createTransport(),
        current.title.id,
        after,
        signal,
      );
      if (signal.aborted) return;
      titleState = {
        ...current,
        title: {
          ...current.title,
          credits: {
            nodes: [...appendDistinct(current.title.credits.nodes, next.nodes)],
            pageInfo: readPageInfo(next.pageInfo, after),
          },
        },
      };
    } catch {
      if (!signal.aborted)
        creditError = 'More credits could not be loaded. Retry this page.';
    } finally {
      if (!signal.aborted) loadingCredits = false;
    }
  }

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

<div class="title-page">
  <nav class="detail-breadcrumb" aria-label="Breadcrumb">
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
      <button type="button" onclick={() => loadTitle(page.params.id)}
        >Retry title</button
      >
      <a href={resolve('/')}>Return to catalog</a>
    </div>
  {:else if titleState.kind === 'movie'}
    <article
      id="title-detail"
      class="title-detail"
      aria-label={titleState.title.title}
    >
      <div class="title-backdrop" aria-hidden="true">
        <ArtworkSlot src={titleState.title.artwork?.url ?? null} alt="" />
      </div>
      <div class="detail-grid">
        <div id="artwork" class="detail-poster">
          <ArtworkSlot
            src={titleState.title.artwork?.url ?? null}
            alt={titleState.title.title}
          />
          <PosterAttribution title={titleState.title} />
        </div>

        <section
          id="metadata"
          class="title-overview"
          aria-label="Title metadata"
        >
          <div class="meta-line">
            <span class="title-kind">Movie</span><span
              >{formatReleaseDate(titleState.title.release) ??
                'Release date unknown'}</span
            ><span
              >{titleState.title.runtimeMinutes === null
                ? 'Runtime unknown'
                : formatRuntime(titleState.title.runtimeMinutes)}</span
            >
          </div>
          <h1 tabindex="-1">{titleState.title.title}</h1>

          {#if titleState.title.originalTitle}
            <p>
              <strong>Original title:</strong>
              {titleState.title.originalTitle}
            </p>
          {/if}

          <p
            class="synopsis"
            lang={titleState.title.synopsisLanguage || undefined}
          >
            {titleState.title.synopsis || ''}
          </p>

          {#if titleState.title.genres.length > 0}
            <div class="genre-tags" aria-label="Genres">
              {#each titleState.title.genres as genre (genre.id)}<span
                  >{genre.label}</span
                >{/each}
            </div>
          {/if}
          <dl class="detail-metadata">
            <div>
              <dt>Release</dt>
              <dd>
                {formatReleaseDate(titleState.title.release) ??
                  'Release date unknown'}
              </dd>
            </div>
            {#if titleState.title.countries.length > 0}<div>
                <dt>Production country</dt>
                <dd>{titleState.title.countries.join(', ')}</dd>
              </div>{/if}
          </dl>
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
      </div>

      {#if titleState.title.credits.nodes.length > 0}
        <!-- eslint-disable svelte/no-navigation-without-resolve -- dynamic person id segments in credits -->
        <section class="title-credits" aria-label="Credits">
          <h2>Credits</h2>
          <ul class="credit-list">
            {#each titleState.title.credits.nodes as credit (credit.id)}
              <li>
                <span class="person-monogram" aria-hidden="true"
                  >{Array.from(credit.personName)
                    .slice(0, 2)
                    .join('')
                    .toUpperCase()}</span
                >
                <div class="credit-copy">
                  <a
                    href={`${resolve('/')}people/${encodeURIComponent(credit.personId)}`}
                  >
                    {credit.personName}
                  </a>
                  <p>
                    {credit.role.label}
                    {#if credit.characters.length > 0}
                      as {credit.characters.join(', ')}
                    {/if}
                  </p>
                </div>
              </li>
            {/each}
          </ul>
        </section>
      {:else}
        <section class="title-credits" aria-label="Credits">
          <h2>Credits</h2>
          <p>No credits known.</p>
        </section>
      {/if}
    </article>
  {:else if titleState.kind === 'tv'}
    <article
      id="title-detail"
      class="title-detail"
      aria-label={titleState.title.title}
    >
      <div class="title-backdrop" aria-hidden="true">
        <ArtworkSlot src={titleState.title.artwork?.url ?? null} alt="" />
      </div>
      <div class="detail-grid">
        <div id="artwork" class="detail-poster">
          <ArtworkSlot
            src={titleState.title.artwork?.url ?? null}
            alt={titleState.title.title}
          />
          <PosterAttribution title={titleState.title} />
        </div>

        <section
          id="metadata"
          class="title-overview"
          aria-label="Title metadata"
        >
          <div class="meta-line">
            <span class="title-kind">TV Series</span><span
              >{tvReleaseLabel(titleState.title)}</span
            ><span
              >{titleState.title.runtimeMinutes === null
                ? 'Episode runtime unknown'
                : formatRuntime(titleState.title.runtimeMinutes)}</span
            >
          </div>
          <h1 tabindex="-1">{titleState.title.title}</h1>

          {#if titleState.title.originalTitle}
            <p>
              <strong>Original title:</strong>
              {titleState.title.originalTitle}
            </p>
          {/if}

          {#if titleState.title.tvStatus}
            <p><strong>Status:</strong> {titleState.title.tvStatus}</p>
          {/if}

          {#if tvEndLabel(titleState.title)}
            <p>{tvEndLabel(titleState.title)}</p>
          {/if}

          <p
            class="synopsis"
            lang={titleState.title.synopsisLanguage || undefined}
          >
            {titleState.title.synopsis || ''}
          </p>

          {#if titleState.title.genres.length > 0}
            <div class="genre-tags" aria-label="Genres">
              {#each titleState.title.genres as genre (genre.id)}<span
                  >{genre.label}</span
                >{/each}
            </div>
          {/if}
          <dl class="detail-metadata">
            <div>
              <dt>Release</dt>
              <dd>
                {formatReleaseDate(titleState.title.release) ??
                  'Release date unknown'}
              </dd>
            </div>
            {#if titleState.title.countries.length > 0}<div>
                <dt>Production country</dt>
                <dd>{titleState.title.countries.join(', ')}</dd>
              </div>{/if}
          </dl>
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
      </div>

      {#if titleState.title.credits.nodes.length > 0}
        <!-- eslint-disable svelte/no-navigation-without-resolve -- dynamic person id segments in credits -->
        <section class="title-credits" aria-label="Credits">
          <h2>Credits</h2>
          <ul class="credit-list">
            {#each titleState.title.credits.nodes as credit (credit.id)}
              <li>
                <span class="person-monogram" aria-hidden="true"
                  >{Array.from(credit.personName)
                    .slice(0, 2)
                    .join('')
                    .toUpperCase()}</span
                >
                <div class="credit-copy">
                  <a
                    href={`${resolve('/')}people/${encodeURIComponent(credit.personId)}`}
                  >
                    {credit.personName}
                  </a>
                  <p>
                    {credit.role.label}
                    {#if credit.characters.length > 0}
                      as {credit.characters.join(', ')}
                    {/if}
                  </p>
                </div>
              </li>
            {/each}
          </ul>
        </section>
      {:else}
        <section class="title-credits" aria-label="Credits">
          <h2>Credits</h2>
          <p>No credits known.</p>
        </section>
      {/if}
    </article>
  {/if}

  {#if titleState.kind === 'movie' || titleState.kind === 'tv'}
    {#if creditError}<p role="alert">{creditError}</p>{/if}
    {#if titleState.title.credits.pageInfo.hasNextPage}<button
        type="button"
        disabled={loadingCredits}
        onclick={() => void moreCredits()}
      >
        {creditError ? 'Retry more credits' : 'Load more credits'}
      </button>{/if}
  {/if}
</div>

<style>
  .meta-line {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 13px;
    color: var(--muted);
  }
  .genre-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin: 24px 0;
  }
  .genre-tags span {
    background: var(--raised);
    padding: 4px 12px;
    font-size: 13px;
    border-radius: var(--radius);
  }
  .detail-metadata {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    margin: 25px 0;
  }
  .detail-metadata dt {
    font-size: 12px;
    color: var(--muted);
  }
  .detail-metadata dd {
    margin: 3px 0 0;
  }

  .title-page {
    position: relative;
  }
  .detail-breadcrumb {
    margin-bottom: var(--primitive-space-8);
    font-size: var(--primitive-type-small);
  }
  .detail-breadcrumb a {
    color: var(--muted);
  }
  .title-detail {
    position: relative;
    isolation: isolate;
  }
  .title-backdrop {
    position: absolute;
    z-index: -1;
    top: -80px;
    left: -48px;
    right: -48px;
    height: 370px;
    overflow: hidden;
    opacity: 0.18;
    pointer-events: none;
    mask-image: linear-gradient(to bottom, black, transparent);
  }
  .title-backdrop :global(.artwork) {
    height: 100%;
    width: 100%;
    max-width: none;
    aspect-ratio: auto;
    border: 0;
    border-radius: 0;
  }
  .title-backdrop :global(img) {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center 44%;
    filter: blur(12px);
  }
  .title-backdrop :global(.missing-art) {
    display: none;
  }
  .detail-grid {
    display: grid;
    grid-template-columns: 240px minmax(0, 1fr);
    gap: var(--primitive-space-12);
    align-items: start;
  }
  .detail-poster {
    overflow: hidden;
    border-radius: var(--component-artwork-radius);
    box-shadow: var(--primitive-shadow-overlay);
  }
  .detail-poster :global(.artwork) {
    width: 100%;
    aspect-ratio: 2/3;
  }
  .detail-poster :global(img) {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .title-overview {
    min-width: 0;
  }
  .title-overview h1 {
    font-size: clamp(36px, 5vw, 56px);
    margin: var(--primitive-space-4) 0 var(--primitive-space-6);
    max-width: 22ch;
  }
  .title-kind {
    display: inline-flex;
    margin: 0;
    padding: var(--primitive-space-1) var(--primitive-space-3);
    border: 1px solid var(--line);
    border-radius: var(--primitive-radius-pill);
    color: var(--muted);
    font-size: 12px;
  }
  .title-overview > p:not(.title-kind):not(.synopsis) {
    color: var(--muted);
    font-size: 14px;
    margin: 0 0 var(--primitive-space-3);
  }
  .title-overview > p strong {
    color: var(--text);
    font-weight: 500;
  }
  .synopsis {
    font-size: 18px;
    line-height: 1.7;
    margin: var(--primitive-space-6) 0;
    max-width: 66ch;
    white-space: pre-wrap;
  }
  .title-overview > details {
    border-top: 1px solid var(--line);
    padding-top: var(--primitive-space-4);
    margin-top: var(--primitive-space-6);
    font-size: var(--primitive-type-small);
  }
  .title-overview summary {
    cursor: pointer;
    min-height: 32px;
    font-weight: 500;
  }
  .provenance {
    display: grid;
    grid-template-columns: 90px minmax(0, 1fr);
    gap: var(--primitive-space-2) var(--primitive-space-4);
    color: var(--muted);
  }
  .provenance dd {
    margin: 0;
    overflow-wrap: anywhere;
  }
  .title-credits {
    margin: 44px 0 var(--primitive-space-8);
  }
  .title-credits h2 {
    margin: 0 0 var(--primitive-space-6);
  }
  .credit-list {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    list-style: none;
    gap: var(--primitive-space-8) var(--primitive-space-6);
    padding: 0;
    margin: 0;
  }
  .credit-list li {
    display: flex;
    gap: 14px;
    align-items: start;
    min-width: 0;
  }
  .person-monogram {
    display: grid;
    place-items: center;
    flex-shrink: 0;
    width: 55px;
    min-height: 65px;
    border-radius: var(--radius);
    background: var(--raised);
    color: var(--muted);
    font-size: 20px;
  }
  .credit-copy {
    min-width: 0;
  }
  .credit-copy a {
    display: inline-block;
    margin-bottom: var(--primitive-space-2);
    color: var(--text);
    font-size: 17px;
    font-weight: 600;
  }
  .credit-copy p {
    margin: 0;
    color: var(--muted);
    font-size: var(--primitive-type-small);
  }
  .unavailable,
  .failure {
    padding: var(--primitive-space-8);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: var(--surface);
  }
  .failure button {
    margin-right: var(--primitive-space-4);
  }
  @media (max-width: 1100px) {
    .title-backdrop {
      left: -28px;
      right: -28px;
    }
  }
  @media (max-width: 1000px) {
    .detail-grid {
      grid-template-columns: 200px minmax(0, 1fr);
      gap: 28px;
    }
    .credit-list {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
  @media (max-width: 699px) {
    .detail-breadcrumb {
      margin-bottom: var(--primitive-space-6);
    }
    .detail-grid {
      display: block;
    }
    .detail-grid::after {
      content: '';
      display: block;
      clear: both;
    }
    .detail-poster {
      float: left;
      width: 95px;
      margin: var(--primitive-space-2) 20px var(--primitive-space-4) 0;
    }
    .detail-poster :global(.missing-art) {
      padding: 4px;
      font-size: 11px;
    }
    .title-overview h1 {
      font-size: 36px;
      margin-top: var(--primitive-space-3);
      margin-bottom: var(--primitive-space-4);
    }
    .title-overview > p:not(.title-kind):not(.synopsis) {
      font-size: 13px;
    }
    .synopsis {
      clear: both;
      padding-top: var(--primitive-space-4);
      font-size: 16px;
    }
    .title-backdrop {
      left: -18px;
      right: -18px;
      height: 240px;
    }
    .credit-list {
      grid-template-columns: minmax(0, 1fr);
      gap: var(--primitive-space-6);
    }
    .detail-metadata {
      grid-template-columns: 1fr;
      font-size: 14px;
    }
    .meta-line {
      gap: 8px;
      font-size: 12px;
    }
    .provenance {
      grid-template-columns: minmax(0, 1fr);
      gap: var(--primitive-space-1);
    }
    .provenance dd {
      margin-bottom: var(--primitive-space-3);
    }
  }
</style>
