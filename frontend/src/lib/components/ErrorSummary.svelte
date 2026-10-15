<script lang="ts">
  import { tick } from 'svelte';
  let { errors }: { errors: { id: string; message: string }[] } = $props();
  let summary = $state<HTMLDivElement>();
  $effect(() => {
    if (errors.length) void tick().then(() => summary?.focus());
  });
</script>

{#if errors.length}<div
    bind:this={summary}
    class="notice error"
    tabindex="-1"
    role="alert"
  >
    <h2>Check these fields</h2>
    <ul>
      {#each errors as error (error.id)}<li>
          <a href={`#${error.id}`}>{error.message}</a>
        </li>{/each}
    </ul>
  </div>{/if}
