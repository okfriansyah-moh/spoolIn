import { PassThrough } from 'node:stream';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { posterInput } from '../../src/tools/poster-import.js';

const network = vi.hoisted(() => ({ lookup: vi.fn(), request: vi.fn() }));
vi.mock('node:dns/promises', () => ({ lookup: network.lookup }));
vi.mock('node:https', () => ({ request: network.request }));

type Reply = {
  status?: number;
  headers?: Record<string, string>;
  bytes?: Buffer;
};
const image = Buffer.from([0xff, 0xd8, 0xff, 0x01]);

function replies(...responses: Reply[]) {
  for (const response of responses) {
    network.request.mockImplementationOnce(
      (
        _url: URL,
        _options: unknown,
        receive: (stream: PassThrough) => void,
      ) => ({
        once: vi.fn(),
        end: () => {
          const stream = Object.assign(new PassThrough(), {
            statusCode: response.status ?? 200,
            headers: response.headers ?? {},
          });
          queueMicrotask(() => {
            receive(stream);
            stream.end(response.bytes ?? image);
          });
        },
      }),
    );
  }
}

beforeEach(() => {
  vi.clearAllMocks();
  network.lookup
    .mockReset()
    .mockResolvedValue([{ address: '93.184.216.34', family: 4 }]);
  network.request.mockReset();
});

describe('bounded remote poster import', () => {
  it('pins an allowed DNS answer and requests uncompressed image bytes', async () => {
    replies({ headers: { 'content-length': '4' } });
    await expect(
      posterInput({ url: 'https://images.example.test/poster#fragment' }, 4),
    ).resolves.toEqual({
      bytes: image,
      mediaType: 'image/jpeg',
      sourceUrl: 'https://images.example.test/poster#fragment',
    });
    const [url, options] = network.request.mock.calls[0]!;
    expect(url.href).toBe('https://images.example.test/poster');
    expect(options).toMatchObject({
      agent: false,
      family: 4,
      headers: {
        accept: 'image/jpeg,image/png,image/webp',
        'accept-encoding': 'identity',
      },
    });
    const selected = vi.fn();
    options.lookup('images.example.test', {}, selected);
    expect(selected).toHaveBeenCalledWith(null, '93.184.216.34', 4);
  });

  it.each([
    { addresses: [] },
    {
      addresses: [
        { address: '93.184.216.34', family: 4 },
        { address: '127.0.0.1', family: 4 },
      ],
    },
  ])(
    'rejects an empty or mixed public/private DNS answer',
    async ({ addresses }) => {
      network.lookup.mockResolvedValue(addresses);
      await expect(
        posterInput({ url: 'https://images.example.test/poster' }, 1024),
      ).rejects.toMatchObject({ problem: { code: 'FORBIDDEN' } });
      expect(network.request).not.toHaveBeenCalled();
    },
  );

  it('revalidates each redirect and imports a relative HTTPS location', async () => {
    replies(
      { status: 302, headers: { location: '/new-poster' } },
      { bytes: image },
    );
    await expect(
      posterInput({ url: 'https://images.example.test/old' }, 1024),
    ).resolves.toMatchObject({ mediaType: 'image/jpeg' });
    expect(network.request.mock.calls[1]![0].href).toBe(
      'https://images.example.test/new-poster',
    );
    expect(network.lookup).toHaveBeenCalledTimes(2);
  });

  it('blocks a redirect to private infrastructure before another request', async () => {
    replies({
      status: 307,
      headers: { location: 'https://169.254.169.254/private' },
    });
    await expect(
      posterInput({ url: 'https://images.example.test/poster' }, 1024),
    ).rejects.toMatchObject({ problem: { code: 'FORBIDDEN' } });
    expect(network.request).toHaveBeenCalledTimes(1);
  });

  it.each([
    { status: 302 },
    { status: 404 },
    { headers: { 'content-encoding': 'gzip' } },
  ])(
    'rejects an unusable response without accepting arbitrary bytes',
    async (reply) => {
      replies(reply);
      await expect(
        posterInput({ url: 'https://images.example.test/poster' }, 1024),
      ).rejects.toMatchObject({ problem: { code: 'FIELD_INVALID' } });
    },
  );

  it('bounds redirect chains rather than following them indefinitely', async () => {
    replies(
      ...Array.from({ length: 4 }, () => ({
        status: 301,
        headers: { location: '/again' },
      })),
    );
    await expect(
      posterInput({ url: 'https://images.example.test/poster' }, 1024),
    ).rejects.toMatchObject({ problem: { code: 'FIELD_INVALID' } });
    expect(network.request).toHaveBeenCalledTimes(4);
  });

  it.each([{ headers: { 'content-length': '5' } }, { bytes: Buffer.alloc(5) }])(
    'enforces declared and streaming byte limits',
    async (reply) => {
      replies(reply);
      await expect(
        posterInput({ url: 'https://images.example.test/poster' }, 4),
      ).rejects.toMatchObject({ problem: { code: 'RESOURCE_EXHAUSTED' } });
    },
  );

  it('bounds concurrent downloads and releases admission after failed DNS resolution', async () => {
    const pending: Array<(value: []) => void> = [];
    network.lookup.mockImplementation(
      () => new Promise<[]>((resolve) => pending.push(resolve)),
    );
    const active = Array.from({ length: 4 }, () =>
      posterInput({ url: 'https://images.example.test/poster' }, 4),
    );
    const results = Promise.allSettled(active);
    await expect(
      posterInput({ url: 'https://images.example.test/fifth' }, 4),
    ).rejects.toMatchObject({ problem: { code: 'RESOURCE_EXHAUSTED' } });
    for (const resolve of pending) resolve([]);
    expect(
      (await results).every((result) => result.status === 'rejected'),
    ).toBe(true);
    network.lookup.mockResolvedValue([{ address: '93.184.216.34', family: 4 }]);
    replies({});
    await expect(
      posterInput({ url: 'https://images.example.test/retry' }, 4),
    ).resolves.toMatchObject({ mediaType: 'image/jpeg' });
  });
});
