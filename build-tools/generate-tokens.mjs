/** Resolve design-token references once for CSS and the MCP review adapter. */
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath, URL } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const tokens = JSON.parse(
  readFileSync(`${root}docs/design-system/tokens.json`, 'utf8'),
);
const flatten = (data) =>
  Object.fromEntries(
    ['primitive', 'semantic', 'component'].flatMap((group) =>
      Object.entries(data[group]).map(([name, value]) => [
        `${group}.${name}`,
        value,
      ]),
    ),
  );
const themes = {};
for (const theme of ['dark', 'light']) {
  const data = JSON.parse(JSON.stringify(tokens));
  for (const [group, overrides] of Object.entries(
    tokens.overrides?.[theme] ?? {},
  ))
    Object.assign(data[group], overrides);
  const flat = flatten(data);
  function resolve(name, seen = new Set()) {
    if (seen.has(name) || !Object.hasOwn(flat, name))
      throw new Error('Invalid design-token reference');
    const value = flat[name];
    return value.replace(/\{([^}]+)\}/g, (_, target) =>
      resolve(target, new Set([...seen, name])),
    );
  }
  themes[theme] = Object.fromEntries(
    Object.keys(flat).map((name) => [name, resolve(name)]),
  );
}
const css =
  Object.entries(themes)
    .map(
      ([theme, values]) =>
        `${theme === 'dark' ? ':root, ' : ''}[data-theme="${theme}"] {\n${Object.entries(
          values,
        )
          .map(
            ([name, value]) =>
              `  --${name.startsWith('semantic.') ? name.slice(9) : name.replace('.', '-')}: ${value};`,
          )
          .join('\n')}\n}`,
    )
    .join('\n') + '\n';
for (const directory of ['frontend/src/lib/generated', 'mcp/src/generated']) {
  mkdirSync(`${root}${directory}`, { recursive: true });
  writeFileSync(`${root}${directory}/tokens.css`, css);
}
writeFileSync(
  `${root}mcp/src/generated/theme-tokens.ts`,
  `export const themeTokens = ${JSON.stringify(themes, null, 2)} as const;\n`,
);
