<script lang="ts">
  import type { Snippet } from 'svelte';
  let {
    current,
    draft,
    children,
  }: { current: string; draft: string; children?: Snippet } = $props();
</script>

<section class="panel">
  <h2>Review the newer saved version</h2>
  <p>Your changes have not replaced the current version.</p>
  <div class="review-grid">
    <div>
      <h3>Current saved version</h3>
      <p class="preserve">{current}</p>
    </div>
    <div>
      <h3>Your retained draft</h3>
      <p class="preserve">{draft}</p>
    </div>
  </div>
  {#if children}{@render children()}{/if}
</section>
