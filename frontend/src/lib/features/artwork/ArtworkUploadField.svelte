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
    onAttached,
  }: {
    request: GraphqlRequest;
    csrf: string;
    titleId: string;
    expectedVersion: string;
    policyRevision: string;
    disabled?: boolean;
    artworkPolicy?: Policy['artwork'];
    onAttached?: () => Promise<void>;
  } = $props();

  const inputId = $props.id();
  let validationMessage = $state('');
  let refreshing = $state(false);
  let refreshNeeded = $state(false);
  let notifiedOperation: string | null = null;
  let mounted = false;
  let view = $state(INITIAL_ARTWORK_UPLOAD_VIEW);
  let controller: ArtworkUploadController | null = null;
  const uploadBlocked = $derived(
    disabled ||
      refreshing ||
      refreshNeeded ||
      ['transferring', 'processing', 'uncertain'].includes(view.stage),
  );

  onMount(() => {
    mounted = true;
    controller = new ArtworkUploadController(request, csrf);
    const unsubscribe = controller.subscribe((next) => {
      view = next;
      if (
        next.stage === 'saved' &&
        next.operationId &&
        next.operationId !== notifiedOperation
      ) {
        notifiedOperation = next.operationId;
        void refreshParent();
      }
    });
    return () => {
      mounted = false;
      unsubscribe();
      controller?.dispose();
    };
  });

  async function refreshParent() {
    if (!onAttached || refreshing) return;
    refreshing = true;
    try {
      await onAttached();
      if (mounted) {
        refreshNeeded = false;
        validationMessage = '';
      }
    } catch {
      if (mounted) {
        refreshNeeded = true;
        validationMessage =
          'Artwork attached. Current movie details could not be refreshed. Retry details before another upload.';
      }
    } finally {
      if (mounted) refreshing = false;
    }
  }

  async function onFileChange(event: Event) {
    const input = event.currentTarget;
    if (!(input instanceof HTMLInputElement)) return;
    const file = input.files?.[0];
    if (!file || uploadBlocked || !controller) return;
    validationMessage = validateArtworkFile(file, artworkPolicy);
    if (validationMessage) {
      input.value = '';
      return;
    }
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
  <p role="status">
    {validationMessage || view.message || stageLabel(view.stage)}
  </p>
  {#if refreshNeeded}
    <button
      type="button"
      class="secondary"
      disabled={refreshing}
      onclick={() => void refreshParent()}>Retry movie details</button
    >
  {/if}
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
    gap: var(--primitive-space-3);
    max-width: 48rem;
    padding-block: var(--primitive-space-8);
    margin-top: var(--primitive-space-6);
    border-top: 1px solid var(--line);
  }
  label {
    font-size: 25px;
    font-weight: 600;
    letter-spacing: -0.02em;
  }
  input[type='file'] {
    padding: var(--primitive-space-6);
    background: var(--surface);
    border: 1px dashed var(--line);
    border-radius: var(--radius);
    max-width: 100%;
  }
  input[type='file']::file-selector-button {
    margin-right: var(--primitive-space-3);
    padding: var(--primitive-space-2) var(--primitive-space-3);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: var(--raised);
    color: var(--text);
    font: inherit;
    cursor: pointer;
  }
  p {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
  }
  button {
    align-self: start;
  }
  progress {
    width: 100%;
  }
</style>
