/** Generate boundary DTOs and complete browser operation types from the one SDL. */
import './generate-json-types.mjs';
import './generate-tokens.mjs';
import {
  readFileSync,
  mkdirSync,
  writeFileSync,
  readdirSync,
  rmSync,
} from 'node:fs';
import { fileURLToPath, URL } from 'node:url';
import {
  parse,
  print,
  buildASTSchema,
  validateSchema,
  validate,
  concatAST,
  Kind,
  isScalarType,
  isEnumType,
  getNamedType,
} from 'graphql';
import { codegen } from '@graphql-codegen/core';
import * as typescript from '@graphql-codegen/typescript';
import * as operations from '@graphql-codegen/typescript-operations';
import * as documents from '@graphql-codegen/typed-document-node';

const root = fileURLToPath(new URL('../', import.meta.url));
const sdl = readFileSync(`${root}contracts/graphql/schema.graphql`, 'utf8');
const ast = parse(sdl);
const schema = buildASTSchema(ast);
const errors = validateSchema(schema);
if (errors.length)
  throw new Error(errors.map((error) => error.message).join('\n'));
const write = (path, body) => {
  mkdirSync(path.slice(0, path.lastIndexOf('/')), { recursive: true });
  writeFileSync(path, body);
};

function selection(type) {
  const named = getNamedType(type);
  if (isScalarType(named) || isEnumType(named)) return '';
  return ` { ${Object.values(named.getFields())
    .map((field) => `${field.name}${selection(field.type)}`)
    .join(' ')} }`;
}
const operationDocuments = [];
for (const [kind, object] of [
  ['query', schema.getQueryType()],
  ['mutation', schema.getMutationType()],
]) {
  for (const field of Object.values(object.getFields())) {
    const name = field.name[0].toUpperCase() + field.name.slice(1);
    const definitions = field.args.map(
      (arg) =>
        `$${arg.name}: ${arg.type}${arg.astNode.defaultValue ? ` = ${print(arg.astNode.defaultValue)}` : ''}`,
    );
    const args = field.args.map((arg) => `${arg.name}: $${arg.name}`);
    operationDocuments.push(
      parse(
        `${kind} ${name}${definitions.length ? `(${definitions.join(', ')})` : ''} { ${field.name}${args.length ? `(${args.join(', ')})` : ''}${selection(field.type)} }`,
      ),
    );
  }
}
// Feature documents remain authored in feature folders; verify them against the same schema.
function featureDocuments(path) {
  try {
    for (const entry of readdirSync(path, { withFileTypes: true })) {
      const child = `${path}/${entry.name}`;
      if (entry.isDirectory() && entry.name !== 'generated')
        featureDocuments(child);
      else if (entry.isFile() && child.endsWith('.graphql'))
        operationDocuments.push(parse(readFileSync(child, 'utf8')));
    }
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
}
featureDocuments(`${root}frontend/src`);
const documentIssues = validate(schema, concatAST(operationDocuments));
if (documentIssues.length)
  throw new Error(documentIssues.map((issue) => issue.message).join('\n'));
let ts = await codegen({
  filename: 'graphql.ts',
  schema: ast,
  documents: operationDocuments.map((document, index) => ({
    document,
    location: `source-operation-${index}`,
  })),
  config: {
    immutableTypes: true,
    useTypeImports: true,
    strictScalars: true,
    scalars: {
      UUID: 'string',
      Long: 'string',
      DateTime: 'string',
      Cursor: 'string',
      Base64: 'string',
      JSON: 'JsonValue',
    },
  },
  plugins: [
    {
      operations: {
        importSchemaTypesFrom: './schema-types.js',
        emitLegacyCommonJSImports: false,
        importExtension: '.js',
      },
    },
    { documents: {} },
  ],
  pluginMap: { typescript, operations, documents },
});
let schemaTs = await codegen({
  filename: 'schema-types.ts',
  schema: ast,
  documents: [],
  config: {
    immutableTypes: true,
    useTypeImports: true,
    strictScalars: true,
    enumsAsTypes: true,
    scalars: {
      UUID: 'string',
      Long: 'string',
      DateTime: 'string',
      Cursor: 'string',
      Base64: 'string',
      JSON: 'JsonValue',
    },
  },
  plugins: [{ typescript: {} }],
  pluginMap: { typescript },
});
const projections = {
  artwork: 'PolicyArtwork',
  textBounds: 'PolicyTextBounds',
  requestLimits: 'PolicyRequestLimits',
  values: 'DraftValues',
  changes: 'AuditChanges',
  fieldSources: 'FieldSources',
};
for (const [filename, source] of [
  ['schema-types.ts', schemaTs],
  ['graphql.ts', ts],
]) {
  let generated = source;
  for (const [field, type] of Object.entries(projections)) {
    generated = generated.replace(
      new RegExp(
        `(\\b${field}\\??:\\s*)(?:Scalars\\['JSON'\\]\\['(?:input|output)'\\]|JsonValue)`,
        'g',
      ),
      `$1${type}`,
    );
  }
  write(
    `${root}frontend/src/lib/generated/${filename}`,
    `import type { JsonValue, ${Object.values(projections).join(', ')} } from './json-types.js';\n${filename === 'graphql.ts' ? "export type * from './schema-types.js';\n" : ''}${generated}`,
  );
}

write(
  `${root}frontend/src/lib/generated/all-operations.graphql`,
  operationDocuments.map(print).join('\n\n') + '\n',
);

const javaRoot = `${root}services/core/build/generated/sources/graphql/com/spoolin/core/boundary/graphql/generated`;
mkdirSync(javaRoot, { recursive: true });
for (const file of readdirSync(javaRoot))
  if (file.endsWith('.java')) rmSync(`${javaRoot}/${file}`);
const scalarJava = {
  String: 'String',
  ID: 'String',
  UUID: 'java.util.UUID',
  Long: 'String',
  DateTime: 'java.time.Instant',
  Cursor: 'String',
  Base64: 'String',
  JSON: 'Object',
  Int: 'Integer',
  Float: 'Double',
  Boolean: 'Boolean',
};
const jsonJava = {
  'Policy.artwork': 'PolicyArtwork',
  'Policy.textBounds': 'PolicyTextBounds',
  'Policy.requestLimits': 'PolicyRequestLimits',
  'Draft.values': 'DraftValues',
  'DraftInput.values': 'DraftValues',
  'AuditEvent.changes': 'java.util.Map<String, AuditChangeValue>',
  'Provenance.fieldSources': 'java.util.Map<String, FieldSourceValue>',
};
function javaType(type) {
  if (type.kind === Kind.NON_NULL_TYPE) return javaType(type.type);
  if (type.kind === Kind.LIST_TYPE)
    return `java.util.List<${javaType(type.type)}>`;
  return scalarJava[type.name.value] ?? type.name.value;
}
const header = 'package com.spoolin.core.boundary.graphql.generated;\n\n';
write(
  `${javaRoot}/InputValue.java`,
  `${header}/** Argument presence is independent of a nullable supplied value. */\npublic sealed interface InputValue<T> {\n  record Absent<T>() implements InputValue<T> {}\n  record Present<T>(T value) implements InputValue<T> { public Present { value = copy(value); } }\n  @SuppressWarnings("unchecked")\n  static <T> T copy(T value) {\n    if (value instanceof java.util.List<?> list) return (T) list.stream().map(InputValue::copy).toList();\n    if (value instanceof java.util.Map<?, ?> map) {\n      var result = new java.util.LinkedHashMap<Object, Object>();\n      map.forEach((key, item) -> result.put(key, copy(item)));\n      return (T) java.util.Collections.unmodifiableMap(result);\n    }\n    return value;\n  }\n}\n`,
);
function record(name, fields, implementsType = '') {
  const copy = fields
    .map((field) => {
      const name = field.slice(field.lastIndexOf(' ') + 1);
      return `${name} = InputValue.copy(${name});`;
    })
    .join(' ');
  return `${header}public record ${name}(${fields.join(', ')}) ${implementsType} { public ${name} { ${copy} } }\n`;
}
for (const definition of ast.definitions) {
  const name = definition.name?.value;
  if (definition.kind === Kind.ENUM_TYPE_DEFINITION) {
    write(
      `${javaRoot}/${name}.java`,
      `${header}public enum ${name} { ${definition.values.map((value) => value.name.value).join(', ')} }\n`,
    );
  } else if (
    [Kind.OBJECT_TYPE_DEFINITION, Kind.INPUT_OBJECT_TYPE_DEFINITION].includes(
      definition.kind,
    )
  ) {
    const fields = definition.fields.map((field) => {
      let type =
        jsonJava[`${name}.${field.name.value}`] ?? javaType(field.type);
      if (
        definition.kind === Kind.INPUT_OBJECT_TYPE_DEFINITION &&
        field.type.kind !== Kind.NON_NULL_TYPE
      )
        type = `InputValue<${type}>`;
      return `${type} ${field.name.value}`;
    });
    write(`${javaRoot}/${name}.java`, record(name, fields));
  }
}
function jsonJavaType(name, shape) {
  const kind = Array.isArray(shape.type)
    ? shape.type.find((type) => type !== 'null')
    : shape.type;
  if (kind === 'object') {
    writeJsonRecord(name, shape);
    return name;
  }
  if (kind === 'array')
    return `java.util.List<${jsonJavaType(`${name}Item`, shape.items)}>`;
  return (
    {
      string: 'String',
      integer: 'Integer',
      number: 'Double',
      boolean: 'Boolean',
    }[kind] ?? 'String'
  );
}
function writeJsonRecord(name, shape, implementsType = '') {
  const fields = Object.entries(shape.properties).map(([field, schema]) => {
    let type = jsonJavaType(
      `${name}${field[0].toUpperCase() + field.slice(1)}`,
      schema,
    );
    if (!shape.required?.includes(field)) type = `InputValue<${type}>`;
    return `${type} ${field}`;
  });
  write(`${javaRoot}/${name}.java`, record(name, fields, implementsType));
}
const policyJson = JSON.parse(
  readFileSync(`${root}contracts/json/policy.schema.json`, 'utf8'),
);
for (const [name, shape] of Object.entries(policyJson.properties))
  writeJsonRecord(`Policy${name[0].toUpperCase() + name.slice(1)}`, shape);
const draftJson = JSON.parse(
  readFileSync(`${root}contracts/json/draft.schema.json`, 'utf8'),
);
for (const [name, shape] of Object.entries(draftJson.$defs))
  writeJsonRecord(
    `${name[0]}${name.slice(1).toLowerCase()}Draft`,
    shape,
    'implements DraftValues',
  );
write(
  `${javaRoot}/DraftValues.java`,
  `${header}public sealed interface DraftValues permits MovieDraft, PersonDraft, CreditDraft {}\n`,
);
write(
  `${javaRoot}/AuditChangeValue.java`,
  record('AuditChangeValue', ['Object before', 'Object after']),
);
write(
  `${javaRoot}/FieldSourceValue.java`,
  `${header}public record FieldSourceValue(String url, java.time.Instant retrievedAt, String reuseBasis, String precision) {}\n`,
);
write(
  `${root}services/core/build/generated/resources/contracts/graphql/schema.graphql`,
  sdl,
);
console.log(
  `Generated ${operationDocuments.length} browser documents and immutable JVM boundary DTOs.`,
);
