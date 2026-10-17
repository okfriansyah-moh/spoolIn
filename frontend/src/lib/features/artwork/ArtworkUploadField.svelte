<script lang="ts">
  import { stageLabel, type ArtworkStage } from './progress';
  import { fileToBase64, uploadArtwork } from './api';
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

  let stage = $state<ArtworkStage>('idle');
  let message = $state('');

  async function onFileChange(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || disabled) return;
    stage = 'transferring';
    message = stageLabel(stage);
    try {
      const contentBase64 = await fileToBase64(file);
      stage = 'processing';
      message = stageLabel(stage);
      const operationId = crypto.randomUUID();
      const result = await uploadArtwork(request, csrf, {
        operationId,
        titleId,
        expectedVersion,
        policyRevision,
        contentBase64,
      });
      stage = result.state === 'SUCCEEDED' || result.state === 'PENDING' ? 'saved' : 'failed';
      message = stageLabel(stage);
    } catch {
      stage = 'failed';
      message = stageLabel(stage);
    } finally {
      input.value = '';
    }
  }
</script>

<label class="artwork-upload">
  <span>Poster artwork</span>
  <input type="file" accept="image/png,image/jpeg,image/webp" {disabled} onchange={onFileChange} />
  <p role="status">{message || stageLabel(stage)}</p>
</label>

<style>
  .artwork-upload {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
</style>
