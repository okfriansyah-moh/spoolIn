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

<div class="person-page">
  <nav class="detail-breadcrumb" aria-label="Breadcrumb">
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
      <header class="person-heading">
        <h1 tabindex="-1">{pageState.person.name}</h1>
      </header>

      {#if pageState.person.biography}
        <section class="person-biography" aria-label="Biography">
          <h2>Biography</h2>
          <p>{pageState.person.biography}</p>
        </section>
      {/if}

      <section class="person-credits" aria-label="Credits">
        <h2>Credits</h2>
        {#if pageState.credits.length === 0}
          <p>No credits known.</p>
        {:else}
          <ul class="person-credit-list">
            {#each pageState.credits as credit (credit.id)}
              <li>
                <a href={resolve('/titles/[id]', { id: credit.titleId })}>
                  {credit.titleName}
                </a>
                <div class="credit-contribution">
                  {credit.role.label}
                  {#if credit.characters.length > 0}
                    as {credit.characters.join(', ')}
                  {/if}
                </div>
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
</div>

<style>
  .detail-breadcrumb {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-3);
    align-items: center;
    margin-bottom: var(--primitive-space-8);
    color: var(--muted);
    font-size: var(--primitive-type-small);
  }
  .detail-breadcrumb a {
    color: var(--muted);
  }
  .person-heading {
    margin-bottom: var(--primitive-space-8);
    max-width: 55rem;
  }
  .person-heading h1 {
    margin: 0;
    font-size: clamp(36px, 5vw, 56px);
  }
  .person-biography {
    max-width: 50rem;
    margin-bottom: var(--primitive-space-12);
  }
  .person-biography h2 {
    font-size: 22px;
    margin: 0 0 var(--primitive-space-4);
  }
  .person-biography p {
    font-size: 18px;
    line-height: 1.75;
    white-space: pre-wrap;
    margin: 0;
    color: var(--muted);
  }
  .person-credits {
    margin-top: var(--primitive-space-12);
  }
  .person-credits h2 {
    margin-bottom: var(--primitive-space-6);
  }
  .person-credit-list {
    list-style: none;
    margin: 0 0 var(--primitive-space-6);
    padding: 0;
    border-top: 1px solid var(--line);
  }
  .person-credit-list li {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: var(--primitive-space-6);
    padding: var(--primitive-space-6) var(--primitive-space-4);
    border-bottom: 1px solid var(--line);
  }
  .person-credit-list a {
    color: var(--text);
    font-weight: 600;
    font-size: 18px;
  }
  .credit-contribution {
    color: var(--muted);
  }
  .unavailable,
  .failure {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    padding: var(--primitive-space-8);
  }
  .failure button {
    margin-right: var(--primitive-space-4);
  }
  @media (max-width: 699px) {
    .person-heading {
      margin-bottom: var(--primitive-space-6);
    }
    .person-biography p {
      font-size: 16px;
    }
    .person-credit-list li {
      grid-template-columns: minmax(0, 1fr);
      gap: var(--primitive-space-2);
      padding: var(--primitive-space-6) 0;
    }
  }
</style>
