<script lang="ts">
  import { page } from '$app/state';
  import PolicyGuidance from '../../../../../../frontend/src/lib/policy/PolicyGuidance.svelte';
  import {
    specimenPolicy,
    retainedDraft,
    policyState,
    plainTextExample,
  } from './fixture';
  const policy = $derived(
    specimenPolicy(page.url.searchParams.get('minimum') === 'raised'),
  );
</script>

<svelte:head><title>Policy guidance · SpoolIn</title></svelte:head>
<p class="muted">Controlled policy examples. No server writes.</p>
<PolicyGuidance
  view={policyState(page.url.searchParams.get('state'))}
  {policy}
  initialDraft={retainedDraft}
  {plainTextExample}
  initialName="V"
  initialOriginalTitle="Cahaya Terakhir"
/>
