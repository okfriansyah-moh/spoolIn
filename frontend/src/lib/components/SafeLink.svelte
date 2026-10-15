<script lang="ts">
  import { safeUrl } from './safe-url';
  let { url, text }: { url: string; text: string } = $props();
  const href = $derived(safeUrl(url));
</script>

{#if href}<a {href} rel="external noopener noreferrer">{text}</a>{:else}<span
    >{text}</span
  >{/if}
