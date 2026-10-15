import type {
  TitleQuery,
  UpdateMovieMutationVariables,
} from '../../frontend/src/lib/generated/graphql.js';
import type { MovieDraft } from '../../frontend/src/lib/generated/json-types.js';
import {
  MovieWrite,
  Provenance,
  MediaServiceService,
  CatalogServiceService,
  McpGatewayServiceService,
  IdentityServiceService,
} from '../../mcp/src/generated/spoolin/v1/spoolin.js';
import {
  appInputSchemas,
  modelInputSchemas,
  toolOutputSchemas,
} from '../../mcp/src/generated/tool-schemas.js';

const write = MovieWrite.fromPartial({
  expectedVersion: '9007199254740993',
  updateMask: ['original_title'],
});
if (
  MovieWrite.decode(MovieWrite.encode(write).finish()).expectedVersion !==
  '9007199254740993'
)
  throw new Error('Long narrowed');
if (
  !MediaServiceService.upload.requestStream ||
  MediaServiceService.upload.responseStream
)
  throw new Error('Upload streaming contract');
if (
  !MediaServiceService.readAsset.responseStream ||
  MediaServiceService.readAsset.requestStream
)
  throw new Error('Read streaming contract');
if (
  Object.keys(CatalogServiceService).length !== 30 ||
  Object.keys(IdentityServiceService).length !== 2 ||
  Object.keys(McpGatewayServiceService).length !== 6
)
  throw new Error('Service inventory');
if (
  'approve_movie' in modelInputSchemas ||
  !('approve_movie' in appInputSchemas)
)
  throw new Error('Trusted schema partition');
if (JSON.stringify(toolOutputSchemas).includes('"$ref"'))
  throw new Error('Unresolved native output schema');
type ExactLong = UpdateMovieMutationVariables['expectedVersion'];
const version: ExactLong = '9223372036854775807';
// @ts-expect-error Decimal Long values must never become JavaScript numbers.
const numericVersion: ExactLong = 9007199254740993;
void numericVersion;
const empty: TitleQuery = { title: null };
if (version !== '9223372036854775807' || empty.title !== null)
  throw new Error('Consumer type mismatch');

const correction: MovieDraft = { release: { year: null } };
if (correction.release?.year !== null)
  throw new Error('Correction draft null year lost');

const source = Provenance.fromPartial({
  retrievedAt: { seconds: '1791774000', nanos: 123456000 },
});
if (
  Provenance.decode(Provenance.encode(source).finish()).retrievedAt?.nanos !==
  123456000
)
  throw new Error('Timestamp microseconds lost');
