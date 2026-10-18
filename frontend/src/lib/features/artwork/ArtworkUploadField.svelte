<script lang="ts">
  import { onMount } from 'svelte';
  import { stageLabel } from './progress';
  import {
    ArtworkUploadController,
    INITIAL_ARTWORK_UPLOAD_VIEW,
  } from './upload-controller';
  import type { Policy } from '../../policy/types';
  import { validateArtworkFile } from './file-validation';
  import type { GraphqlRequest } from '../../transport/graphql';

  let {
    request,
    csrf,
    titleId,
    expectedVersion,
    policyRevision,
    disabled = false,
    artworkPolicy,
  }: {
    request: GraphqlRequest;
    csrf: string;
    titleId: string;
    expectedVersion: string;
    policyRevision: string;
    disabled?: boolean;
    artworkPolicy?: Policy['artwork'];
  } = $props();

  const inputId = $props.id();
  let validationMessage = $state('');
  let view = $state(INITIAL_ARTWORK_UPLOAD_VIEW);
  let controller: ArtworkUploadController | null = null;
  const uploadBlocked = $derived(
    disabled || ['transferring', 'processing', 'uncertain'].includes(view.stage),
  );

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
    const input = event.currentTarget;
    if (!(input instanceof HTMLInputElement)) return;
    const file = input.files?.[0];
    if (!file || uploadBlocked || !controller) return;
    validationMessage = validateArtworkFile(file, artworkPolicy);
    if (validationMessage) { input.value = ''; return; }
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

<section class="artwork-upload" aria-label="Poster artwork upload">
  <label for={inputId}>Poster artwork</label>
  <input
    id={inputId}
    type="file"
    accept="image/png,image/jpeg,image/webp"
    disabled={uploadBlocked}
    onchange={onFileChange}
  />
  {#if view.stage === 'transferring'}
    <progress
      max="100"
      value={view.transferPercent}
      aria-label="Transfer progress"
    ></progress>
  {/if}
  <p role="status">{validationMessage || view.message || stageLabel(view.stage)}</p>
  {#if view.canCancel}
    <button type="button" class="secondary" onclick={onCancel}
      >Cancel upload</button
    >
  {/if}
  {#if view.canResumeCheck}
    <button type="button" class="secondary" onclick={onResume}
      >Check original result</button
    >
  {/if}
</section>

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
