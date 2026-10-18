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

<div class="stack">
  <div class="field">
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
    <div class="field">
      <label for={`${id}-kind`}>Title type</label><select
        id={`${id}-kind`}
        bind:value={criteria.kind}
        ><option value="">All titles</option><option value="MOVIE"
          >Movies</option
        ><option value="TV">TV</option></select
      >
    </div>
    <fieldset class="genre-choice">
      <legend>Genres · match any selected</legend>
      <div
        class="genre-options"
        role="region"
        aria-label="Genre choices"
        aria-describedby={`${id}-genres-help`}
        tabindex="0"
      >
        {#each genres as genre (genre.id)}<label class="check"
            ><input
              type="checkbox"
              value={genre.id}
              checked={criteria.genres.includes(genre.id)}
              onchange={(event) =>
                toggleGenre(genre.id, event.currentTarget.checked)}
            />{genre.name}</label
          >{/each}
      </div>
      <small id={`${id}-genres-help`}
        >Scroll or use the keyboard to reach every genre.</small
      >
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
  .filter-categories {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    align-items: start;
    gap: var(--primitive-space-4);
  }
  .genre-choice {
    inline-size: 100%;
    max-inline-size: 280px;
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
    .filter-categories {
      grid-template-columns: minmax(0, 1fr);
    }
    .genre-choice {
      max-inline-size: none;
    }
  }
</style>
