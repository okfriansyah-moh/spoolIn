import { describe, expect, it } from 'vitest';
import {
  decodePoster,
  MAX_POSTER_INPUT_BYTES,
  posterInput,
} from '../../src/tools/poster-import.js';

const jpeg = Buffer.from([0xff, 0xd8, 0xff, 0x01]);
const png = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
const webp = Buffer.from('RIFF1234WEBP');

describe('poster byte validation', () => {
  it.each([jpeg, png, webp])(
    'preserves valid bytes rather than reinterpreting their content',
    (bytes) => {
      expect(decodePoster(bytes.toString('base64'), bytes.length)).toEqual(
        bytes,
      );
    },
  );

  it.each(['', '***', 'YQ', 'YQ===', 'YQ==\n', 'YR=='])(
    'rejects malformed or ambiguous base64 %j',
    (content) => {
      expect(() => decodePoster(content, 1024)).toThrow();
    },
  );

  it('enforces decoded-byte and encoded-input bounds at their exact limits', () => {
    expect(decodePoster(jpeg.toString('base64'), 4)).toEqual(jpeg);
    expect(() => decodePoster(jpeg.toString('base64'), 3)).toThrow();
    expect(() => decodePoster(Buffer.alloc(6).toString('base64'), 4)).toThrow();
  });

  it.each([
    [jpeg, 'image/jpeg'],
    [png, 'image/png'],
    [webp, 'image/webp'],
  ] as const)(
    'detects supported image signatures and retains attribution',
    async (bytes, mediaType) => {
      await expect(
        posterInput(
          {
            content: bytes.toString('base64'),
            mediaType,
            sourceUrl: 'https://example.test/poster',
          },
          100,
        ),
      ).resolves.toEqual({
        bytes,
        mediaType,
        sourceUrl: 'https://example.test/poster',
      });
    },
  );

  it('rejects bytes whose signature disagrees with the claimed format', async () => {
    await expect(
      posterInput(
        { content: jpeg.toString('base64'), mediaType: 'image/png' },
        100,
      ),
    ).rejects.toMatchObject({ problem: { code: 'FIELD_INVALID' } });
    await expect(
      posterInput(
        { content: Buffer.from('plain text').toString('base64') },
        100,
      ),
    ).rejects.toMatchObject({ problem: { code: 'FIELD_INVALID' } });
  });

  it('caps imported content at the processing ceiling even when the policy is larger', async () => {
    const content = 'A'.repeat(Math.ceil(MAX_POSTER_INPUT_BYTES / 3) * 4 + 4);
    await expect(
      posterInput({ content }, MAX_POSTER_INPUT_BYTES + 100),
    ).rejects.toMatchObject({ problem: { code: 'RESOURCE_EXHAUSTED' } });
  });
});

describe('poster URL boundary', () => {
  it.each([
    'invalid',
    'http://example.test/image',
    'https://user:password@example.test/image',
    'https://example.test:8443/image',
  ])('rejects unsafe URL %s before network I/O', async (url) => {
    await expect(posterInput({ url }, 1024)).rejects.toMatchObject({
      problem: { code: 'FIELD_INVALID' },
    });
  });

  it.each([
    '127.0.0.1',
    '10.1.2.3',
    '192.168.1.1',
    '169.254.169.254',
    '100.64.1.2',
    '192.0.2.1',
    '198.51.100.1',
    '203.0.113.1',
    '[::1]',
    '[fc00::1]',
    '[2001:db8::1]',
  ])(
    'blocks private and reserved destination %s before HTTPS I/O',
    async (address) => {
      await expect(
        posterInput({ url: `https://${address}/image` }, 1024),
      ).rejects.toMatchObject({ problem: { code: 'FORBIDDEN' } });
    },
  );
});
