import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vite';
export default defineConfig(({ mode }) => ({
  server: { host: '127.0.0.1', port: 43104, strictPort: true },
  plugins: [
    sveltekit({
      adapter: adapter({
        pages: 'build',
        assets: 'build',
        fallback: '200.html',
      }),
      preprocess: vitePreprocess(),
      ...(mode === 'shell-test'
        ? {
            files: {
              routes: '../build-tools/verification-tests/frontend/src/routes',
            },
          }
        : {}),
    }),
  ],
}));
