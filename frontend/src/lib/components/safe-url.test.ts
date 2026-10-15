import { it, expect } from 'vitest';
import { safeUrl, safeArtwork, isRecordId } from './safe-url';
const denied = [
  'javascript:alert(1)',
  'data:text/html,<script>alert(1)</script>',
  ' javaScript:alert(1)',
  'java\nscript:alert(1)',
  '%6aavascript:alert(1)',
  '&#106;avascript:alert(1)',
  'https%3a//example.test',
  ' https://example.test',
  'https://example.test/ a',
  '//example.test',
  'http://',
  'https://user:pass@example.test',
  'ftp://example.test',
  null,
  undefined,
  3,
];
for (const url of denied)
  it(`rejects unsafe URL ${JSON.stringify(url)}`, () => {
    expect(safeUrl(url)).toBeUndefined();
  });
for (const url of ['https://example.test/path?q=%20', 'http://example.test/a'])
  it(`preserves permitted URL ${url}`, () => {
    expect(safeUrl(url)).toBe(url);
  });

it('artwork uses only the authorized proxy with exact stable IDs', () => {
  expect(safeArtwork('/artwork/11111111-1111-4111-8111-111111111111')).toBe(
    '/artwork/11111111-1111-4111-8111-111111111111',
  );
  for (const value of [
    'https://example.test/a.png',
    '/artwork/../private',
    '/artwork/not-id',
    '/artwork/11111111-1111-4111-8111-111111111111?secret=x',
    null,
  ])
    expect(safeArtwork(value)).toBeUndefined();
  expect(isRecordId('invalid')).toBe(false);
});
