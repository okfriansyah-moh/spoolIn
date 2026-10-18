<script lang="ts">
  import { onMount } from 'svelte';
  import ArchiveAction from '../../../../lib/features/archive/ArchiveAction.svelte';
  import { page } from '$app/state';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createMovieEditorController } from '../../../../lib/features/movie/editor-controller';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import MovieEditorForm from '../../../../lib/features/movie/MovieEditorForm.svelte';
  import type {
    EditorState,
    WriteContext,
  } from '../../../../lib/features/movie/types';
  import ConfirmDialog from '../../../../lib/components/ConfirmDialog.svelte';
  import { isRecordId } from '../../../../lib/components/safe-url';
  import CreditPanel from '../../../../lib/features/credit/CreditPanel.svelte';
  import ArtworkUploadField from '../../../../lib/features/artwork/ArtworkUploadField.svelte';
  import { applyMovieDraftField } from '../../../../lib/features/movie/draft-field-update';

  const sessionCtl = sessionController();
  let editorState = $state<EditorState | null>(null);
  let writeContext = $state<WriteContext | null>(null);
  let creditTitleVersion = $state<string | null>(null);
  let ready = $state(false);
  let controller: ReturnType<typeof createMovieEditorController> | null = null;
  const movieId = $derived(page.params.id ?? '');

  onMount(() => {
    if (!isRecordId(movieId)) {
      ready = true;
      return;
    }
    const transport = createTransport();
    controller = createMovieEditorController(
      transport,
      createDraftSource(transport),
      'edit',
      movieId,
    );
    const unsubscribe = controller.subscribe((next) => {
      editorState = next;
    });
    const abort = new AbortController();
    void (async () => {
      const context = await loadWriteContext(abort.signal);
      const policy = sessionCtl.snapshot.policy;
      if (!context || !policy || abort.signal.aborted) return;
      writeContext = context;
      await controller?.bootstrap(policy, context, abort.signal);
      ready = true;
    })();
    return () => {
      abort.abort();
      unsubscribe();
      controller?.dispose();
    };
  });

  function draftChange(
    field: import('$lib/features/movie/draft-field-update').MovieDraftField,
    value: string,
  ) {
    if (!controller || !editorState) return;
    const update = applyMovieDraftField(editorState.draft, field, value);
    controller.updateDraft(
      update.draft,
      update.originalTitleTouched ? { originalTitleTouched: true } : {},
    );
  }
</script>

<svelte:head><title>Edit movie · SpoolIn</title></svelte:head>
<h1 tabindex="-1">{editorState?.saved?.title ?? 'Edit movie'}</h1>
<p class="muted">
  <a href={resolve('/manage/movies')}>Movies</a>
  {#if editorState?.saved}
    · <a href={resolve('/titles/[id]', { id: editorState.saved.id })}
      >Review public record</a
    >
  {/if}
</p>

{#if !isRecordId(movieId)}
  <p role="alert">This movie link is not valid.</p>
{:else if ready && editorState && sessionCtl.snapshot.policy}
  <MovieEditorForm
    state={editorState}
    policy={sessionCtl.snapshot.policy}
    disabled={editorState.phase === 'target-changed'}
    onSubmit={() => void controller?.submit()}
    onCheckOriginal={() => void controller?.checkOriginalResult()}
    onReviewStale={() => controller?.toggleStaleReview()}
    onRebase={(reviewed) => void controller?.rebaseAfterReview(reviewed)}
    onSelectDraft={(id) => void controller?.selectRetainedDraft(id)}
    onStartNewDraft={() => controller?.startNewDraft()}
    onDraftChange={draftChange}
    onGenreToggle={(id, checked) => controller?.toggleGenre(id, checked)}
    onDiscardOpen={() => controller?.openDiscardDialog()}
  />
  {#if editorState.saved && writeContext && sessionCtl.snapshot.policy}
    <ArtworkUploadField
      request={createTransport()}
      csrf={writeContext.csrf}
      titleId={editorState.saved.id}
      expectedVersion={editorState.saved.version}
      policyRevision={sessionCtl.snapshot.policy.revision}
      disabled={editorState.phase === 'target-changed'}
    />
    {@const version = creditTitleVersion ?? editorState.saved.version}
    <CreditPanel
      titleId={editorState.saved.id}
      titleVersion={version}
      policy={sessionCtl.snapshot.policy}
      context={writeContext}
      draftSource={createDraftSource(createTransport())}
      onTitleVersionChange={(next) => {
        creditTitleVersion = next;
      }}
    />
  {/if}
  {#if editorState.saved?.state === 'ACTIVE'}
    <ArchiveAction
      kind="movie"
      id={editorState.saved.id}
      version={editorState.saved.version}
      disabled={editorState.phase === 'submitting'}
    />
  {/if}
{:else}
  <p role="status">Loading movie editor…</p>
{/if}

<ConfirmDialog
  open={editorState?.discardDialogOpen ?? false}
  title="Leave this draft?"
  action="Discard draft"
  oncancel={() => controller?.closeDiscardDialog()}
  onconfirm={() => void controller?.confirmDiscard()}
>
  <p>
    Leaving discards the current unsaved form values. Saved movie information
    stays unchanged.
  </p>
</ConfirmDialog>

<style>
  .muted {
    margin-bottom: 1rem;
  }
</style>
