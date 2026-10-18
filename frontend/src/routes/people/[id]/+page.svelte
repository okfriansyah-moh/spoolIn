<script lang="ts">
  /**
   * T013 – Public person detail page (/people/[id]).
   *
   * Shows person name, biography (if available) and their public credits.
   * Unavailable (archived/deleted) → safe message, no archived data.
   * AC-022-C01/AC-004: only active people shown; same names never collapse identity.
   */
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../lib/transport/graphql';
  import {
    fetchPerson,
    fetchPersonCredits,
  } from '../../../lib/features/person/read/api';
  import {
    appendDistinct,
    readPageInfo,
    type PageInfo,
  } from '../../../lib/state/pagination';
  import { isRecordId } from '../../../lib/components/safe-url';
  import type {
    PublicPersonDetail,
    PublicPersonCredit,
  } from '../../../lib/features/person/read/api';

  type State =
    | { kind: 'loading' }
    | { kind: 'unavailable'; id: string }
    | { kind: 'failure' }
    | {
        kind: 'person';
        person: PublicPersonDetail;
        credits: PublicPersonCredit[];
        pageInfo: PageInfo;
      };

  let pageState = $state<State>({ kind: 'loading' });

  let mounted = $state(false);
  let active: AbortController | null = null;
  let loadingCredits = $state(false);
  let creditError = $state('');
  onMount(() => {
    mounted = true;
    return () => active?.abort();
  });
  $effect(() => {
    if (mounted) loadPerson(page.params.id);
  });

  function loadPerson(id: string | undefined) {
    active?.abort();
    loadingCredits = false;
    creditError = '';
    pageState = { kind: 'loading' };
    if (!isRecordId(id)) {
      pageState = { kind: 'unavailable', id: id ?? '' };
      return;
    }

    const transport = createTransport();
    const controller = new AbortController();
    active = controller;

    Promise.all([
      fetchPerson(transport, { id }, controller.signal),
      fetchPersonCredits(transport, { id }, controller.signal),
    ])
      .then(([person, creditsResult]) => {
        if (controller.signal.aborted) return;
        // Public API returns null for archived/unknown people
        if (!person || person.state === 'ARCHIVED') {
          pageState = { kind: 'unavailable', id: id ?? '' };
          return;
        }
        pageState = {
          kind: 'person',
          person,
          credits: [...creditsResult.nodes],
          pageInfo: readPageInfo(creditsResult.pageInfo),
        };
      })
      .catch((err) => {
        if (controller.signal.aborted) return;
        void err;
        pageState = { kind: 'failure' };
      });
  }

  async function moreCredits() {
    const current = pageState;
    if (current.kind !== 'person' || loadingCredits || !active) return;
    const after = current.pageInfo.endCursor;
    if (!after) return;
    const signal = active.signal;
    loadingCredits = true;
    creditError = '';
    try {
      const next = await fetchPersonCredits(
        createTransport(),
        { id: current.person.id, after },
        signal,
      );
      if (signal.aborted) return;
      pageState = {
        ...current,
        credits: [...appendDistinct(current.credits, next.nodes)],
        pageInfo: readPageInfo(next.pageInfo, after),
      };
    } catch {
      if (!signal.aborted)
        creditError = 'More credits could not be loaded. Retry this page.';
    } finally {
      if (!signal.aborted) loadingCredits = false;
    }
  }

  const documentTitle = $derived(
    pageState.kind === 'person'
      ? `${pageState.person.name} · SpoolIn`
      : pageState.kind === 'loading'
        ? 'Loading… · SpoolIn'
        : 'Unavailable · SpoolIn',
  );
</script>

<svelte:head>
  <title>{documentTitle}</title>
</svelte:head>

<nav aria-label="Breadcrumb">
  <a href={resolve('/')}>Catalog</a> /
  <a href={resolve('/people')}>People</a>
</nav>

{#if pageState.kind === 'loading'}
  <div role="status" aria-live="polite">
    <h1 tabindex="-1">Loading…</h1>
  </div>
{:else if pageState.kind === 'unavailable'}
  <div class="unavailable">
    <h1 tabindex="-1">This person is unavailable</h1>
    <p>The person you requested is no longer available.</p>
    <a href={resolve('/people')}>Return to people</a>
  </div>
{:else if pageState.kind === 'failure'}
  <div role="alert" class="failure">
    <h1 tabindex="-1">Could not load person</h1>
    <p>The person could not be loaded. Please try again.</p>
    <button type="button" onclick={() => loadPerson(page.params.id)}
      >Retry person</button
    >
    <a href={resolve('/people')}>Return to people</a>
  </div>
{:else if pageState.kind === 'person'}
  <article id="person-detail" aria-label={pageState.person.name}>
    <h1 tabindex="-1">{pageState.person.name}</h1>

    {#if pageState.person.biography}
      <section aria-label="Biography">
        <h2>Biography</h2>
        <p>{pageState.person.biography}</p>
      </section>
    {/if}

    <section aria-label="Credits">
      <h2>Credits</h2>
      {#if pageState.credits.length === 0}
        <p>No credits known.</p>
      {:else}
        <ul>
          {#each pageState.credits as credit (credit.id)}
            <li>
              <!-- eslint-disable-next-line svelte/no-navigation-without-resolve -- dynamic title id segment -->
              <a
                href={`${resolve('/')}titles/${encodeURIComponent(credit.titleId)}`}
              >
                {credit.titleName}
              </a>
              — {credit.role.label}
              {#if credit.characters.length > 0}
                as {credit.characters.join(', ')}
              {/if}
            </li>
          {/each}
        </ul>
      {/if}
      {#if creditError}<p role="alert">{creditError}</p>{/if}
      {#if pageState.pageInfo.hasNextPage}<button
          type="button"
          disabled={loadingCredits}
          onclick={() => void moreCredits()}
        >
          {creditError ? 'Retry more credits' : 'Load more credits'}
        </button>{/if}
    </section>
  </article>
{/if}
