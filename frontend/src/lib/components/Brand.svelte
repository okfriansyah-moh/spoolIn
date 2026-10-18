<script lang="ts">
  let {
    compact = false,
    size = 24,
  }: { compact?: boolean; size?: 16 | 24 | 48 } = $props();
</script>

{#if compact}<img
    src="/icon.svg"
    width={size}
    height={size}
    alt="SpoolIn"
  />{:else}<span class="wordmark"
    ><span class="brand-mark" aria-hidden="true"><i></i><i></i></span
    >SpoolIn</span
  >{/if}
