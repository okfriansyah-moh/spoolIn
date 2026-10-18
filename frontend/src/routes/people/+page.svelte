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
    return () => {
      globalThis.clearTimeout(debounceTimer);
      activeController?.abort();
    };
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

  async function load(
    text: string,
    after: string | null = historyCursor(page.state, 'peopleCursor'),
  ): Promise<void> {
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
    void goto(resolve('/people'), {
      replaceState: false,
      state: { ...page.state, peopleCursor: null },
    });
  }

  function nextPage() {
    const cursor = peopleState.pageInfo?.endCursor;
    if (!cursor) return;
    // eslint-disable-next-line svelte/no-navigation-without-resolve -- preserve the current public criteria URL
    void goto(page.url.href, {
      state: { ...page.state, peopleCursor: cursor },
      reset: false,
    });
  }

  function biographyPreview(biography: string): string {
    const codePoints = Array.from(biography);
    return codePoints.length > 120
      ? `${codePoints.slice(0, 120).join('')}…`
      : biography;
  }
</script>

<svelte:head><title>People · SpoolIn</title></svelte:head>

<section class="people-directory">
  <header class="people-heading">
    <h1 tabindex="-1">People</h1>
    <p>Meet the people behind the films and series in the catalog.</p>
  </header>

  <div class="field people-search">
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
    <div class="people-results">
      <ul class="people-list" aria-label="People">
        {#each peopleState.people as person (person.id)}
          <li>
            <article class="person-result">
              <div class="person-summary">
                <a
                  class="person-name"
                  href={resolve('/people/[id]', { id: person.id })}
                >
                  {person.name}
                </a>
              </div>
              <div class="person-biography">
                {#if person.biography}
                  <p class="bio-excerpt">
                    {biographyPreview(person.biography)}
                  </p>
                  {#if Array.from(person.biography).length > 120}
                    <details class="biography-details">
                      <summary>Read full biography</summary>
                      <p>{person.biography}</p>
                    </details>
                  {/if}
                {/if}
              </div>
            </article>
          </li>
        {/each}
      </ul>
    </div>
    {#if peopleState.pageInfo?.hasNextPage}<button
        type="button"
        onclick={nextPage}>Next people page</button
      >{/if}
  {/if}
</section>

<style>
  .people-heading {
    margin-bottom: var(--primitive-space-8);
  }
  .people-heading h1 {
    margin: 0 0 var(--primitive-space-3);
  }
  .people-heading p {
    margin: 0;
    color: var(--muted);
    font-size: 18px;
    max-width: 65ch;
  }
  .people-search {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: end;
    gap: var(--primitive-space-3);
    max-width: 48rem;
    margin-bottom: var(--primitive-space-12);
  }
  .people-search label {
    grid-column: 1 / -1;
  }
  .people-search input {
    min-height: 50px;
  }
  .people-results {
    border-top: 1px solid var(--line);
    margin-bottom: var(--primitive-space-6);
  }
  .people-list {
    margin: 0;
    padding: 0;
    list-style: none;
  }
  .people-list li {
    min-width: 0;
    border-bottom: 1px solid var(--line);
  }
  .person-result {
    display: grid;
    grid-template-columns: minmax(12rem, 1fr) minmax(0, 2fr);
    gap: var(--primitive-space-12);
    padding: var(--primitive-space-6) var(--primitive-space-4);
    align-items: start;
  }
  .person-name {
    display: inline-block;
    color: var(--text);
    font-size: 19px;
    font-weight: 600;
    text-underline-offset: 0.2em;
  }
  .person-biography {
    min-width: 0;
  }
  .bio-excerpt {
    margin: 0;
    color: var(--muted);
    max-width: 70ch;
  }
  .biography-details {
    margin-top: var(--primitive-space-3);
  }
  .biography-details summary {
    color: var(--accent);
    font-size: var(--primitive-type-small);
    text-underline-offset: 0.18em;
    cursor: pointer;
  }
  .biography-details p {
    margin: var(--primitive-space-3) 0 0;
    white-space: pre-wrap;
  }
  .catalog-status {
    padding: var(--primitive-space-8) var(--primitive-space-6);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius);
  }
  @media (max-width: 699px) {
    .people-heading p {
      font-size: 16px;
    }
    .people-search {
      grid-template-columns: minmax(0, 1fr);
      margin-bottom: var(--primitive-space-8);
    }
    .people-search label {
      grid-column: auto;
    }
    .people-search button {
      width: 100%;
    }
    .person-result {
      grid-template-columns: minmax(0, 1fr);
      gap: var(--primitive-space-3);
      padding: var(--primitive-space-6) 0;
    }
    .person-name {
      font-size: 18px;
    }
  }
</style>
