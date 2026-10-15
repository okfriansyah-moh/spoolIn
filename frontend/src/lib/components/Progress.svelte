<script lang="ts">
  let {
    stage,
    value,
  }: { stage: 'loading' | 'transfer' | 'processing'; value?: number } =
    $props();
  const id = $props.id();
  const amount = $derived(
    value !== undefined && Number.isFinite(value)
      ? Math.max(0, Math.min(100, value))
      : undefined,
  );
</script>

<div role="status">
  <label for={id}
    >{stage === 'transfer'
      ? `Transfer${amount === undefined ? '' : ` · ${amount}%`}`
      : stage === 'processing'
        ? 'Processing artwork'
        : 'Loading the current search…'}</label
  >{#if stage === 'transfer'}<progress {id} max="100" value={amount}
    ></progress>{:else}<div
      class="skeleton"
      aria-hidden="true"
    ></div>{/if}{#if stage === 'transfer' && amount === 100}<p>
      Transfer complete. Waiting for processing and attachment.
    </p>{/if}
</div>
