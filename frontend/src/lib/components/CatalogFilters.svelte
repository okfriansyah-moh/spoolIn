<script lang="ts">
  import type { Criteria } from '../features/catalog/types';
  let {
    criteria = $bindable(),
    genres,
    years,
  }: {
    criteria: Criteria;
    genres: { id: string; name: string }[];
    years: string[];
  } = $props();
  const id = $props.id();
  let customYear = $state(false);
  let customYearText = $state('');
  let yearError = $state('');
  let genreOpen = $state(false);
  let genreSearch = $state('');
  const selectedGenres = $derived(
    genres.filter((genre) => criteria.genres.includes(genre.id)),
  );
  const visibleGenres = $derived.by(() => {
    const query = genreSearch.trim().toLocaleLowerCase();
    return genres.filter((genre) =>
      genre.name.toLocaleLowerCase().includes(query),
    );
  });
  const selectedYear = $derived(
    customYear || (!!criteria.year && !years.includes(criteria.year))
      ? 'other'
      : criteria.year,
  );
  let priorYear = '';
  $effect(() => {
    const year = criteria.year;
    if (year === priorYear) return;
    priorYear = year;
    customYear = !!year && !years.includes(year);
    customYearText = year;
    yearError = '';
  });
  function yearChanged(event: Event) {
    const year = (event.currentTarget as HTMLSelectElement).value;
    yearError = '';
    if (year === 'other') {
      customYear = true;
      customYearText = criteria.year;
      return;
    }
    customYear = false;
    criteria = { ...criteria, year, month: year ? criteria.month : '' };
  }
  function otherYearChanged(event: Event) {
    customYear = true;
    customYearText = (event.currentTarget as HTMLInputElement).value;
    if (customYearText && !/^[0-9]{1,4}$/.test(customYearText)) {
      yearError =
        'Enter a release year from 1 to 9999. The current year filter stays unchanged.';
      return;
    }
    if (customYearText && Number(customYearText) < 1) {
      yearError =
        'Enter a release year from 1 to 9999. The current year filter stays unchanged.';
      return;
    }
    yearError = '';
    criteria = {
      ...criteria,
      year: customYearText,
      month: customYearText ? criteria.month : '',
    };
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

<div class="catalog-controls stack">
  <div class="field catalog-search">
    <label for={`${id}-text`}>Search titles</label><input
      id={`${id}-text`}
      type="search"
      bind:value={criteria.text}
    /><button
      class="secondary"
      onclick={() => (criteria = { ...criteria, text: '' })}
      >Clear search text</button
    >
  </div>
  <div class="filter-categories">
    <fieldset class="type-filters">
      <legend>Title type</legend>
      {#each [['', 'All titles'], ['MOVIE', 'Movies'], ['TV', 'TV series']] as choice (choice[0])}
        <label
          ><input
            type="radio"
            name={`${id}-kind`}
            value={choice[0]}
            bind:group={criteria.kind}
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
      <details class="genre-filter-disclosure" bind:open={genreOpen}>
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
            >Every genre stays available. Scroll the list or search by name.</small
          >
        </div>
      </details>
    </fieldset>
    <div class="field">
      <label for={`${id}-year`}>Release year</label><select
        id={`${id}-year`}
        value={selectedYear}
        onchange={yearChanged}
        ><option value="">All years</option>{#each years as year (year)}<option
            value={year}>{year}</option
          >{/each}<option value="other">Choose another year…</option></select
      >
      {#if selectedYear === 'other'}
        <label for={`${id}-other-year`}>Other release year</label>
        <input
          id={`${id}-other-year`}
          inputmode="numeric"
          value={customYear ? customYearText : criteria.year}
          aria-invalid={!!yearError}
          aria-describedby={`${id}-year-help`}
          oninput={otherYearChanged}
        />
        <small id={`${id}-year-help`}
          >{yearError ||
            'Enter any known release year from 1 to 9999, or clear it for all years.'}</small
        >
      {/if}
    </div>
    <div class="field">
      <label for={`${id}-month`}>Month</label><select
        id={`${id}-month`}
        bind:value={criteria.month}
        disabled={!criteria.year}
        aria-describedby={`${id}-month-help`}
        ><option value="">All months</option
        >{#each ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'] as month, i (month)}<option
            value={String(i + 1)}>{month}</option
          >{/each}</select
      ><small id={`${id}-month-help`}
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
    grid-template-columns: minmax(240px, 1.25fr) minmax(180px, 1fr) minmax(
        120px,
        0.7fr
      ) minmax(120px, 0.7fr);
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
