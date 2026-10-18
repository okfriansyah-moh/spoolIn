<script lang="ts">
  /**
   * T013 – Public people listing page (/people).
   *
   * States: loading, people, empty, no-match, failure
   * AC-022: active people only; same names never collapse identity; distinct states.
   */
  import { onMount } from 'svelte';
  import { resolve } from '$app/paths';
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { createTransport } from '../../lib/transport/graphql';
  import { fetchPeople } from '../../lib/features/person/read/api';
  import type { PeopleState } from '../../lib/features/person/read/types';
  import { readPageInfo, historyCursor } from '../../lib/state/pagination';

  let peopleState = $state<PeopleState>({
    kind: 'loading',
    text: '',
    people: [],
    pageInfo: null,
  });

  let searchText = $state('');
  let debounceTimer: ReturnType<typeof globalThis.setTimeout> | undefined;
  let generation = 0;
  let activeController: AbortController | null = null;

  let mounted = $state(false);
  let requested = '';
  onMount(() => {
    mounted = true;
    return () => { globalThis.clearTimeout(debounceTimer); activeController?.abort(); };
  });
  $effect(() => {
    if (!mounted) return;
    const text = page.url.searchParams.get('q') ?? '';
    const after = historyCursor(page.state, 'peopleCursor');
    const key = JSON.stringify([text, after]);
    if (key === requested) return;
    requested = key;
    searchText = text;
    void load(text, after);
  });

  async function load(text: string, after: string | null = historyCursor(page.state, 'peopleCursor')): Promise<void> {
    const gen = ++generation;
    activeController?.abort();
    activeController = new AbortController();
    const signal = activeController.signal;

    peopleState = { kind: 'loading', text, people: [], pageInfo: null };
    try {
      const result = await fetchPeople(
        createTransport(),
        { ...(text ? { text } : {}), after },
        signal,
      );
      if (gen !== generation) return;
      const people = result.nodes.map((p) => ({
        id: p.id,
        name: p.name,
        biography: p.biography,
      }));
      const isEmpty = people.length === 0;
      peopleState = {
        kind: isEmpty ? (text ? 'no-match' : 'empty') : 'people',
        text,
        people,
        pageInfo: readPageInfo(result.pageInfo, after),
      };
    } catch (err) {
      if (gen !== generation) return;
      void err;
      peopleState = { kind: 'failure', text, people: [], pageInfo: null };
    }
  }

  function onInput(event: Event) {
    const value = (event.currentTarget as HTMLInputElement).value;
    searchText = value;
    globalThis.clearTimeout(debounceTimer);
    debounceTimer = globalThis.setTimeout(() => {
      /* eslint-disable svelte/no-navigation-without-resolve -- preserve ?q= on the people route */
      void goto(
        value
          ? `${resolve('/people')}?q=${encodeURIComponent(value)}`
          : resolve('/people'),
        { replaceState: false, state: { ...page.state, peopleCursor: null } },
      );
      /* eslint-enable svelte/no-navigation-without-resolve */
    }, 250);
  }

  function clearSearch() {
    globalThis.clearTimeout(debounceTimer);
    searchText = '';
    void goto(resolve('/people'), { replaceState: false, state: { ...page.state, peopleCursor: null } });
  }

  function nextPage() {
    const cursor = peopleState.pageInfo?.endCursor;
    if (!cursor) return;
    // eslint-disable-next-line svelte/no-navigation-without-resolve -- preserve the current public criteria URL
    void goto(page.url.href, { state: { ...page.state, peopleCursor: cursor }, reset: false });
  }
</script>

<svelte:head><title>People · SpoolIn</title></svelte:head>

<h1 tabindex="-1">People</h1>

<div class="field">
  <label for="people-search">Search people</label>
  <input
    id="people-search"
    type="search"
    bind:value={searchText}
    oninput={onInput}
  />
  <button class="secondary" onclick={clearSearch}>Clear search text</button>
</div>

{#if peopleState.kind === 'loading'}
  <div role="status" aria-live="polite" class="catalog-status">
    <p>Finding people…</p>
  </div>
{:else if peopleState.kind === 'failure'}
  <div role="alert" class="catalog-status catalog-failure">
    <p>People could not be loaded.</p>
    <button onclick={() => void load(searchText)}>Try again</button>
  </div>
{:else if peopleState.kind === 'empty'}
  <div class="catalog-status catalog-empty">
    <p>No people have been added yet.</p>
  </div>
{:else if peopleState.kind === 'no-match'}
  <div class="catalog-status catalog-no-match">
    <p>No people match your search.</p>
  </div>
{:else if peopleState.kind === 'people'}
  <ul class="people-list" aria-label="People">
    {#each peopleState.people as person (person.id)}
      <li>
        <!-- eslint-disable-next-line svelte/no-navigation-without-resolve -- dynamic person id segment -->
        <a href={`${resolve('/')}people/${encodeURIComponent(person.id)}`}>
          {person.name}
        </a>
        {#if person.biography}
          <p class="bio-excerpt">
            {person.biography.slice(0, 120)}{person.biography.length > 120
              ? '…'
              : ''}
          </p>
        {/if}
      </li>
    {/each}
  </ul>
  {#if peopleState.pageInfo?.hasNextPage}<button type="button" onclick={nextPage}>Next people page</button>{/if}
{/if}
