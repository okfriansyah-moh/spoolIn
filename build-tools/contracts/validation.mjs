/** Executable schema rules for generated-contract compatibility checks. */
import { readFileSync } from 'node:fs';
import { URL } from 'node:url';
import { buildSchema } from 'graphql';
import {
  longString,
  int32,
  uuid,
  unicode,
  ordinaryText,
  dateTime,
  approvalChallenge,
  envelope,
} from './wire.mjs';

const root = new URL('../../', import.meta.url);
const load = (name) =>
  JSON.parse(
    readFileSync(new URL(`contracts/json/${name}.schema.json`, root), 'utf8'),
  );
const outputs = load('mcp-outputs');
const inputs = load('mcp-inputs');
const drafts = load('draft');
const graphql = buildSchema(
  readFileSync(new URL('contracts/graphql/schema.graphql', root), 'utf8'),
);
const patchFields = Object.fromEntries(
  ['Movie', 'Person', 'Credit'].map((name) => [
    name.toUpperCase(),
    new Set(Object.keys(graphql.getType(`${name}Patch`).getFields())),
  ]),
);

export function resolveRef(registry, ref) {
  if (!ref.startsWith('#/'))
    throw new TypeError('Only local schema references are supported');
  return ref
    .slice(2)
    .split('/')
    .reduce((node, key) => {
      const next = node?.[key.replaceAll('~1', '/').replaceAll('~0', '~')];
      if (next === undefined)
        throw new TypeError('Unresolved local schema reference');
      return next;
    }, registry);
}

export function checkReferences(schema, registry = schema) {
  if (Array.isArray(schema))
    schema.forEach((item) => checkReferences(item, registry));
  else if (schema && typeof schema === 'object') {
    if (schema.$ref) resolveRef(registry, schema.$ref);
    Object.values(schema).forEach((item) => checkReferences(item, registry));
  }
}

function bounded(value, minimum, maximum) {
  if (
    (minimum !== undefined && value < minimum) ||
    (maximum !== undefined && value > maximum)
  ) {
    throw new RangeError('Value outside bounds');
  }
}

function validateType(schema, value) {
  const matches = {
    null: value === null,
    object:
      value !== null && typeof value === 'object' && !Array.isArray(value),
    array: Array.isArray(value),
    string: typeof value === 'string',
    integer: typeof value === 'number' && Number.isInteger(value),
    number: typeof value === 'number' && Number.isFinite(value),
    boolean: typeof value === 'boolean',
  };
  const types = Array.isArray(schema.type) ? schema.type : [schema.type];
  if (schema.type && !types.some((type) => matches[type]))
    throw new TypeError('Invalid schema value type');
}

function validateChoices(schema, value, registry) {
  if (
    schema.anyOf &&
    !schema.anyOf.some((branch) => {
      try {
        validateSchema(branch, value, registry);
        return true;
      } catch {
        return false;
      }
    })
  )
    throw new TypeError('Value matches no permitted schema');
  if ('const' in schema && value !== schema.const)
    throw new TypeError('Unexpected constant');
  if (schema.enum && !schema.enum.includes(value))
    throw new TypeError('Unrecognized enum');
}

function validateText(schema, value) {
  unicode(value);
  bounded([...value].length, schema.minLength, schema.maxLength);
  if (schema.pattern && !new RegExp(schema.pattern, 'u').test(value))
    throw new TypeError('Invalid string encoding');
  const formats = { uuid, 'date-time': dateTime, 'int64-string': longString };
  if (formats[schema.format]) formats[schema.format](value);
}

function validateNumber(schema, value) {
  if (!Number.isFinite(value)) throw new TypeError('Nonfinite number');
  const types = Array.isArray(schema.type) ? schema.type : [schema.type];
  if (types.includes('integer')) int32(value);
  bounded(value, schema.minimum, schema.maximum);
}

function validateArray(schema, value, registry) {
  bounded(value.length, schema.minItems, schema.maxItems);
  if (
    schema.uniqueItems &&
    new Set(value.map((item) => JSON.stringify(item))).size !== value.length
  )
    throw new TypeError('Duplicate array member');
  if (schema.items)
    value.forEach((item) => validateSchema(schema.items, item, registry));
}

function validateObject(schema, value, registry) {
  if (schema.required?.some((key) => !Object.hasOwn(value, key)))
    throw new TypeError('Required field missing');
  for (const [key, item] of Object.entries(value)) {
    if (Object.hasOwn(schema.properties ?? {}, key))
      validateSchema(schema.properties[key], item, registry);
    else if (schema.additionalProperties === false)
      throw new TypeError('Undeclared field');
  }
}

