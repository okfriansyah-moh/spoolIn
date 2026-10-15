import { URL } from 'node:url';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { codegen } from '@graphql-codegen/core';
import * as typescript from '@graphql-codegen/typescript';
import * as operations from '@graphql-codegen/typescript-operations';
import * as document from '@graphql-codegen/typed-document-node';
import { parse } from 'graphql';

const schema = parse(
  await readFile(new URL('./schema.graphql', import.meta.url), 'utf8'),
);
const operation = parse(
  await readFile(new URL('./probe.graphql', import.meta.url), 'utf8'),
);
const generated = await codegen({
  filename: 'types.ts',
  schema,
  documents: [{ document: operation }],
  config: { useTypeImports: true },
  plugins: [{ typescript: {} }, { operations: {} }, { document: {} }],
  pluginMap: { typescript, operations, document },
});
await mkdir(new URL('./build/', import.meta.url), { recursive: true });
await writeFile(new URL('./build/types.ts', import.meta.url), generated);
