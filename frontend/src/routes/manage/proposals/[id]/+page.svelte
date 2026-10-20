<script lang="ts">
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { resolve } from '$app/paths';
  import SafeLink from '../../../../lib/components/SafeLink.svelte';
  import { isRecordId } from '../../../../lib/components/safe-url';
  import { sessionController } from '../../../../lib/features/session/context';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import { fileToBase64 } from '../../../../lib/features/artwork/api';
  import { parseProposal, parseDecision, posterStillProcessing, proposalRequest, proposalValue, type ProposalReview } from '../../../../lib/features/proposal/api';
  import { TransportFailure } from '../../../../lib/transport/graphql';

  const session = sessionController();
  const id = $derived(page.params.id ?? '');
  const revisionQuery = $derived(page.url.searchParams.get('revision'));
  let proposal = $state<ProposalReview | null>(null);
  let csrf = $state('');
  let loading = $state(true);
  let busy = $state(false);
  let message = $state('');
  let uncertain = $state(false);
  let selectedFile = $state<File | null>(null);
  let sourceUrl = $state('');
  let reuseBasis = $state('');
  let posterOperationId = $state<string | null>(null);
  let pendingUploadBody = $state<Record<string, unknown> | null>(null);
  let processingPoster = $state(false);
  const reviewable = $derived(proposal?.state === 'REVIEW' && !uncertain);
  const fields = [
    ['title', 'Title'], ['originalTitle', 'Original title'], ['synopsis', 'Synopsis'],
    ['genreIds', 'Genres'], ['release', 'Release date'], ['runtimeMinutes', 'Runtime in minutes'],
    ['originalLanguage', 'Original language'], ['synopsisLanguage', 'Synopsis language'], ['countries', 'Countries'],
  ];

  function errorMessage(error: unknown): string {
    if (error instanceof TransportFailure) {
      if (error.kind === 'uncertain') return 'The result is not confirmed. Reload this proposal before making another change.';
      if (error.code === 'CONFLICT') return 'This revision has changed. Reload the proposal and review its current values.';
      if (error.code === 'UNAUTHENTICATED') return 'Your session expired. Sign in again to continue.';
      if (error.code === 'FORBIDDEN') return 'This action requires a current Maintainer session. Reload before retrying.';
    }
    return 'The proposal could not be loaded or changed. Reload to check its current state.';
  }

  $effect(() => {
    const selectedId = id;
    const revision = revisionQuery;
    const abort = new AbortController();
    proposal = null;
    csrf = '';
    loading = true;
    message = '';
    uncertain = false;
    selectedFile = null;
    posterOperationId = null;
    pendingUploadBody = null;
    processingPoster = false;
    void (async () => {
      try {
        if (!isRecordId(selectedId) || (revision !== null && !/^[1-9][0-9]*$/.test(revision))) throw new Error('Invalid link');
        const context = await loadWriteContext(abort.signal);
        if (!context) throw new TransportFailure('rejected', 'FORBIDDEN');
        const data = await proposalRequest(selectedId, revision ? `?revision=${encodeURIComponent(revision)}` : '', { signal: abort.signal });
        if (abort.signal.aborted) return;
        const loaded = parseProposal(data);
        if (loaded.proposalId !== selectedId || (revision && loaded.revision !== Number(revision))) throw new TransportFailure('rejected', 'CONFLICT');
        proposal = loaded;
        csrf = context.csrf;
      } catch (error) {
        if (!abort.signal.aborted) message = errorMessage(error);
      } finally {
        if (!abort.signal.aborted) loading = false;
      }
    })();
    return () => abort.abort();
  });

  async function decide(action: 'approve' | 'reject') {
    if (!proposal || !reviewable || busy || processingPoster) return;
    busy = true;
    message = '';
    try {
      const result = await proposalRequest(id, `/${action}`, { csrf, body: {
        revision: proposal.revision, contentHash: proposal.contentHash, challenge: proposal.challenge,
      } });
      const outcome = parseDecision(result);
      uncertain = true;
      message = action === 'reject' && outcome.state === 'SUCCEEDED'
        ? 'Proposal rejected. Nothing was published.'
        : outcome.state === 'SUCCEEDED'
          ? 'Movie created and its poster attached, if provided. Reload to view the current proposal state.'
          : outcome.problemCode
            ? `The operation returned ${outcome.problemCode}. Reload before continuing.`
            : 'The operation is not yet complete. Reload to check the persisted result.';
    } catch (error) {
      uncertain = true;
      message = errorMessage(error);
    } finally { busy = false; }
  }

  async function replacePoster() {
    if (!proposal || !selectedFile || !reviewable || busy) return;
    const maximum = Number(session.snapshot.policy?.artwork.maxInputBytes);
    if (!Number.isFinite(maximum) || selectedFile.size > maximum) {
      message = 'This image exceeds the current upload limit, or the artwork policy is unavailable.';
      return;
    }
    busy = true;
    message = '';
    try {
      posterOperationId ??= crypto.randomUUID();
      pendingUploadBody ??= {
        operationId: posterOperationId, revision: proposal.revision, contentHash: proposal.contentHash,
        content: await fileToBase64(selectedFile), mediaType: selectedFile.type,
        sourceUrl: sourceUrl.trim(), reuseBasis: reuseBasis.trim(),
      };
      const result = await proposalRequest(id, '/poster', { csrf, body: pendingUploadBody });
      if (posterStillProcessing(result)) {
        processingPoster = true;
        message = 'The poster is processing. Select Check poster processing to resume this upload and create the new revision when it is ready.';
        return;
      }
      processingPoster = false;
      const updated = parseProposal(result);
      if (updated.proposalId !== id || updated.revision <= proposal.revision) throw new TransportFailure('uncertain');
      await goto(resolve(`/manage/proposals/${id}?revision=${updated.revision}`));
    } catch (error) {
      uncertain = true;
      message = errorMessage(error);
    } finally { busy = false; }
  }
