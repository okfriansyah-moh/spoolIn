<script lang="ts">
  import { onMount } from 'svelte';
  import { stageLabel } from './progress';
  import { ArtworkUploadController, INITIAL_ARTWORK_UPLOAD_VIEW } from './upload-controller';
  import type { GraphqlRequest } from '../../transport/graphql';

  let {
    request,
    csrf,
    titleId,
    expectedVersion,
    policyRevision,
    disabled = false,
  }: {
    request: GraphqlRequest;
    csrf: string;
    titleId: string;
    expectedVersion: string;
    policyRevision: string;
    disabled?: boolean;
  } = $props();

  let view = $state(INITIAL_ARTWORK_UPLOAD_VIEW);
  let controller: ArtworkUploadController | null = null;

  onMount(() => {
    controller = new ArtworkUploadController(request, csrf);
    const unsubscribe = controller.subscribe((next) => {
      view = next;
    });
    return () => {
      unsubscribe();
      controller?.dispose();
    };
  });

  async function onFileChange(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || disabled || !controller) return;
    await controller.uploadFile(file, titleId, expectedVersion, policyRevision);
    input.value = '';
  }

  function onCancel() {
    void controller?.cancelActive(view.operationId);
  }

  function onResume() {
    if (!view.operationId) return;
    void controller?.resumeCheck(view.operationId, titleId, expectedVersion);
  }
</script>

<label class="artwork-upload">
  <span>Poster artwork</span>
  <input
    type="file"
    accept="image/png,image/jpeg,image/webp"
    {disabled}
    onchange={onFileChange}
  />
  {#if view.stage === 'transferring'}
    <progress max="100" value={view.transferPercent} aria-label="Transfer progress"></progress>
  {/if}
  <p role="status">{view.message || stageLabel(view.stage)}</p>
  {#if view.canCancel}
    <button type="button" class="secondary" onclick={onCancel}>Cancel upload</button>
  {/if}
  {#if view.canResumeCheck}
    <button type="button" class="secondary" onclick={onResume}>Check original result</button>
  {/if}
</label>

<style>
  .artwork-upload {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  progress {
    width: 100%;
  }
</style>
