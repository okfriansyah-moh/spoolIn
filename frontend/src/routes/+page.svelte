<script lang="ts">
  import { compareUtf16 } from '../lib/state/ordering';
  import heroArtwork from '../lib/design-system/artwork/hero.png';
  /**
   * T013 – Discovery page (root route).
   *
   * States: catalog, loading, empty, no-match, failure, outdated, invalid
   * URL state: q, kind, genre (repeated), year, month, sort
   * Debounce: 250ms on text changes; non-text filter changes are immediate.
   * Generation-bound: stale replies are suppressed in the controller.
   *
   * SSR note: hero/head always rendered; interactive sections are client-side only.
   */
  import { onMount, untrack, tick } from 'svelte';
  import { plainNavigation } from '../lib/features/catalog/return-context';
  import { catalogPageStarts } from '../lib/features/catalog/page-history';
  import { catalogSort } from '../lib/features/catalog/types';
  import { historyCursor } from '../lib/state/pagination';
  import { goto } from '$app/navigation';
  import { resolve } from '$app/paths';
  import { page } from '$app/state';
  import { createTransport } from '../lib/transport/graphql';
  import { CatalogController } from '../lib/features/catalog/controller.svelte';
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
  import { fetchCatalogFilterOptions } from '../lib/features/catalog/api';
  import CatalogFilters from '../lib/components/CatalogFilters.svelte';
  import TitleCard from '../lib/components/TitleCard.svelte';

  let controller = $state.raw<CatalogController | null>(null);
  let mounted = $state(false);
  let genres = $state<Array<{ id: string; name: string }>>([]);
  let genreError = $state('');
  let genresLoading = $state(false);
  let genreAbort: AbortController | null = null;
  let releaseYears = $state<{ year: number; months: number[] }[]>([]);
  const years = $derived(releaseYears.map((date) => String(date.year)));
  const months = $derived(
    releaseYears.find((date) => String(date.year) === criteria.year)?.months ??
      [],
  );
  const catalogState = $derived<CatalogState>(
    controller?.snapshot ?? {
      kind: 'loading',
      criteria: EMPTY_CRITERIA,
      titles: [],
      pageInfo: null,
      invalidField: null,
    },
  );

  // Criteria state: synced from URL on load and updated by filters.
  let criteria = $state<Criteria>({ ...EMPTY_CRITERIA });

  // Non-reactive: track last criteria dispatched to controller to avoid duplicate sends.
  let lastDispatched: Criteria = EMPTY_CRITERIA;
  // Non-reactive: guard to prevent URL sync loop.
  let applyingUrlCriteria = false;
  let lastPageCursor: string | null = null;
  let resultsHeading = $state<HTMLElement>();

  const filterTextErrorId = 'catalog-text-error';

  onMount(() => {
    mounted = true;
    const transport = createTransport();
    controller = new CatalogController(transport);

    void loadFilterOptions();

    // Bootstrap from URL
    const initial = parseCriteria(page.url.searchParams);
    criteria = { ...initial };
    lastDispatched = initial;
    lastPageCursor = historyCursor(page.state, 'catalogCursor');
    controller.load(
      initial,
      lastPageCursor,
      catalogPageStarts(page.state, lastPageCursor),
    );

    return () => {
      genreAbort?.abort();
      controller?.dispose();
    };
  });

  async function loadFilterOptions() {
    genreAbort?.abort();
    const abort = new AbortController();
    genreAbort = abort;
    genresLoading = true;
    genreError = '';
    try {
      const current = await fetchCatalogFilterOptions(
        createTransport(),
        abort.signal,
      );
      if (!abort.signal.aborted) {
        genres = current.genres.map((genre) => ({
          id: genre.id,
          name: genre.label,
        }));
        releaseYears = current.releaseYears;
      }
    } catch {
      if (!abort.signal.aborted)
        genreError =
          'Filter choices could not be loaded. Your current filters are retained.';
    } finally {
      if (!abort.signal.aborted) genresLoading = false;
    }
  }

  // Update catalog history without route navigation, preserving focus and scroll.
  function pushUrl(c: Criteria): void {
    const params = criteriaToParams(c);
    const qs = params.toString();
    const root = resolve('/');
    // eslint-disable-next-line svelte/no-navigation-without-resolve -- preserve catalog query string on /
    void goto(qs ? `${root}?${qs}` : root, {
      shallow: true,
      replace: true,
      state: { ...page.state, catalogCursor: null, catalogPages: [null] },
    });
  }

  // Dispatch criteria to the controller, with optional debounce for text.
  function dispatch(c: Criteria, textChanged: boolean): void {
    lastDispatched = { ...c, genres: [...c.genres] };
    lastPageCursor = null;
    controller?.setCriteria(lastDispatched, { debounce: textChanged });
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
    if (sameCriteria(safe, lastDispatched)) return;

    const textChanged = safe.text !== lastDispatched.text;
    pushUrl(safe);
    dispatch(safe, textChanged);
  });

  // Sync URL changes (Back/Forward) back to criteria and controller.
  $effect(() => {
    if (!mounted || !controller) return;
    const params = (page.shallow?.url ?? page.url).searchParams;
    const fromUrl = parseCriteria(params);
    const cur = untrack(() => criteria);

    const cursor = historyCursor(page.state, 'catalogCursor');
    const same = cursor === lastPageCursor && sameCriteria(fromUrl, cur);

    if (!same && !applyingUrlCriteria) {
      applyingUrlCriteria = true;
      criteria = { ...fromUrl };
      lastDispatched = fromUrl;
      lastPageCursor = cursor;
      controller.load(fromUrl, cursor, catalogPageStarts(page.state, cursor));
      applyingUrlCriteria = false;
    }
  });

  function sameCriteria(a: Criteria, b: Criteria): boolean {
    return (
      catalogSort(a.sort) === catalogSort(b.sort) &&
      a.text === b.text &&
      a.kind === b.kind &&
      a.year === b.year &&
      a.month === b.month &&
      [...a.genres].sort(compareUtf16).join(',') ===
        [...b.genres].sort(compareUtf16).join(',')
    );
  }

  async function changeTitlesPage(direction: 'next' | 'previous') {
    if (!controller) return;
    const success =
      direction === 'next'
        ? await controller.loadMore()
        : await controller.previousPage();
    if (!success) return;
    const state = controller.snapshot;
    lastPageCursor = state.pageStarts?.[(state.pageNumber ?? 1) - 1] ?? null;
    // eslint-disable-next-line svelte/no-navigation-without-resolve -- retain the current catalog criteria
    await goto((page.shallow?.url ?? page.url).href, {
      shallow: true,
      state: {
        ...page.state,
        catalogCursor: lastPageCursor,
        catalogPages: state.pageStarts ?? [null],
      },
    });
    await focusResults();
  }

  async function focusResults() {
    await tick();
    resultsHeading?.focus({ preventScroll: true });
    resultsHeading?.scrollIntoView?.({ block: 'start', behavior: 'instant' });
  }

  function openTitle(event: MouseEvent, id: string) {
    if (!plainNavigation(event)) return;
    event.preventDefault();
    const state = controller?.snapshot;
    void goto(resolve('/titles/[id]', { id }), {
      state: {
        catalogReturn: {
          url: (page.shallow?.url ?? page.url).href,
          catalogCursor: lastPageCursor,
          catalogPages: state?.pageStarts ?? [null],
        },
      },
    });
  }

  function restartTitles() {
    if (!controller) return;
    lastPageCursor = null;
    pushUrl(criteria);
    controller.load(criteria);
  }

  const textInvalid = $derived(
    mounted &&
      criteria.text.length > 0 &&
      !validateText(criteria.text, MAX_SEARCH_CODE_POINTS).valid,
  );
