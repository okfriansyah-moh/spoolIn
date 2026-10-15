<script lang="ts">
  let {
    values,
    source,
  }: {
    values: { label: string; value: string | number | null }[];
    source?: string;
  } = $props();
</script>

<dl class="metadata">
  {#each values as item (item.label)}<div>
      <dt>{item.label}</dt>
      <dd>{item.value ?? 'Unknown'}</dd>
    </div>{/each}
</dl>
{#if source}<details>
    <summary>Source context</summary>
    <p class="preserve">{source}</p>
  </details>{/if}
