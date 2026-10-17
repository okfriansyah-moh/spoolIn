<script lang="ts">
  /**
   * T013 – Discovery page (root route).
   *
   * States: catalog, loading, empty, no-match, failure, outdated, invalid
   * URL state: q, kind, genre (repeated), year, month
   * Debounce: 250ms on text changes; non-text filter changes are immediate.
   * Generation-bound: stale replies are suppressed in the controller.
   *
   * SSR note: hero/head always rendered; interactive sections are client-side only.
   */
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { resolve } from '$app/paths';
  import { page } from '$app/state';
  import { createTransport } from '../lib/transport/graphql';
  import { CatalogController } from '../lib/features/catalog/controller';
  import {
    parseCriteria,
    criteriaToParams,
    validateText,
  } from '../lib/features/catalog/filter';
  import {
    EMPTY_CRITERIA,
    MAX_SEARCH_CODE_POINTS,
  } from '../lib/features/catalog/types';
  import type { Criteria, CatalogState } from '../lib/features/catalog/types';
  import { fetchGenres } from '../lib/features/catalog/api';
  import CatalogFilters from '../lib/components/CatalogFilters.svelte';
  import TitleCard from '../lib/components/TitleCard.svelte';

  let mounted = $state(false);
  let genres = $state<Array<{ id: string; name: string }>>([]);
  let years = $state<string[]>(
    Array.from({ length: 10 }, (_, i) => String(new Date().getFullYear() - i)),
  );
  let catalogState = $state<CatalogState>({
    kind: 'loading',
    criteria: EMPTY_CRITERIA,
    titles: [],
    pageInfo: null,
    invalidField: null,
  });

  // Criteria state: synced from URL on load and updated by filters.
  let criteria = $state<Criteria>({ ...EMPTY_CRITERIA });

  let controller: CatalogController | null = null;

  // Non-reactive: track last criteria dispatched to controller to avoid duplicate sends.
  let lastDispatched: Criteria = EMPTY_CRITERIA;
  // Non-reactive: debounce timer for text changes.
  let textDebounceTimer: ReturnType<typeof globalThis.setTimeout> | undefined;
  // Non-reactive: guard to prevent URL sync loop.
  let applyingUrlCriteria = false;

  const filterTextErrorId = 'catalog-text-error';

  onMount(() => {
    mounted = true;
    const transport = createTransport();
    controller = new CatalogController(transport);

    const unsubscribe = controller.subscribe((state) => {
      catalogState = state;
    });

    // Load genres (non-blocking)
    fetchGenres(transport)
      .then((gs) => {
        genres = gs.map((g) => ({ id: g.id, name: g.label }));
      })
      .catch(() => {
        // Genre list unavailable; filters still usable
      });

    // Bootstrap from URL
    const initial = parseCriteria(page.url.searchParams);
    criteria = { ...initial };
    lastDispatched = initial;
    controller.load(initial);

    return () => {
      unsubscribe();
      globalThis.clearTimeout(textDebounceTimer);
      controller?.dispose();
    };
  });

  // Push current criteria into the URL (replaceState = true for filter changes,
  // false for explicit navigation like clicking a title).
  function pushUrl(c: Criteria): void {
    const params = criteriaToParams(c);
    const qs = params.toString();
    const root = resolve('/');
    // eslint-disable-next-line svelte/no-navigation-without-resolve -- preserve catalog query string on /
    void goto(qs ? `${root}?${qs}` : root, { replaceState: true });
  }

  // Dispatch criteria to the controller, with optional debounce for text.
  function dispatch(c: Criteria, textChanged: boolean): void {
    lastDispatched = c;
    if (textChanged) {
      // Text changes: debounce 250ms and validate length.
      globalThis.clearTimeout(textDebounceTimer);
      if (!validateText(c.text, MAX_SEARCH_CODE_POINTS).valid) {
        // Invalid text: update controller immediately so it shows 'invalid' state
        // without issuing a query.
        controller?.setCriteria(c);
        return;
      }
      textDebounceTimer = globalThis.setTimeout(() => {
        controller?.setCriteria(c);
      }, 250);
    } else {
      // Filter changes (kind/genre/year/month): apply immediately.
      controller?.setCriteria(c);
    }
  }

  // Called when CatalogFilters mutates criteria via two-way binding.
  // Intercepts the change, enforces year→month, pushes URL, dispatches to controller.
  $effect(() => {
    if (!mounted || !controller) return;
    const c = criteria;

    // Enforce year → month dependency.
    const safe: Criteria = c.year ? c : { ...c, month: '' };
    if (safe.month !== c.month) {
      // Svelte 5: safe to assign reactive state inside $effect when it converges.
      criteria = safe;
      return; // Effect will re-run with safe criteria.
    }

    // Skip if this criteria was already dispatched (e.g., set from URL sync below).
    if (
      safe.text === lastDispatched.text &&
      safe.kind === lastDispatched.kind &&
      safe.year === lastDispatched.year &&
      safe.month === lastDispatched.month &&
      [...safe.genres].sort().join(',') ===
        [...lastDispatched.genres].sort().join(',')
    ) {
      return;
    }

    const textChanged = safe.text !== lastDispatched.text;
    pushUrl(safe);
    dispatch(safe, textChanged);
  });

  // Sync URL changes (Back/Forward) back to criteria and controller.
  $effect(() => {
    if (!mounted || !controller) return;
    const params = page.url.searchParams;
    const fromUrl = parseCriteria(params);
    const cur = criteria;

    const same =
      fromUrl.text === cur.text &&
      fromUrl.kind === cur.kind &&
      fromUrl.year === cur.year &&
      fromUrl.month === cur.month &&
      [...fromUrl.genres].sort().join(',') === [...cur.genres].sort().join(',');

    if (!same && !applyingUrlCriteria) {
      applyingUrlCriteria = true;
      criteria = { ...fromUrl };
      lastDispatched = fromUrl;
      controller.load(fromUrl);
      applyingUrlCriteria = false;
    }
  });

  const textInvalid = $derived(
    mounted &&
      criteria.text.length > 0 &&
      !validateText(criteria.text, MAX_SEARCH_CODE_POINTS).valid,
  );