</script>

<svelte:head><title>SpoolIn · Every reel holds a story.</title></svelte:head>

<section class="catalog-hero" aria-label="SpoolIn introduction">
  <img class="hero-photo" src={heroArtwork} alt="" />
  <div class="hero-copy">
    <h1 tabindex="-1">Every reel<br />holds a story.</h1>
    <p>Unspooling stories and discovering what&#x2019;s behind them.</p>
  </div>
</section>

<div class="catalog-content">
  {#if mounted}
    <section aria-label="Catalog filters" class="catalog-filters stack">
      {#if textInvalid}
        <p id={filterTextErrorId} role="alert" class="field-error">
          <a href={`#${filterTextErrorId.replace('error', 'text')}`}
            >Search text is too long</a
          >
          — up to {MAX_SEARCH_CODE_POINTS} characters are accepted.
        </p>
      {/if}

      {#if genresLoading}<p role="status">Loading filter choices…</p>{/if}
      {#if genreError}<p role="alert">{genreError}</p>
        <button type="button" onclick={() => void loadFilterOptions()}
          >Retry filter choices</button
        >{/if}
      <CatalogFilters
        bind:criteria
        {genres}
        {years}
        {months}
        onsearch={() => dispatch(criteria, false)}
      />
    </section>

    <div class="catalog-results">
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
          {#if catalogState.cursorInvalid}<button onclick={restartTitles}
              >Restart from page one</button
            >{/if}
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
            Search text is too long — up to {MAX_SEARCH_CODE_POINTS} characters are
            accepted. Correct the input above.
          </p>
        </div>
      {:else if catalogState.kind === 'catalog'}
        <section aria-label="Title results">
          <div class="catalog-heading">
            <h2 bind:this={resultsHeading} tabindex="-1">
              Explore the catalog
            </h2>
            <p class="result-count">
              {catalogState.titles.length} title{catalogState.titles.length ===
              1
                ? ''
                : 's'} found
            </p>
          </div>
          {#if catalogState.titles.length === 0}<p>
              No titles remain on this page. Go back or restart from page one.
            </p>{/if}
          <ul class="title-grid" aria-label="Titles">
            {#each catalogState.titles as t (t.id)}
              <li>
                <TitleCard
                  onopen={openTitle}
                  id={t.id}
                  title={t.title}
                  kind={t.kind === 'TV' ? 'TV' : 'Movie'}
                  date={t.displayRelease ?? null}
                  genres={t.genreLabels ?? []}
                  artwork={t.artworkUrl}
                />
              </li>
            {/each}
          </ul>
          {#if catalogState.pageError}<p role="alert">
              {catalogState.pageError}
            </p>{/if}
          {#if catalogState.loadingMore}<p role="status" aria-live="polite">
              Loading {catalogState.pageDirection ?? 'next'} titles…
            </p>{/if}
          <nav class="catalog-pagination" aria-label="Title pages">
            <button
              type="button"
              disabled={(catalogState.pageNumber ?? 1) <= 1 ||
                catalogState.loadingMore}
              aria-busy={catalogState.loadingMore &&
                catalogState.pageDirection === 'previous'}
              onclick={() => void changeTitlesPage('previous')}
              >Previous titles page</button
            >
            <span role="status" aria-live="polite"
              >Page {catalogState.pageNumber ?? 1}</span
            >
            <button
              type="button"
              disabled={!catalogState.pageInfo?.hasNextPage ||
                catalogState.loadingMore}
              aria-busy={catalogState.loadingMore &&
                catalogState.pageDirection === 'next'}
              onclick={() => void changeTitlesPage('next')}
              >Next titles page</button
            >
          </nav>
          {#if catalogState.pageError}
            <button
              type="button"
              disabled={catalogState.loadingMore}
              onclick={() =>
                void changeTitlesPage(catalogState.pageDirection ?? 'next')}
              >Retry {catalogState.pageDirection ?? 'next'} titles page</button
            >
          {/if}
          {#if catalogState.pageError || catalogState.titles.length === 0}
            <button
              type="button"
              disabled={catalogState.loadingMore}
              onclick={restartTitles}>Restart from page one</button
            >
          {/if}
        </section>
      {/if}
    </div>
  {:else}
    <!-- SSR / pre-hydration placeholder -->
    <div role="status" aria-live="polite" class="catalog-status">
      <p>Finding titles for the current criteria…</p>
    </div>
  {/if}
</div>

<style>
  .catalog-pagination {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
    margin-top: 1.5rem;
  }
  .catalog-pagination span {
    color: var(--muted);
  }
  .catalog-results {
    min-height: 100vh;
  }
</style>
