<script lang="ts">
  export type Criteria = {
    text: string;
    kind: string;
    genres: string[];
    year: string;
    month: string;
  };
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
  function yearChanged(event: Event) {
    const year = (event.currentTarget as HTMLSelectElement).value;
    criteria = { ...criteria, year, month: year ? criteria.month : '' };
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
  <label for={`${id}-kind`}>Title type</label><select
    id={`${id}-kind`}
    bind:value={criteria.kind}
    ><option value="">All titles</option><option value="MOVIE">Movies</option
    ><option value="TV">TV</option></select
  >
  <fieldset>
    <legend>Genres · match any selected</legend
    >{#each genres as genre (genre.id)}<label class="check"
        ><input
          type="checkbox"
          value={genre.id}
          bind:group={criteria.genres}
        />{genre.name}</label
      >{/each}
  </fieldset>
  <label for={`${id}-year`}>Release year</label><select
    id={`${id}-year`}
    value={criteria.year}
    onchange={yearChanged}
    ><option value="">All years</option>{#each years as year (year)}<option
        value={year}>{year}</option
      >{/each}</select
  ><label for={`${id}-month`}>Month</label><select
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
