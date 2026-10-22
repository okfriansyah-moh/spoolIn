import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, isAbsolute, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const repositoryRoot = fileURLToPath(new URL('../../', import.meta.url));
const oldPromptReference = /(?:^|[^\w-])(?:docs\/)?prompt\/[^\s)"'`<>]+\.md/;

function promptLinks(root, path) {
  const source = readFileSync(resolve(root, path), 'utf8');
  const failures = [];
  for (const match of source.matchAll(
    /!?\[[^\]\n]*\]\((<[^>]+>|[^\s)]+)(?:\s+"[^"]*")?\)/g,
  )) {
    const target = match[1].replace(/^<|>$/g, '').split('#')[0];
    if (!target || isAbsolute(target) || /^[a-z][a-z\d+.-]*:/i.test(target))
      continue;
    let decoded;
    try {
      decoded = decodeURIComponent(target);
    } catch {
      failures.push(`${path}: invalid link encoding: ${target}`);
      continue;
    }
    if (!existsSync(resolve(root, dirname(path), decoded))) {
      failures.push(`${path}: missing local link: ${target}`);
    }
  }
  return failures;
}

export function checkRepository(root, paths) {
  const failures = [];
  if (existsSync(resolve(root, 'docs/prompt'))) {
    failures.push('Keep project prompts together in docs/prompts/.');
  }
  for (const path of paths) {
    if (!path.endsWith('.md') || !existsSync(resolve(root, path))) continue;
    if (path.startsWith('.ai/') || path.startsWith('docs/release/')) continue;
    const source = readFileSync(resolve(root, path), 'utf8');
    if (oldPromptReference.test(source)) {
      failures.push(
        `${path}: replace obsolete prompt path with docs/prompts/.`,
      );
    }
    if (path.startsWith('docs/prompts/'))
      failures.push(...promptLinks(root, path));
  }
  return failures;
}

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  const paths = execFileSync(
    'git',
    ['ls-files', '--cached', '--others', '--exclude-standard', '-z'],
    {
      cwd: repositoryRoot,
      encoding: 'utf8',
    },
  )
    .split('\0')
    .filter(Boolean);
  const failures = checkRepository(repositoryRoot, paths);
  if (failures.length) {
    for (const failure of failures) process.stderr.write(`${failure}\n`);
    process.exitCode = 1;
  } else {
    process.stdout.write(
      'Repository hygiene: prompt placement, references and local prompt links passed.\n',
    );
  }
}