</script>

<svelte:head><title>SpoolIn · Every reel holds a story.</title></svelte:head>

<div class="hero">
  <h1 tabindex="-1">Every reel holds a story.</h1>
  <p class="lead">
    Unspooling stories and discovering what&#x2019;s behind them.
  </p>
</div>

{#if mounted}
  <section aria-label="Catalog filters" class="stack">
    {#if textInvalid}
      <p id={filterTextErrorId} role="alert" class="field-error">
        <a href={`#${filterTextErrorId.replace('error', 'text')}`}
          >Search text is too long</a
        >
        — up to {MAX_SEARCH_CODE_POINTS} characters are accepted.
      </p>
    {/if}

    <CatalogFilters bind:criteria {genres} {years} />
  </section>

  {#if catalogState.kind === 'loading'}
    <div
      role="status"
      aria-live="polite"
      aria-label="Finding titles"
      class="catalog-status"
    >
      <p>Finding titles for the current criteria…</p>
    </div>
  {:else if catalogState.kind === 'outdated'}
    <div
      role="status"
      aria-live="polite"
      aria-label="Refreshing"
      class="catalog-status"
    >
      <p>Finding titles for the current criteria…</p>
    </div>
  {:else if catalogState.kind === 'failure'}
    <div role="alert" class="catalog-status catalog-failure">
      <p>The catalog could not load.</p>
      <button onclick={() => controller?.retry()}>Try again</button>
    </div>
  {:else if catalogState.kind === 'empty'}
    <div class="catalog-status catalog-empty">
      <p>The catalog is empty.</p>
      <a href={resolve('/')}>Browse again</a>
    </div>
  {:else if catalogState.kind === 'no-match'}
    <div class="catalog-status catalog-no-match">
      <p>No matching titles — try changing your filters.</p>
    </div>
  {:else if catalogState.kind === 'invalid'}
    <div role="alert" class="catalog-status catalog-invalid">
      <p>
        Search text is too long — up to {MAX_SEARCH_CODE_POINTS} characters are accepted.
        Correct the input above.
      </p>
    </div>
  {:else if catalogState.kind === 'catalog'}
    <section aria-label="Title results">
      <p class="result-count">
        {catalogState.titles.length} title{catalogState.titles.length === 1
          ? ''
          : 's'} found
      </p>
      <ul class="title-grid" aria-label="Titles">
        {#each catalogState.titles as t (t.id)}
          <li>
            <TitleCard
              id={t.id}
              title={t.title}
              kind={t.kind}
              date={t.releaseContext}
              artwork={t.artworkUrl}
            />
          </li>
        {/each}
      </ul>
    </section>
  {/if}
{:else}
  <!-- SSR / pre-hydration placeholder -->
  <div role="status" aria-live="polite" class="catalog-status">
    <p>Finding titles for the current criteria…</p>
  </div>
{/if}
