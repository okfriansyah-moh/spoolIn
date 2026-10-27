import { compareText } from '../operations/order.js';
import { fromJsonSchema } from '@modelcontextprotocol/server';
import type { JsonSchemaType } from '@modelcontextprotocol/server';
import type {
  PolicySnapshot,
  TextBound,
} from '../generated/spoolin/v1/spoolin.js';
import { MAX_POSTER_INPUT_BYTES } from './poster-import.js';
import { GatewayFailure } from '../operations/gateway-failure.js';

const fields = [
  'registrationName',
  'personName',
  'title',
  'originalTitle',
  'synopsis',
  'creditRole',
  'characterName',
  'biography',
  'email',
  'password',
  'search',
  'sourceUrl',
  'reuseBasis',
] as const;
const optional = new Set([
  'originalTitle',
  'characterName',
  'biography',
  'search',
]);
const MAX_INT = 2147483647;
const MAX_LONG = 9223372036854775807n;
const MAX_RESPONSE_BYTES = 128 * 1024;
type Field = (typeof fields)[number];
export type EffectivePolicy = Readonly<{
  revision: string;
  bounds: Readonly<Record<Field, Readonly<TextBound>>>;
  maxPage: number;
  metadataBytes: number;
  artwork: ReturnType<typeof artwork>;
  fingerprint: string;
}>;

function unavailable(): never {
  throw new GatewayFailure('UNAVAILABLE');
}
function integer(value: unknown, minimum = 1): number {
  if (
    typeof value !== 'number' ||
    !Number.isSafeInteger(value) ||
    value < minimum ||
    value > MAX_INT
  )
    unavailable();
  return value;
}
function long(value: unknown): string {
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value)) unavailable();
  if (BigInt(value) > MAX_LONG) unavailable();
  return value;
}
function bound(value: TextBound): Readonly<TextBound> {
  if (!value || typeof value.field !== 'string') unavailable();
  const minimum = integer(value.minCodePoints, 0);
  const maximum = integer(value.maxCodePoints);
  if (minimum > maximum || typeof value.required !== 'boolean') unavailable();
  if (
    value.required !== !optional.has(value.field) ||
    (value.required && minimum < 1)
  )
    unavailable();
  return Object.freeze({
    field: value.field,
    minCodePoints: minimum,
    maxCodePoints: maximum,
    required: value.required,
  });
}
function artwork(value: PolicySnapshot['artwork']) {
  if (!value || !Array.isArray(value.formats) || !value.formats.length)
    unavailable();
  if (value.formats.some((format) => !['JPEG', 'PNG', 'WEBP'].includes(format)))
    unavailable();
  if (new Set(value.formats).size !== value.formats.length) unavailable();
  return {
    maxInputBytes: long(value.maxInputBytes),
    formats: [...value.formats].sort(compareText),
    maxDimensionPx: integer(value.maxDimensionPx),
    maxPixels: long(value.maxPixels),
  };
}
export function readEffectivePolicy(value: PolicySnapshot): EffectivePolicy {
  if (
    !value ||
    typeof value.revision !== 'string' ||
    !/^[0-9a-f]{64}$/.test(value.revision)
  )
    unavailable();
  if (!Array.isArray(value.textBounds)) unavailable();
  const entries = new Map(
    value.textBounds.map((entry) => {
      const checked = bound(entry);
      return [checked.field, checked] as const;
    }),
  );
  if (
    entries.size !== fields.length ||
    value.textBounds.length !== fields.length
  )
    unavailable();
  if (fields.some((name) => !entries.has(name))) unavailable();
  const bounds = Object.freeze(
    Object.fromEntries(fields.map((name) => [name, entries.get(name)!])),
  ) as EffectivePolicy['bounds'];
  const requests = value.requestLimits;
  if (!requests) unavailable();
  const maxPage = integer(requests.maxPage);
  const metadataBytes = integer(requests.maxMetadataBytes);
  if (integer(requests.maxSearchCodePoints) !== bounds.search.maxCodePoints)
    unavailable();
  const image = artwork(value.artwork);
  return Object.freeze({
    revision: value.revision,
    bounds,
    maxPage,
    metadataBytes,
    artwork: Object.freeze(image),
    fingerprint: JSON.stringify([
      value.revision,
      bounds,
      maxPage,
      metadataBytes,
      image,
    ]),
  });
}

