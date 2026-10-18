import { build } from 'vite';
import { copyFile, mkdir } from 'node:fs/promises';
import { fileURLToPath, URL } from 'node:url';
const root = fileURLToPath(new URL('../', import.meta.url));
await build({ configFile: false, root, build: {
  outDir: 'dist/review', emptyOutDir: false, minify: false,
  lib: { entry: `${root}src/review/main.ts`, formats: ['iife'], name: 'SpoolinReview', fileName: () => 'review.js' },
} });
await mkdir(`${root}dist/review`, { recursive: true });
await copyFile(`${root}src/review/review.css`, `${root}dist/review/review.css`);

for (const name of ['mcp-inputs', 'mcp-outputs']) await copyFile(`${root}../contracts/json/${name}.schema.json`, `${root}dist/${name}.schema.json`);
