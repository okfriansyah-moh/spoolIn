<script lang="ts">
  let {
    sectionLabel,
    sectionHref,
    title,
    description,
    actionHref,
    actionLabel,
  }: {
    sectionLabel: string;
    sectionHref: string;
    title: string;
    description: string;
    actionHref?: string;
    actionLabel?: string;
  } = $props();
</script>

<nav class="breadcrumbs" aria-label="Breadcrumb">
  <a href={sectionHref}>{sectionLabel}</a>
  <span aria-hidden="true">/</span>
  <span aria-current="page">{title}</span>
</nav>
<header class="editor-page-heading">
  <h1 tabindex="-1">{title}</h1>
  <p>{description}</p>
  {#if actionHref && actionLabel}
    <a class="button secondary" href={actionHref}>{actionLabel}</a>
  {/if}
</header>

<style>
  .breadcrumbs {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--primitive-space-2);
    margin-bottom: var(--primitive-space-6);
    color: var(--muted);
    font-size: var(--primitive-type-small);
  }
  .breadcrumbs a {
    color: var(--muted);
  }
  .breadcrumbs [aria-current='page'] {
    color: var(--text);
    font-weight: 600;
  }
  .editor-page-heading {
    display: flex;
    flex-wrap: wrap;
    align-items: end;
    gap: var(--primitive-space-3) var(--primitive-space-4);
    margin-bottom: var(--primitive-space-6);
  }
  .editor-page-heading h1 {
    flex-basis: 100%;
    margin: 0;
    overflow-wrap: anywhere;
  }
  .editor-page-heading p {
    flex: 1 1 20rem;
    margin: 0;
    color: var(--muted);
    font-size: 18px;
  }
  .editor-page-heading .button {
    text-decoration: none;
  }
  @media (max-width: 699px) {
    .editor-page-heading {
      align-items: stretch;
    }
    .editor-page-heading p {
      flex-basis: 100%;
      font-size: 16px;
    }
    .editor-page-heading .button {
      width: 100%;
    }
  }
</style>
