<script lang="ts">
  import type { Criteria } from '../features/catalog/types';
  let {
    criteria = $bindable(),
    genres,
    years,
    months = [],
    onsearch,
  }: {
    criteria: Criteria;
    genres: { id: string; name: string }[];
    years: string[];
    months?: number[];
    onsearch?: () => void;
  } = $props();
  const id = $props.id();
  let genreOpen = $state(false);
  let genreSearch = $state('');
  let genreDisclosure = $state<HTMLDetailsElement>();

  function dismissGenres(event: MouseEvent): void {
    if (
      genreOpen &&
      genreDisclosure &&
      !event.composedPath().includes(genreDisclosure)
    )
      genreOpen = false;
  }

  function closeGenres(): void {
    genreOpen = false;
    genreDisclosure?.querySelector('summary')?.focus({ preventScroll: true });
  }

  function genreKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && genreOpen) {
      event.preventDefault();
      closeGenres();
    }
  }
  const selectedGenres = $derived(
    genres.filter((genre) => criteria.genres.includes(genre.id)),
  );
  const visibleGenres = $derived.by(() => {
    const query = genreSearch.trim().toLocaleLowerCase();
    return genres.filter((genre) =>
      genre.name.toLocaleLowerCase().includes(query),
    );
  });
  const monthNames = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ];

  function yearChanged(event: Event) {
    const year = (event.currentTarget as HTMLSelectElement).value;
    criteria = { ...criteria, year, month: '' };
  }

  function toggleGenre(genreId: string, checked: boolean): void {
    const next = checked
      ? criteria.genres.includes(genreId)
        ? criteria.genres
        : Object.freeze([...criteria.genres, genreId])
      : Object.freeze(criteria.genres.filter((id) => id !== genreId));
    criteria = { ...criteria, genres: next };
  }
</script>

<svelte:window onclick={dismissGenres} onkeydown={genreKeydown} />

