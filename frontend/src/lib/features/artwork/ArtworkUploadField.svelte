<script lang="ts">
  import { onMount } from 'svelte';
  import { stageLabel } from './progress';
  import {
    ArtworkUploadController,
    INITIAL_ARTWORK_UPLOAD_VIEW,
  } from './upload-controller.svelte';
  import type { Policy } from '../../policy/types';
  import { prepareArtworkFile } from './prepare-file';
  import type { GraphqlRequest } from '../../transport/graphql';

  let controller = $state.raw<ArtworkUploadController | null>(null);
  let {
    request,
    csrf,
    titleId,
    expectedVersion,
    policyRevision,
    disabled = false,
    artworkPolicy,
    onAttached,
    onStateChange,
  }: {
    request: GraphqlRequest;
    csrf: string;
    titleId: string;
    expectedVersion: string;
    policyRevision: string;
    disabled?: boolean;
    artworkPolicy?: Policy['artwork'];
    onAttached?: () => Promise<void>;
    onStateChange?: (state: {
      blocked: boolean;
      busy: boolean;
      message: string;
      saved: boolean;
    }) => void;
  } = $props();

  const inputId = $props.id();
  let validationMessage = $state('');
  let preparationMessage = $state('');
  let refreshing = $state(false);
  let validating = $state(false);
  let refreshNeeded = $state(false);
  let notifiedOperation: string | null = null;
  let mounted = false;
  const view = $derived(controller?.snapshot ?? INITIAL_ARTWORK_UPLOAD_VIEW);
  const uploadBlocked = $derived(
    disabled ||
      validating ||
      refreshing ||
      refreshNeeded ||
      ['transferring', 'processing', 'uncertain'].includes(view.stage),
  );

  $effect(() => {
    const active = ['transferring', 'processing', 'uncertain'].includes(
      view.stage,
    );
    const blocked = validating || refreshing || refreshNeeded || active;
    const message = validating
      ? 'Wait for artwork preparation to finish.'
      : refreshNeeded
        ? 'Select Retry movie details before saving the movie.'
        : refreshing
          ? 'Wait for the saved artwork details to refresh.'
          : view.stage === 'uncertain'
            ? 'Check the original artwork result before saving the movie.'
            : active
              ? 'Wait for artwork transfer and processing to finish.'
              : '';
    onStateChange?.({
      blocked,
      busy: validating || refreshing || active,
      message,
      saved: view.stage === 'saved' && !blocked,
    });
  });

  onMount(() => {
    mounted = true;
    controller = new ArtworkUploadController(request, csrf);
    controller.onSaved = (next) => {
      if (
        next.stage === 'saved' &&
        next.operationId &&
        next.operationId !== notifiedOperation
      ) {
        notifiedOperation = next.operationId;
        void refreshParent();
      }
    };
    return () => {
      mounted = false;
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
    validating = true;
    preparationMessage = '';
    validationMessage = 'Preparing artwork for upload…';
    try {
      const prepared = await prepareArtworkFile(file, artworkPolicy);
      if (!mounted || disabled) return;
      preparationMessage = prepared.message;
      validationMessage = '';
      validating = false;
      await controller.uploadFile(
        prepared.file,
        titleId,
        expectedVersion,
        policyRevision,
      );
    } catch (error) {
      if (mounted)
        validationMessage =
          error instanceof Error
            ? error.message
            : 'This image could not be prepared. Choose another image.';
    } finally {
      validating = false;
      input.value = '';
    }
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
  {#if preparationMessage}<p>{preparationMessage}</p>{/if}
  {#if view.stage === 'transferring'}
    <progress
      max="100"
      value={view.transferPercent}
      aria-label="Transfer progress"
    ></progress>
  {/if}
  <p role="status">
    {validationMessage ||
      (view.stage === 'saved' && !refreshing && !refreshNeeded
        ? 'Artwork saved. Select Save movie below to finish.'
        : view.message || stageLabel(view.stage))}
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
