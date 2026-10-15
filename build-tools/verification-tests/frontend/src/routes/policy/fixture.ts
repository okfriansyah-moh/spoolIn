import { parsePolicy } from '../../../../../../frontend/src/lib/policy/parse';
import { textFields } from '../../../../../../frontend/src/lib/policy/types';
const limits: Record<string, [number, number, boolean]> = {
  registrationName: [2, 100, true],
  personName: [1, 100, true],
  title: [1, 255, true],
  originalTitle: [1, 255, false],
  synopsis: [1, 5000, true],
  creditRole: [1, 100, true],
  characterName: [1, 200, false],
  biography: [1, 5000, false],
  email: [1, 254, true],
  password: [15, 128, true],
  search: [0, 200, false],
  sourceUrl: [1, 2048, true],
  reuseBasis: [1, 4000, true],
};
export function specimenPolicy() {
  return parsePolicy({
    revision: 'a'.repeat(64),
    textBounds: Object.fromEntries(
      textFields.map((name) => {
        const range = limits[name];
        if (!range) throw new Error('Missing controlled policy field');
        return [
          name,
          {
            minCodePoints: range[0],
            maxCodePoints: range[1],
            required: range[2],
            normalization: name === 'password' ? 'EXACT' : 'TRIM_WHITE_SPACE',
          },
        ];
      }),
    ),
    artwork: {
      maxInputBytes: '5242880',
      formats: ['JPEG', 'PNG', 'WEBP'],
      maxDimensionPx: 8192,
      maxPixels: '40000000',
      maxDecodedBytes: '160000000',
      maxOutputBytes: '8388608',
    },
    requestLimits: {
      metadataBytes: 131072,
      uploadEnvelopeBytes: 7100000,
      graphqlQueryBytes: 16384,
      maxDepth: 8,
      maxFields: 200,
      maxAliases: 20,
      maxCost: 1000,
      defaultPage: 24,
      maxPage: 100,
    },
  });
}
export const retainedDraft =
  'After fifteen years away, a railway archivist returns to her coastal hometown to record its last overnight train.';
export type PolicyState =
  | 'text-boundaries'
  | 'optional-clearing'
  | 'policy-changed'
  | 'policy-unavailable';
export function policyState(value: string | null): PolicyState {
  return value === 'optional-clearing' ||
    value === 'policy-changed' ||
    value === 'policy-unavailable'
    ? value
    : 'text-boundaries';
}

export const plainTextExample = '<script>alert(1)</script>';