<div class="catalog-controls stack">
  <form
    class="field catalog-search"
    onsubmit={(event) => {
      event.preventDefault();
      onsearch?.();
    }}
  >
    <label for={`${id}-text`}>Search titles</label><input
      id={`${id}-text`}
      type="search"
      bind:value={criteria.text}
    /><button
      type="button"
      class="secondary"
      onclick={() => (criteria = { ...criteria, text: '' })}
      >Clear search text</button
    >
  </form>
  <div class="filter-categories">
    <fieldset class="type-filters">
      <legend>Title type</legend>
      {#each [['', 'All titles'], ['MOVIE', 'Movies'], ['TV', 'TV series']] as choice (choice[0])}
        <label
          ><input
            type="radio"
            name={`${id}-kind`}
            value={choice[0]}
            checked={criteria.kind === choice[0]}
            onchange={() => {
              criteria = {
                ...criteria,
                kind:
                  choice[0] === 'MOVIE'
                    ? 'MOVIE'
                    : choice[0] === 'TV'
                      ? 'TV'
                      : '',
              };
            }}
          /><span>{choice[1]}</span></label
        >
      {/each}
    </fieldset>
    <fieldset class="genre-choice">
      <legend>Genres · match any selected</legend>
      {#if selectedGenres.length}
        <ul class="selected-genre-filters" aria-label="Selected genres">
          {#each selectedGenres as genre (genre.id)}
            <li>
              <span>{genre.name}</span>
              <button
                type="button"
                class="remove-genre"
                aria-label="Remove {genre.name} filter"
                onclick={() => toggleGenre(genre.id, false)}>×</button
              >
            </li>
          {/each}
        </ul>
      {/if}
      <details
        class="genre-filter-disclosure"
        bind:this={genreDisclosure}
        bind:open={genreOpen}
      >
        <summary>
          {criteria.genres.length ? 'Change genres' : 'Choose genres'}
          <span>{criteria.genres.length} selected</span>
        </summary>
        <div class="genre-filter-panel">
          <label for={`${id}-genre-search`}>Find a genre</label>
          <input
            id={`${id}-genre-search`}
            type="search"
            bind:value={genreSearch}
            placeholder="Search genres"
          />
          <small role="status" aria-live="polite">
            {visibleGenres.length}
            {visibleGenres.length === 1 ? 'genre' : 'genres'}
          </small>
          <!-- svelte-ignore a11y_no_noninteractive_tabindex (Keyboard users can focus and scroll this bounded long options region.) -->
          <div
            class="genre-options"
            role="region"
            aria-label="Genre choices"
            aria-describedby={`${id}-genres-help`}
            tabindex="0"
          >
            {#each visibleGenres as genre (genre.id)}<label class="check"
                ><input
                  type="checkbox"
                  value={genre.id}
                  checked={criteria.genres.includes(genre.id)}
                  onchange={(event) =>
                    toggleGenre(genre.id, event.currentTarget.checked)}
                />{genre.name}</label
              >{:else}<p>
                No genres match that search. Try another name.
              </p>{/each}
          </div>
          <small id={`${id}-genres-help`}
            >Genres with available titles are listed. Scroll or search by name.</small
          >
          <button type="button" class="secondary" onclick={closeGenres}
            >Close genres</button
          >
        </div>
      </details>
    </fieldset>
    <div class="field">
      <label for={`${id}-year`}>Release year</label><select
        id={`${id}-year`}
        value={criteria.year}
        onchange={yearChanged}
        ><option value="">All years</option>{#each years as year (year)}<option
            value={year}>{year}</option
          >{/each}
        {#if criteria.year && !years.includes(criteria.year)}
          <option value={criteria.year} disabled
            >{criteria.year} (unavailable)</option
          >
        {/if}
      </select>
    </div>
    <div class="field">
      <label for={`${id}-month`}>Month</label><select
        id={`${id}-month`}
        bind:value={criteria.month}
        disabled={!criteria.year}
        aria-describedby={`${id}-month-help`}
        ><option value="">All months</option
        >{#each months as month (month)}<option value={String(month)}
            >{monthNames[month - 1]}</option
          >{/each}
        {#if criteria.month && !months.includes(Number(criteria.month))}
          <option value={criteria.month} disabled
            >{monthNames[Number(criteria.month) - 1] ?? criteria.month} (unavailable)</option
          >
        {/if}
      </select><small id={`${id}-month-help`}
        >Choose a year first. Clearing it also clears month. Other categories
        combine with AND.</small
      >
    </div>
  </div>
</div>

<style>
  .catalog-controls {
    gap: 28px;
  }
  .catalog-search {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 0;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    padding: 6px;
    background: var(--surface);
  }
  .catalog-search label {
    grid-column: 1 / -1;
    margin: -32px 0 8px -6px;
    font-size: 13px;
  }
  .catalog-search input {
    border: 0;
    background: transparent;
  }
  .catalog-search button {
    margin-left: 8px;
  }
  .type-filters {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    border: 0;
    padding: 0;
    margin: 0;
  }
  .type-filters legend {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
  }
  .type-filters label {
    position: relative;
    cursor: pointer;
  }
  .type-filters input {
    position: absolute;
    opacity: 0;
    width: 1px;
    height: 1px;
    min-height: 0;
  }
  .type-filters span {
    display: block;
    padding: 9px 17px;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    font-size: 14px;
    font-weight: 500;
    min-height: 44px;
  }
  .type-filters input:checked + span {
    background: var(--selected);
    border-color: var(--accent);
  }
  .type-filters input:focus-visible + span {
    outline: 3px solid var(--accent);
    outline-offset: 3px;
  }
  .filter-categories > .field {
    font-size: 13px;
  }
  .filter-categories > .field label {
    color: var(--muted);
  }
  .filter-categories > .field select {
    font-size: 14px;
  }

  .filter-categories {
    display: grid;
    grid-template-columns:
      minmax(240px, 1.25fr) minmax(180px, 1fr) minmax(120px, 0.7fr)
      minmax(120px, 0.7fr);
    align-items: start;
    gap: var(--primitive-space-6);
  }
  .genre-choice {
    border: 0;
    padding: 0;
    margin: 0;
    inline-size: 100%;
    max-inline-size: 280px;
  }
  .genre-filter-disclosure {
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: var(--surface);
  }
  .genre-filter-disclosure summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--primitive-space-2);
    padding-inline: var(--primitive-space-3);
    font-weight: 600;
  }
  .genre-filter-disclosure summary span {
    color: var(--muted);
    font-size: var(--primitive-type-small);
    font-weight: 400;
  }
  .genre-filter-panel {
    display: grid;
    gap: var(--primitive-space-2);
    padding: 0 var(--primitive-space-3) var(--primitive-space-3);
  }
  .selected-genre-filters {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-1);
    padding: 0;
    margin: 0;
    list-style: none;
  }
  .selected-genre-filters li {
    display: inline-flex;
    align-items: center;
    gap: var(--primitive-space-1);
    padding-left: var(--primitive-space-2);
    border: 1px solid var(--line);
    border-radius: 999px;
    background: var(--selected);
  }
  .selected-genre-filters .remove-genre {
    width: 44px;
    height: 44px;
    min-height: 44px;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: transparent;
    color: var(--text);
    font-size: 1.2rem;
  }
  .genre-options {
    display: grid;
    max-block-size: calc(5 * var(--primitive-space-12));
    overflow-y: auto;
    scrollbar-gutter: stable;
    padding: var(--primitive-space-2);
  }
  .genre-options input {
    flex-shrink: 0;
  }
  @media (max-width: 1000px) {
    .filter-categories {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
  @media (max-width: 700px) {
    .catalog-search {
      grid-template-columns: minmax(0, 1fr) auto;
    }
    .catalog-search button {
      font-size: 12px;
      padding-inline: 10px;
    }
    .type-filters span {
      font-size: 12px;
      padding-inline: 13px;
    }
    .filter-categories {
      grid-template-columns: minmax(0, 1fr);
    }
    .genre-choice {
      max-inline-size: none;
    }
    .genre-filter-disclosure summary {
      align-items: flex-start;
      flex-direction: column;
    }
  }
</style>
