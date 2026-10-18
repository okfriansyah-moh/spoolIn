<script lang="ts">
  import { onMount } from 'svelte';
  import ArchiveAction from '../../../../lib/features/archive/ArchiveAction.svelte';
  import { page } from '$app/state';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createPersonEditorController } from '../../../../lib/features/person/write/editor-controller';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import PersonEditorForm from '../../../../lib/features/person/write/PersonEditorForm.svelte';
  import EditorPageHeading from '../../../../lib/components/EditorPageHeading.svelte';
  import type { EditorState } from '../../../../lib/features/person/write/types';
  import { isRecordId } from '../../../../lib/components/safe-url';

  const sessionCtl = sessionController();
  let editorState = $state<EditorState | null>(null);
  let ready = $state(false);
  let controller: ReturnType<typeof createPersonEditorController> | null = null;
  const personId = $derived(page.params.id ?? '');

  let mounted = $state(false);
  let bootstrapError = $state('');
  let cleanupEditor = () => {};
  onMount(() => {
    mounted = true;
    return () => cleanupEditor();
  });
  $effect(() => {
    if (mounted) loadEditor(personId);
  });

  function loadEditor(id: string) {
    cleanupEditor();
    ready = false;
    editorState = null;
    bootstrapError = '';
    if (!isRecordId(id)) {
      ready = true;
      return;
    }
    const transport = createTransport();
    const next = createPersonEditorController(
      transport,
      createDraftSource(transport),
      'edit',
      id,
    );
    controller = next;
    const unsubscribe = next.subscribe((state) => {
      editorState = state;
    });
    const abort = new AbortController();
    cleanupEditor = () => {
      abort.abort();
      unsubscribe();
      next.dispose();
    };
    void bootstrapEditor(next, abort.signal);
  }

  async function bootstrapEditor(
    next: NonNullable<typeof controller>,
    signal: AbortSignal,
  ) {
    try {
      const context = await loadWriteContext(signal);
      const policy = sessionCtl.snapshot.policy;
      if (signal.aborted) return;
      if (!context || !policy) {
        bootstrapError =
          'Check your Maintainer session before loading this editor.';
        return;
      }
      await next.bootstrap(policy, context, signal);
      if (!signal.aborted) ready = true;
    } catch {
      if (!signal.aborted)
        bootstrapError = 'Person editor could not load. Retry the read.';
    }
  }
</script>

<svelte:head><title>Edit person · SpoolIn</title></svelte:head>
<section class="person-editor-page">
  <EditorPageHeading
    sectionLabel="People"
    sectionHref={resolve('/manage/people')}
    title={editorState?.saved?.name ?? 'Edit person'}
    description="Update this person’s public name and biography."
    actionHref={editorState?.saved
      ? resolve('/people/[id]', { id: editorState.saved.id })
      : undefined}
    actionLabel={editorState?.saved ? 'Review public record' : undefined}
  />

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
      onRebase={(reviewed) => void controller?.rebaseAfterReview(reviewed)}
      onSelectDraft={(id) => void controller?.selectRetainedDraft(id)}
      onStartNewDraft={() => controller?.startNewDraft()}
      onDiscardOpen={() => controller?.openDiscardDialog()}
      onDiscardClose={() => controller?.closeDiscardDialog()}
      onDiscardConfirm={() => void controller?.confirmDiscard()}
      onDraftChange={(field, value) => {
        if (!controller || !editorState) return;
        controller.updateDraft({ ...editorState.draft, [field]: value });
      }}
    />
    {#if editorState.saved?.state === 'ACTIVE'}
      <ArchiveAction
        kind="person"
        id={editorState.saved.id}
        version={editorState.saved.version}
        disabled={editorState.phase === 'submitting'}
      />
    {/if}
  {:else if bootstrapError}
    <p role="alert">{bootstrapError}</p>
    <button type="button" onclick={() => loadEditor(personId)}
      >Retry person editor</button
    >
  {:else}
    <p role="status">Loading person editor…</p>
  {/if}
</section>

<style>
  .person-editor-page {
    width: 100%;
    max-width: 52rem;
    margin-inline: auto;
  }
</style>
