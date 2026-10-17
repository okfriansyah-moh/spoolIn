<script lang="ts">
  import { onMount } from 'svelte';
  import DraftEditor from '../../../../../../frontend/src/lib/features/draft/DraftEditor.svelte';
  import { DraftController } from '../../../../../../frontend/src/lib/features/draft/controller';
  import type {
    DraftDiscardOutcome,
    DraftMutationOutcome,
    DraftReceipt,
    DraftRecord,
    DraftSource,
  } from '../../../../../../frontend/src/lib/features/draft/types';
  import {
    accountA,
    accountB,
    catalogSubmission,
    draftSession,
  } from './fixture';

  type Mode = 'default' | 'save-fail' | 'account-switch' | 'pending-recover';

  const initial = new URLSearchParams(
    typeof location === 'undefined' ? '' : location.search,
  );
  let mode = $state<Mode>((initial.get('mode') ?? 'default') as Mode);
  let account = $state<'A' | 'B'>(initial.get('account') === 'B' ? 'B' : 'A');
  let saveStarted = $state(false);
  let saveCompleted = $state(false);
  let releaseSave: (() => void) | undefined;

  const source: DraftSource = {
    async recoverOperation(operationId, _csrf, _signal) {
      (globalThis as { __draftRequests?: string[] }).__draftRequests ??= [];
      (globalThis as { __draftRequests: string[] }).__draftRequests.push(
        `recover:${operationId}`,
      );
      const receipt: DraftReceipt = {
        operationId,
        state: 'SUCCEEDED',
        resultId: '40000000-0000-4000-8000-000000000004',
        version: '0',
        resultExpired: false,
      };
      return receipt;
    },
    async listDrafts(_csrf, _signal) {
      (globalThis as { __draftRequests?: string[] }).__draftRequests ??= [];
      (globalThis as { __draftRequests: string[] }).__draftRequests.push(
        'drafts',
      );
      const record: DraftRecord = {
        id: '40000000-0000-4000-8000-000000000004',
        targetKind: 'PERSON',
        targetId: null,
        baseVersion: null,
        values: { name: 'Server draft', biography: null },
        operationId: catalogSubmission,
        version: '8',
        expiresAt: '2099-01-01T00:00:00Z',
      };
      return Object.freeze([record]);
    },
    async saveDraft(operationId, _draftId, _expected, input, _csrf, _signal) {
      saveStarted = true;
      (globalThis as { __draftRequests?: string[] }).__draftRequests ??= [];
      (globalThis as { __draftRequests: string[] }).__draftRequests.push(
        `save:${operationId}:${input.operationId ?? ''}`,
      );
      if (mode === 'save-fail') {
        throw new Error('save failed');
      }
      if (mode === 'pending-recover') {
        await new Promise<void>((resolve) => {
          releaseSave = resolve;
        });
      }
      if (mode === 'account-switch') {
        await new Promise((resolve) => setTimeout(resolve, 200));
      }
      saveCompleted = true;
      const outcome: DraftMutationOutcome = {
        receipt: {
          operationId,
          state: 'SUCCEEDED',
          resultId: '40000000-0000-4000-8000-000000000004',
          version: '0',
          resultExpired: false,
        },
        problemCode: null,
      };
      return outcome;
    },
    async discardDraft(operationId) {
      const outcome: DraftDiscardOutcome = {
        receipt: {
          operationId,
          state: 'SUCCEEDED',
          resultId: '40000000-0000-4000-8000-000000000004',
          version: '1',
          resultExpired: false,
        },
        discarded: true,
        problemCode: null,
      };
      return outcome;
    },
  };

  const controller = new DraftController(source);
  onMount(() => {
    (globalThis as { __draftController?: DraftController }).__draftController =
      controller;
    (globalThis as { __draftReleaseSave?: () => void }).__draftReleaseSave =
      () => releaseSave?.();
  });
  const session = $derived(draftSession(account === 'B' ? accountB : accountA));
</script>

<DraftEditor
  {controller}
  {session}
  target={{ kind: 'PERSON', id: null }}
  catalogOperationId={catalogSubmission}
  initialName=""
/>

<p id="draft-fixture-mode" hidden>{mode}</p>
<p id="draft-save-started" hidden>{String(saveStarted)}</p>
<p id="draft-save-completed" hidden>{String(saveCompleted)}</p>

<div class="fixture-controls">
  <button type="button" data-account="A" onclick={() => (account = 'A')}
    >Account A</button
  >
  <button type="button" data-account="B" onclick={() => (account = 'B')}
    >Account B</button
  >
</div>

<style>
  .fixture-controls {
    display: flex;
    gap: 0.5rem;
    margin-top: 1rem;
  }
</style>
