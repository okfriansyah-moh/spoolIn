<script lang="ts">
  import { safeUrl } from './safe-url';
  import type { CatalogTitleDetail } from '../features/catalog/api';

  let { title }: { title: Pick<CatalogTitleDetail, 'artwork' | 'provenance'> } =
    $props();
  const source = $derived(
    title.artwork
      ? title.provenance.find(
          (row) =>
            row.source === 'poster' &&
            row.externalId === title.artwork?.assetId &&
            safeUrl(row.recordUrl),
        )
      : undefined,
  );
</script>

{#if source?.recordUrl}
  <p class="poster-source">
    <!-- eslint-disable-next-line svelte/no-navigation-without-resolve -- Recorded external poster source. -->
    <a href={source.recordUrl} target="_blank" rel="noopener noreferrer"
      >{source.reuseNote}</a
    >
  </p>
{/if}

<style>
  .poster-source {
    margin-block: 0.75rem 0;
    font-size: 0.8rem;
    line-height: 1.5;
    overflow-wrap: anywhere;
  }

  a {
    color: var(--color-text-secondary, currentColor);
    text-underline-offset: 0.2em;
  }
</style>
