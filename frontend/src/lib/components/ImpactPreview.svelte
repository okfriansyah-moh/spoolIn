<script lang="ts">
  let {
    target,
    activeMovies,
    archivedMovies,
    tv,
    changed = false,
  }: {
    target: string;
    activeMovies: number;
    archivedMovies: number;
    tv: number;
    changed?: boolean;
  } = $props();
</script>

<section class="panel">
  <h2>Impact on {target}</h2>
  {#if changed}<p role="alert">
      The impact changed. Review it again before confirming.
    </p>{/if}
  <dl class="metadata">
    <div>
      <dt>Active movies</dt>
      <dd>{activeMovies}</dd>
    </div>
    <div>
      <dt>Archived movies</dt>
      <dd>{archivedMovies}</dd>
    </div>
    <div>
      <dt>TV</dt>
      <dd>{tv}</dd>
    </div>
  </dl>
  <p>Archive before permanent deletion. Restore before editing.</p>
</section>