export function validateSchema(schema, value, registry = schema) {
  if (schema.$ref)
    return validateSchema(resolveRef(registry, schema.$ref), value, registry);
  validateType(schema, value);
  validateChoices(schema, value, registry);
  if (value === null) return value;
  const handlers = {
    string: validateText,
    number: validateNumber,
    array: validateArray,
    object: validateObject,
  };
  const kind = Array.isArray(value) ? 'array' : typeof value;
  if (handlers[kind]) handlers[kind](schema, value, registry);
  return value;
}

export function validateOutput(tool, value) {
  if (!Object.hasOwn(outputs.toolOutputs, tool))
    throw new TypeError('Unknown output tool');
  envelope(value);
  validateSchema(outputs.toolOutputs[tool], value, outputs);
  if (
    ['movie_operation', 'approve_movie', 'reject_movie'].includes(tool) &&
    value.version != null
  )
    longString(value.version);
  return value;
}

export function validateInput(tool, value) {
  if (!Object.hasOwn(inputs, tool)) throw new TypeError('Unknown input tool');
  envelope(value);
  validateSchema(inputs[tool], value);
  if (tool === 'approve_movie' || tool === 'reject_movie')
    approvalChallenge(value.challenge);
  if (tool === 'propose_movie') {
    if (
      Object.hasOwn(value, 'proposalId') !==
      Object.hasOwn(value, 'expectedRevision')
    )
      throw new TypeError('Proposal identity and revision must occur together');
    for (const source of value.sources ?? []) {
      const url = new URL(source.url);
      if (
        !['http:', 'https:'].includes(url.protocol) ||
        !Object.hasOwn(drafts.$defs.MOVIE.properties, source.field)
      )
        throw new TypeError('Unsupported source evidence');
    }
  }
  return value;
}

export function validateDraft(kind, value) {
  if (!Object.hasOwn(drafts.$defs, kind))
    throw new TypeError('Unknown draft target');
  envelope(value, 65536);
  validateSchema(drafts.$defs[kind], value, drafts);
  return value;
}

function patchText(value, required) {
  const result = ordinaryText(value);
  if (!result && required) throw new TypeError('FIELD_REQUIRED');
  return result || null;
}

function patchArray(key, value) {
  let result = value.map(ordinaryText);
  if (key === 'characters') result = result.filter(Boolean);
  if (new Set(result).size !== result.length)
    throw new TypeError('Duplicate array member');
  if (key === 'genreIds' && !result.length)
    throw new TypeError('FIELD_REQUIRED');
  return result;
}

function patchField(key, raw, required) {
  if (raw === null && (required || ['countries', 'characters'].includes(key)))
    throw new TypeError('FIELD_REQUIRED');
  let value = raw;
  if (typeof value === 'string') value = patchText(value, required);
  if (Array.isArray(value)) value = patchArray(key, value);
  if (
    key === 'release' &&
    (!value || !Object.hasOwn(value, 'year') || value.year === null)
  )
    throw new TypeError('FIELD_REQUIRED');
  return value;
}

export function patchValues(kind, before, patch) {
  if (!Object.hasOwn(drafts.$defs, kind))
    throw new TypeError('Unknown patch target');
  const properties = drafts.$defs[kind].properties;
  const required = {
    MOVIE: ['title', 'synopsis', 'genreIds', 'release', 'synopsisLanguage'],
    PERSON: ['name'],
    CREDIT: ['roleId'],
  }[kind];
  const next = { ...before };
  for (const [key, raw] of Object.entries(patch)) {
    if (!patchFields[kind].has(key))
      throw new TypeError('Undeclared patch field');
    const value = patchField(key, raw, required.includes(key));
    validateSchema(properties[key], value);
    next[key] = value;
  }
  return next;
}

export function maskedValues(kind, before, values, paths) {
  const names = Object.keys(drafts.$defs[kind]?.properties ?? {});
  const mapping = Object.fromEntries(
    names.map((name) => [
      name.replace(/[A-Z]/g, (c) => `_${c.toLowerCase()}`),
      name,
    ]),
  );
  if (new Set(paths).size !== paths.length)
    throw new TypeError('Duplicate mask path');
  const patch = {};
  for (const path of paths) {
    if (!Object.hasOwn(mapping, path))
      throw new TypeError('Unsupported mask path');
    const name = mapping[path];
    patch[name] = Object.hasOwn(values, name)
      ? values[name]
      : ['countries', 'characters', 'genreIds'].includes(name)
        ? []
        : null;
  }
  return patchValues(kind, before, patch);
}
