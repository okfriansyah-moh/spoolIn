export type TextBound = Readonly<{
  minCodePoints: number;
  maxCodePoints: number;
  required: boolean;
  normalization: 'EXACT' | 'TRIM_WHITE_SPACE';
}>;
export const textFields = [
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
export type TextField = (typeof textFields)[number];
export const requestFields = [
  'metadataBytes',
  'uploadEnvelopeBytes',
  'graphqlQueryBytes',
  'maxDepth',
  'maxFields',
  'maxAliases',
  'maxCost',
  'defaultPage',
  'maxPage',
] as const;
export type Policy = Readonly<{
  revision: string;
  textBounds: Readonly<Record<string, TextBound>>;
  artwork: Readonly<{
    maxInputBytes: string;
    formats: readonly string[];
    maxDimensionPx: number;
    maxPixels: string;
    maxDecodedBytes: string;
    maxOutputBytes: string;
  }>;
  requestLimits: Readonly<Record<string, number>>;
}>;
export class PolicyUnavailable extends Error {
  constructor() {
    super('The current input rules could not be loaded safely.');
  }
}
