<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createPersonEditorController } from '../../../../lib/features/person/write/editor-controller';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import PersonEditorForm from '../../../../lib/features/person/write/PersonEditorForm.svelte';
  import type { EditorState } from '../../../../lib/features/person/write/types';
  import { isRecordId } from '../../../../lib/components/safe-url';

  const sessionCtl = sessionController();
  let editorState = $state<EditorState | null>(null);
  let ready = $state(false);
  let controller: ReturnType<typeof createPersonEditorController> | null = null;
  const personId = $derived(page.params.id ?? '');

  onMount(() => {
    if (!isRecordId(personId)) {
      ready = true;
      return;
    }
    const transport = createTransport();
    controller = createPersonEditorController(
      transport,
      createDraftSource(transport),
      'edit',
      personId,
    );
    const unsubscribe = controller.subscribe((next) => {
      editorState = next;
    });
    const abort = new AbortController();
    void (async () => {
      const context = await loadWriteContext(abort.signal);
      const policy = sessionCtl.snapshot.policy;
      if (!context || !policy || abort.signal.aborted) return;
      await controller?.bootstrap(policy, context, abort.signal);
      ready = true;
    })();
    return () => {
      abort.abort();
      unsubscribe();
      controller?.dispose();
    };
  });
</script>

<svelte:head><title>Edit person · SpoolIn</title></svelte:head>
<h1 tabindex="-1">{editorState?.saved?.name ?? 'Edit person'}</h1>
<p class="muted">
  <a href={resolve('/manage/people')}>People</a>
  {#if editorState?.saved}
    · <a href={resolve('/people/[id]', { id: editorState.saved.id })}
      >Review public record</a
    >
  {/if}
</p>

{#if !isRecordId(personId)}
  <p role="alert">This person link is not valid.</p>
{:else if ready && editorState && sessionCtl.snapshot.policy}
  <PersonEditorForm
    state={editorState}
    policy={sessionCtl.snapshot.policy}
    disabled={editorState.phase === 'target-changed'}
    onSubmit={() => void controller?.submit()}
    onCheckOriginal={() => void controller?.checkOriginalResult()}
    onReviewStale={() => controller?.toggleStaleReview()}
    onDraftChange={(field, value) => {
      if (!controller || !editorState) return;
      controller.updateDraft({ ...editorState.draft, [field]: value });
    }}
  />
{:else}
  <p role="status">Loading person editor…</p>
{/if}

<style>
  .muted {
    margin-bottom: 1rem;
  }
</style>
