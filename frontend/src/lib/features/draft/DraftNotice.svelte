<script lang="ts">
  import Notice from '../../components/Notice.svelte';
  import type { DraftViewState } from './types';
  let { state }: { state: DraftViewState } = $props();
  const title = $derived(
    state.notice === 'unbacked'
      ? 'Changes are not backed up yet'
      : state.notice === 'backed'
        ? 'Draft saved'
        : state.notice === 'saving'
          ? 'Saving draft'
          : state.notice === 'recheck'
            ? 'Check before continuing'
            : state.notice === 'discarding'
              ? 'Discarding draft'
              : state.notice === 'discard-failed'
                ? 'Discard did not complete'
                : state.notice === 'discard-uncertain'
                  ? 'Discard is not confirmed'
                  : state.message || 'Draft',
  );
  const tone = $derived(
    state.notice === 'unbacked' ||
      state.notice === 'discard-failed' ||
      state.notice === 'discard-uncertain'
      ? 'warning'
      : state.notice === 'recheck'
        ? 'info'
        : 'info',
  );
  const urgent = $derived(state.notice === 'unbacked');
</script>

{#if !state.concealed && (state.message || state.notice !== 'idle')}
  <Notice {title} {tone} {urgent}><p>{state.message}</p></Notice>
{/if}
