/** Render typed JSON projections from the authored schema shapes. */
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath, URL } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const load = (name) =>
  JSON.parse(readFileSync(`${root}contracts/json/${name}.schema.json`, 'utf8'));
const quote = (name) => JSON.stringify(name);
function type(schema, registry) {
  if (schema.$ref)
    return type(
      schema.$ref
        .slice(2)
        .split('/')
        .reduce((node, key) => node[key], registry),
      registry,
    );
  if (schema.anyOf)
    return schema.anyOf.map((branch) => type(branch, registry)).join(' | ');
  if (schema.enum) return schema.enum.map(quote).join(' | ');
  if ('const' in schema) return quote(schema.const);
  if (Array.isArray(schema.type))
    return schema.type
      .map((item) => type({ ...schema, type: item }, registry))
      .join(' | ');
  if (schema.type === 'object')
    return `{ ${Object.entries(schema.properties ?? {})
      .map(
        ([key, item]) =>
          `readonly ${quote(key)}${schema.required?.includes(key) ? '' : '?'}: ${type(item, registry)}`,
      )
      .join('; ')} }`;
  if (schema.type === 'array')
    return `ReadonlyArray<${type(schema.items, registry)}>`;
  return (
    {
      string: 'string',
      integer: 'number',
      number: 'number',
      boolean: 'boolean',
      null: 'null',
    }[schema.type] ?? 'JsonValue'
  );
}
const policy = load('policy');
const draft = load('draft');
const body =
  [
    'export type JsonValue = null | boolean | number | string | ReadonlyArray<JsonValue> | {readonly [key: string]: JsonValue};',
    ...Object.entries(policy.properties).map(
      ([name, schema]) =>
        `export type Policy${name[0].toUpperCase() + name.slice(1)} = ${type(schema, policy)};`,
    ),
    ...Object.entries(draft.$defs).map(
      ([name, schema]) =>
        `export type ${name[0]}${name.slice(1).toLowerCase()}Draft = ${type(schema, draft)};`,
    ),
    'export type DraftValues = MovieDraft | PersonDraft | CreditDraft;',
    'export type AuditChanges = Readonly<Partial<Record<string, {readonly before: JsonValue; readonly after: JsonValue}>>>;',
    'export type FieldSources = Readonly<Partial<Record<string, {readonly url: string; readonly retrievedAt: string; readonly reuseBasis: string; readonly precision: string | null}>>>;',
  ].join('\n') + '\n';
for (const directory of ['frontend/src/lib/generated', 'mcp/src/generated']) {
  mkdirSync(`${root}${directory}`, { recursive: true });
  writeFileSync(`${root}${directory}/json-types.ts`, body);
}

const outputRegistry = load('mcp-outputs');
function resolve(value) {
  if (Array.isArray(value)) return value.map(resolve);
  if (value && typeof value === 'object') {
    if (value.$ref)
      return resolve(
        value.$ref
          .slice(2)
          .split('/')
          .reduce((node, key) => node[key], outputRegistry),
      );
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, resolve(item)]),
    );
  }
  return value;
}
const inputRegistry = load('mcp-inputs');
const modelInputSchemas = Object.fromEntries(
  Object.entries(inputRegistry).filter(
    ([name]) => !['approve_movie', 'reject_movie'].includes(name),
  ),
);
const appInputSchemas = Object.fromEntries(
  Object.entries(inputRegistry).filter(([name]) =>
    ['approve_movie', 'reject_movie'].includes(name),
  ),
);
writeFileSync(
  `${root}mcp/src/generated/tool-schemas.ts`,
  `export const modelInputSchemas = ${JSON.stringify(modelInputSchemas, null, 2)} as const;\nexport const appInputSchemas = ${JSON.stringify(appInputSchemas, null, 2)} as const;\nexport const toolOutputSchemas = ${JSON.stringify(resolve(outputRegistry.toolOutputs), null, 2)} as const;\n`,
);
