<script lang="ts">
  import Notice from './Notice.svelte';
  import { operationMessage, type OperationState } from './operation';
  let { state }: { state: OperationState } = $props();
  const message = $derived(operationMessage(state));
</script>

<Notice title={message.title} tone={message.tone}
  ><p>{message.detail}</p></Notice
>