</script>

<svelte:head><title>Review movie proposal · SpoolIn</title></svelte:head>
<section class="proposal-page">
  <header><a href={resolve('/manage/movies')}>Movies</a><h1>Review movie proposal</h1><p>Review the exact movie details, sources, and poster before approving this revision.</p></header>
  {#if loading}<p role="status">Loading proposal…</p>{/if}
  {#if message}<p role="status" class="panel">{message}</p>{/if}
  {#if proposal}
    <div class="proposal-summary"><p>Revision {proposal.revision} · {proposal.state}</p><p>Expires: {proposal.expiresAt}</p></div>
    <div class="review-grid">
      <section class="panel"><h2>Movie details</h2><dl class="metadata">
        {#each fields as [key, label]}<div><dt>{label}</dt><dd>{proposalValue(proposal.values[key])}</dd></div>{/each}
      </dl><h2>Sources</h2>
      {#if proposal.sources.length}<ul>{#each proposal.sources as source}<li><strong>{source.field}</strong>: <SafeLink url={source.url} text={source.url} />{#if source.note}<p>{source.note}</p>{/if}</li>{/each}</ul>{:else}<p>No source evidence was provided.</p>{/if}
      </section>
      <section class="panel"><h2>Poster</h2>
        {#if proposal.poster}<img class="poster" src={proposal.poster.url} alt={`Proposed poster for ${proposalValue(proposal.values.title)}`} width={proposal.poster.width} height={proposal.poster.height} /><p>{proposal.poster.width} × {proposal.poster.height} · {proposal.poster.mediaType}</p>{:else}<p>No poster is included.</p>{/if}
        {#if reviewable}<form onsubmit={(event) => { event.preventDefault(); void replacePoster(); }}>
          <label for="poster-file">Upload a poster</label><input id="poster-file" type="file" accept="image/*" disabled={busy || processingPoster} onchange={(event) => { selectedFile = event.currentTarget.files?.[0] ?? null; posterOperationId = null; }} />
          <label for="poster-source">Image source URL</label><input id="poster-source" type="url" bind:value={sourceUrl} disabled={busy || processingPoster} />
          <label for="poster-reuse">Permission or reuse basis</label><textarea id="poster-reuse" bind:value={reuseBasis} disabled={busy || processingPoster}></textarea>
          <p>Uploading creates a new revision. Review the new poster and details before approval.</p><button type="submit" disabled={busy || !selectedFile}>{processingPoster ? 'Check poster processing' : 'Upload and revise proposal'}</button>
        </form>{/if}
      </section>
    </div>
    <section class="panel"><h2>Decision</h2><p>Approval creates the movie using this exact revision. Its poster must finish attaching before the operation is complete.</p>
      {#if proposal.state !== 'REVIEW'}<p>This proposal is {proposal.state.toLowerCase()}. Approval and replacement are unavailable.</p>{/if}
      <div class="actions"><button type="button" disabled={!reviewable || busy || processingPoster} aria-busy={busy} onclick={() => void decide('approve')}>Approve exact revision</button><button type="button" class="secondary" disabled={!reviewable || busy || processingPoster} onclick={() => void decide('reject')}>Reject</button></div>
      <p class="revision">Content digest: {proposal.contentHash}</p>
    </section>
  {/if}
  <button class="secondary" type="button" disabled={busy || loading} onclick={() => window.location.reload()}>Reload proposal</button>
</section>

<style>
  .proposal-page { display: grid; gap: var(--primitive-space-5); width: 100%; }
  header p, .proposal-summary { color: var(--muted); }
  h1 { font-size: clamp(28px, 4vw, 36px); }
  .review-grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(240px, 1fr); gap: var(--primitive-space-5); align-items: start; }
  .panel { min-width: 0; }
  dd, li, .revision { overflow-wrap: anywhere; white-space: pre-wrap; }
  li { margin-block: var(--primitive-space-3); }
  .poster { display: block; width: 100%; max-width: 320px; height: auto; }
  form { display: grid; gap: var(--primitive-space-3); margin-top: var(--primitive-space-5); }
  .actions { display: flex; gap: var(--primitive-space-3); flex-wrap: wrap; }
  .revision { color: var(--muted); font-size: var(--primitive-type-small); }
  @media (max-width: 760px) { .review-grid { grid-template-columns: minmax(0, 1fr); } }
</style>
