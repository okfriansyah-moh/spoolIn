<script lang="ts">
  import { resolve } from '$app/paths';
  import { isRecordId } from './safe-url';
  import CatalogArtwork from './CatalogArtwork.svelte';
  let {
    id,
    title,
    kind,
    date,
    artwork,
    genres = [],
    onopen,
  }: {
    id: string;
    title: string;
    kind: 'Movie' | 'TV';
    date?: string | null;
    artwork?: string | null;
    genres?: readonly string[];
    onopen?: (event: MouseEvent, id: string) => void;
  } = $props();
</script>

<article class="title-card">
  <div class="card-poster">
    {#if isRecordId(id)}
      <a
        class="poster-link"
        href={resolve('/titles/[id]', { id })}
        aria-label={title}
        onclick={(event) => onopen?.(event, id)}
      >
        <CatalogArtwork src={artwork ?? null} />
        <span class="title-kind-badge">{kind}</span>
      </a>
    {:else}
      <CatalogArtwork src={artwork ?? null} />
      <span class="title-kind-badge">{kind}</span>
    {/if}
  </div>
  <h3>
    {#if isRecordId(id)}
      <a
        href={resolve('/titles/[id]', { id })}
        onclick={(event) => onopen?.(event, id)}>{title}</a
      >
    {:else}{title}{/if}
  </h3>
  <p>{date ?? 'Release date unknown'}</p>
  {#if genres.length}<p class="card-genres">{genres.join(', ')}</p>{/if}
</article>

<style>
  .poster-link {
    display: block;
  }
</style>
