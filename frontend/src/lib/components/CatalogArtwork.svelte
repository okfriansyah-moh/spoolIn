<script lang="ts">
  import { onMount } from 'svelte';
  import { safeArtwork } from './safe-url';
  import { readArtwork } from '../features/artwork/read';
  let { src }: { src?: string | null } = $props();
  let element = $state<HTMLDivElement>();
  let visible = $state(false);
  let objectUrl = $state<string | null>(null);
  let failed = $state(false);

  onMount(() => {
    if (!element || !globalThis.IntersectionObserver) {
      visible = true;
      return;
    }
    const observer = new globalThis.IntersectionObserver(
      (entries) => {
        visible = !!objectUrl || entries.some((entry) => entry.isIntersecting);
      },
      { rootMargin: '120px' },
    );
    observer.observe(element);
    return () => observer.disconnect();
  });

  $effect(() => {
    const url = safeArtwork(src);
    objectUrl = null;
    failed = false;
    if (!visible || !url) return;
    const abort = new AbortController();
    let owned: string | null = null;
    void readArtwork(url, abort.signal)
      .then((value) => {
        if (abort.signal.aborted) {
          globalThis.URL.revokeObjectURL(value);
          return;
        }
        owned = value;
        objectUrl = value;
      })
      .catch(() => {
        if (!abort.signal.aborted) failed = true;
      });
    return () => {
      abort.abort();
      if (owned) globalThis.URL.revokeObjectURL(owned);
    };
  });
</script>

<div class="artwork" bind:this={element}>
  {#if objectUrl && !failed}<img
      src={objectUrl}
      alt=""
      decoding="async"
      onerror={() => (failed = true)}
    />
  {:else if !safeArtwork(src) || failed}<span class="missing-art"
      >Artwork unavailable</span
    >
  {:else}<span class="missing-art">Loading artwork…</span>{/if}
</div>
