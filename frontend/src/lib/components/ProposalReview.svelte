<script lang="ts">
  let {
    revision,
    values,
    state,
  }: {
    revision: string;
    values: { label: string; value: string | null }[];
    state: 'review' | 'rejected' | 'approved' | 'uncertain';
  } = $props();
</script>

<section class="panel">
  <h2>Movie proposal</h2>
  <p>Revision: {revision}</p>
  <dl class="metadata">
    {#each values as item (item.label)}<div>
        <dt>{item.label}</dt>
        <dd>{item.value ?? 'Unknown'}</dd>
      </div>{/each}
  </dl>
  <p role="status">
    {state === 'approved'
      ? 'Revision approved. Creation is not yet confirmed.'
      : state === 'rejected'
        ? 'Proposal rejected. Nothing was published.'
        : state === 'uncertain'
          ? 'Creation result not confirmed. Check the original operation.'
          : 'Review exact values and sources before approval in the connected client.'}
  </p>
</section>