function object(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value))
    unavailable();
  return value as Record<string, unknown>;
}
function text(schema: Record<string, unknown>, value: Readonly<TextBound>) {
  schema.minLength = value.minCodePoints;
  schema.maxLength = value.maxCodePoints;
}
export function effectiveInputSchemas(
  base: Record<string, JsonSchemaType>,
  policy: EffectivePolicy,
) {
  const schemas = structuredClone(base);
  const context = object(object(schemas.movie_context).properties);
  context.query = {
    type: 'string',
    anyOf: [
      { maxLength: 0 },
      {
        minLength: Math.max(1, policy.bounds.search.minCodePoints),
        maxLength: policy.bounds.search.maxCodePoints,
      },
    ],
  };
  object(context.limit).maximum = Math.min(20, policy.maxPage);
  const proposal = object(object(schemas.propose_movie).properties);
  for (const name of ['title', 'originalTitle', 'synopsis'] as const)
    text(object(proposal[name]), policy.bounds[name]);
  const source = object(object(object(proposal.sources).items).properties);
  text(object(source.url), policy.bounds.sourceUrl);
  text(object(source.reuseBasis), policy.bounds.reuseBasis);
  if (schemas.prepare_movie_poster) {
    const prepare = object(object(schemas.prepare_movie_poster).properties);
    const maximum = Math.min(Number(policy.artwork.maxInputBytes), MAX_POSTER_INPUT_BYTES);
    object(prepare.content).maxLength = Math.ceil(maximum / 3) * 4;
    text(object(prepare.url), policy.bounds.sourceUrl);
    object(prepare.sourceUrl).maxLength = policy.bounds.sourceUrl.maxCodePoints;
    text(object(prepare.reuseBasis), policy.bounds.reuseBasis);
    object(prepare.mediaType).enum = policy.artwork.formats.map((format) =>
      ({ JPEG: 'image/jpeg', PNG: 'image/png', WEBP: 'image/webp' } as Record<string, string>)[format],
    );
  }
  return schemas;
}
export async function validateEffectiveInput(
  schema: JsonSchemaType,
  args: Record<string, unknown>,
  policy: EffectivePolicy,
): Promise<void> {
  if (
    Buffer.byteLength(JSON.stringify({ ...args, ...(typeof args.content === 'string' ? { content: '' } : {}) })) >
    Math.min(MAX_RESPONSE_BYTES, policy.metadataBytes)
  ) {
    throw new GatewayFailure('RESOURCE_EXHAUSTED');
  }
  const checked = await fromJsonSchema(schema)['~standard'].validate(args);
  if (checked.issues) throw new GatewayFailure('FIELD_INVALID');
}

export class SessionPolicy {
  current: EffectivePolicy;
  private loading: Promise<EffectivePolicy> | undefined;
  constructor(
    initial: PolicySnapshot,
    private readonly load: () => Promise<PolicySnapshot>,
    private readonly changed: (policy: EffectivePolicy) => void,
  ) {
    this.current = readEffectivePolicy(initial);
  }
  async refresh(): Promise<EffectivePolicy> {
    this.loading ??= this.fetch().finally(() => {
      this.loading = undefined;
    });
    return this.loading;
  }
  private async fetch(): Promise<EffectivePolicy> {
    return this.accept(await this.load());
  }
  accept(snapshot: PolicySnapshot): EffectivePolicy {
    const next = readEffectivePolicy(snapshot);
    if (
      next.revision === this.current.revision &&
      next.fingerprint !== this.current.fingerprint
    )
      unavailable();
    if (next.revision !== this.current.revision) {
      this.current = next;
      this.changed(next);
    }
    return next;
  }
}
