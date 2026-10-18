<script lang="ts">
  import heroArtwork from '../../lib/design-system/artwork/hero.png';
  import AccountForm from '../../lib/features/session/AccountForm.svelte';
</script>

<div class="account-screen">
  <aside class="account-art" aria-hidden="true">
    <img src={heroArtwork} alt="" />
    <div class="account-art-copy">
      <h2>Every story<br />has a way in.</h2>
      <p>Explore freely. Sign in when you need your account.</p>
    </div>
  </aside>
  <div class="account-form-panel">
    <AccountForm mode="signIn" />
  </div>
</div>

<style>
  .account-screen {
    display: grid;
    grid-template-columns: 1.05fr 1fr;
    min-height: 720px;
    max-width: 1440px;
    margin: auto;
    overflow: hidden;
  }
  .account-art {
    position: relative;
    min-height: 700px;
    background: var(--primitive-image-scrim);
    overflow: hidden;
  }
  .account-art img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: 66% center;
  }
  .account-art::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      0deg,
      var(--primitive-image-scrim),
      transparent 85%
    );
  }
  .account-art-copy {
    position: absolute;
    inset: auto var(--primitive-space-12) 64px;
    z-index: 1;
    color: var(--primitive-image-ink);
  }
  .account-art-copy h2 {
    font-family: var(--font-display);
    font-size: 48px;
    line-height: 1.1;
    font-weight: 500;
    margin: 0 0 var(--primitive-space-6);
  }
  .account-art-copy p {
    color: var(--primitive-image-muted);
    max-width: 25ch;
    margin: 0;
  }
  .account-form-panel {
    align-self: center;
    min-width: 0;
    width: 100%;
    max-width: 540px;
    margin-inline: auto;
    padding: 62px var(--primitive-space-12);
  }
  .account-form-panel :global(.account-layout) {
    display: block;
    max-inline-size: none;
    margin: 0;
    padding-block: 0;
  }
  .account-form-panel :global(h1) {
    font-size: 40px;
    margin-bottom: var(--primitive-space-6);
  }
  .account-form-panel :global(form.panel) {
    padding: 0;
    border: 0;
    background: transparent;
  }
  .account-form-panel :global(form > .field) {
    margin-block: 0 var(--primitive-space-2);
  }
  .account-form-panel :global(label) {
    font-size: 14px;
  }
  .account-form-panel :global(input) {
    min-height: 48px;
  }
  .account-form-panel :global(.actions) {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: var(--primitive-space-3);
    margin-top: var(--primitive-space-4);
  }
  .account-form-panel :global(.actions button) {
    width: 100%;
  }
  .account-form-panel :global(form > p) {
    font-size: var(--primitive-type-small);
    color: var(--muted);
  }
  .account-form-panel :global(.notice) {
    margin: 0;
  }
  @media (max-width: 1000px) {
    .account-form-panel {
      padding-inline: var(--primitive-space-6);
    }
    .account-art-copy {
      left: var(--primitive-space-6);
      right: var(--primitive-space-6);
    }
    .account-art-copy h2 {
      font-size: 40px;
    }
  }
  @media (max-width: 699px) {
    .account-screen {
      grid-template-columns: minmax(0, 1fr);
      min-height: 0;
    }
    .account-art {
      min-height: 190px;
      max-height: 190px;
    }
    .account-art-copy {
      left: 23px;
      right: 23px;
      bottom: var(--primitive-space-6);
    }
    .account-art-copy h2 {
      font-size: 30px;
      margin: 0;
    }
    .account-art-copy p {
      display: none;
    }
    .account-form-panel {
      padding: var(--primitive-space-8) 22px;
    }
    .account-form-panel :global(h1) {
      font-size: 36px;
    }
  }
</style>
