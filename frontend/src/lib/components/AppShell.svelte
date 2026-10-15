<script lang="ts">
  import { resolve } from '$app/paths';
  import { onMount, type Snippet } from 'svelte';
  import Brand from './Brand.svelte';
  import { navigation, type Capabilities } from '../state/navigation';
  import {
    browserStorage,
    readThemePreference,
    selectTheme,
    type Theme,
  } from '../state/theme';
  let {
    children,
    capabilities = { verified: false, manageCatalog: false },
    currentPath = '/',
  }: {
    children: Snippet;
    capabilities?: Capabilities;
    currentPath?: string;
  } = $props();
  let theme = $state<Theme>('dark');
  let explicit = false;
  onMount(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const preference = readThemePreference(browserStorage());
    explicit = preference !== undefined;
    theme = preference ?? (media.matches ? 'dark' : 'light');
    const followSystem = () => {
      if (!explicit) theme = media.matches ? 'dark' : 'light';
    };
    media.addEventListener('change', followSystem);
    return () => {
      media.removeEventListener('change', followSystem);
    };
  });
  $effect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
  });
  function toggleTheme() {
    theme = selectTheme(
      theme,
      theme === 'dark' ? 'light' : 'dark',
      browserStorage(),
    );
    explicit = true;
  }
</script>

<a class="skip-link" href="#main-content">Skip to content</a>
<header class="app-header">
  <a href={resolve('/')} aria-label="SpoolIn home"><Brand /></a>
  <nav aria-label="Main navigation">
    {#each navigation(capabilities) as item (item.href)}
      <!-- eslint-disable svelte/no-navigation-without-resolve -- Downstream route owners provide these declared routes; prefix the actual base without claiming generated routes exist. -->
      <a
        href={`${resolve('/')}${item.href.slice(1)}`}
        aria-current={currentPath === item.href ? 'page' : undefined}
        >{item.label}</a
      ><!-- eslint-enable svelte/no-navigation-without-resolve -->{/each}
  </nav>
  <button
    class="secondary"
    onclick={toggleTheme}
    aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
    >Switch theme</button
  >
</header>
<main id="main-content" tabindex="-1" class="container">
  {@render children()}
</main>
