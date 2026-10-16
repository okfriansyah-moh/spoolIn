<script lang="ts">
  import { resolve } from '$app/paths';
  import { isRecordId } from './safe-url';
  import ArtworkSlot from './ArtworkSlot.svelte';
  let {
    id,
    title,
    kind,
    date,
    artwork,
  }: {
    id: string;
    title: string;
    kind: 'Movie' | 'TV';
    date?: string | null;
    artwork?: string | null;
  } = $props();
</script>

<article class="title-card">
  <ArtworkSlot src={artwork ?? null} />
  <h3>
    {#if isRecordId(id)}
      <!-- eslint-disable-next-line svelte/no-navigation-without-resolve -- T013 owns the title route; preserve its contract and base until it is generated. -->
      <a href={`${resolve('/')}titles/${encodeURIComponent(id)}`}>{title}</a>
    {:else}{title}{/if}
  </h3>
  <p>{kind} · {date ?? 'Release date unknown'}</p>
</article>
