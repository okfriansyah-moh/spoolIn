import type { PolicySnapshot } from '../../src/generated/spoolin/v1/spoolin.js';

/** Complete protobuf projection of approved defaults; this fixture does not establish runtime policy proof. */
export function policyFixture(): PolicySnapshot {
  const optional = new Set(['originalTitle', 'characterName', 'biography', 'search']);
  const definitions: [string, number, number][] = [
    ['registrationName', 2, 100], ['personName', 1, 100], ['title', 1, 255], ['originalTitle', 1, 255],
    ['synopsis', 1, 5000], ['creditRole', 1, 100], ['characterName', 1, 200], ['biography', 1, 5000],
    ['email', 1, 254], ['password', 15, 128], ['search', 0, 200], ['sourceUrl', 1, 2048], ['reuseBasis', 1, 4000],
  ];
  return {
    revision: 'a'.repeat(64),
    textBounds: definitions.map(([field, minCodePoints, maxCodePoints]) => ({
      field, minCodePoints, maxCodePoints, required: !optional.has(field),
    })),
    artwork: { maxInputBytes: '5242880', formats: ['JPEG', 'PNG', 'WEBP'], maxDimensionPx: 8192, maxPixels: '40000000' },
    requestLimits: { maxPage: 100, maxMetadataBytes: 131072, maxSearchCodePoints: 200 },
  };
}
