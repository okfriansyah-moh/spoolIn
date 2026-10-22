import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { checkRepository } from './repository-hygiene.mjs';

function fixture(t, files) {
  const root = mkdtempSync(join(tmpdir(), 'spoolin-hygiene-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  for (const [path, contents] of Object.entries(files)) {
    const target = join(root, path);
    mkdirSync(join(target, '..'), { recursive: true });
    writeFileSync(target, contents);
  }
  return root;
}

test('accepts consolidated prompts and existing relative links', (t) => {
  const files = {
    'docs/prompts/run.md':
      '[source](../source%20file.md#section) [web](https://example.com) [self](#inputs)',
    'docs/source file.md': '# Section',
    'README.md':
      '[run](docs/prompts/run.md) [shortcut](.agents/skills/running-prompt/SKILL.md)',
  };
  assert.deepEqual(checkRepository(fixture(t, files), Object.keys(files)), []);
});

test('rejects a second prompt directory and stale links in docs or shortcuts', (t) => {
  const files = {
    'docs/prompt/run.md': '# Old prompt',
    'README.md': '[run](docs/prompt/run.md)',
    '.agents/skills/run/SKILL.md': 'Read `docs/prompt/run.md`.',
  };
  const failures = checkRepository(fixture(t, files), Object.keys(files));
  assert.equal(failures.length, 3);
});

test('reports missing or malformed local prompt links', (t) => {
  const files = {
    'docs/prompts/run.md': '[missing](../gone.md) [invalid](../%zz.md)',
  };
  const failures = checkRepository(fixture(t, files), Object.keys(files));
  assert.equal(failures.length, 2);
  assert.match(failures[0], /missing local link/);
  assert.match(failures[1], /invalid link encoding/);
});

test('preserves frozen release evidence without treating old paths as active instructions', (t) => {
  const files = { 'docs/release/old.md': '`docs/prompt/run.md`' };
  assert.deepEqual(checkRepository(fixture(t, files), Object.keys(files)), []);
});
