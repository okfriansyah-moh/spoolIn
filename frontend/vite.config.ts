import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vite';

// T004 replaces the build fixture with the production route sources.
export default defineConfig({
  plugins: [
    sveltekit({
      version: { name: '0.0.0-foundation' },
      adapter: adapter({
        pages: 'build',
        assets: 'build',
        fallback: '200.html',
      }),
      preprocess: vitePreprocess(),
    }),
  ],
});
