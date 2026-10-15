import { existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath, URL } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const generator = new URL('../generate-contracts.mjs', import.meta.url);
const schema = new URL(
  '../../contracts/graphql/schema.graphql',
  import.meta.url,
);

if (existsSync(generator)) {
  const result = spawnSync(process.execPath, [fileURLToPath(generator)], {
    cwd: root,
    stdio: 'inherit',
  });
  process.exitCode = result.status ?? 2;
} else if (existsSync(schema) || process.argv.includes('--required')) {
  console.error(
    'Setup unavailable: T002 must provide build-tools/generate-contracts.mjs.',
  );
  process.exitCode = 2;
}
