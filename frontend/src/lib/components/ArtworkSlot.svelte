<script lang="ts">
  import { safeArtwork } from './safe-url';
  let { src, alt = '' }: { src?: string | null; alt?: string } = $props();
  let failed = $state(false);
  const url = $derived(safeArtwork(src));
  $effect(() => {
    void src;
    failed = false;
  });
</script>

<div class="artwork">
  {#if url && !failed}<img
      src={url}
      {alt}
      onerror={() => (failed = true)}
    />{:else}<span class="missing-art">Artwork unavailable</span>{/if}
</div>
