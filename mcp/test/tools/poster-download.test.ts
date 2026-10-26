import { PassThrough } from 'node:stream';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { posterInput } from '../../src/tools/poster-import.js';

const network = vi.hoisted(() => ({ lookup: vi.fn(), request: vi.fn(), cancellation: vi.fn() }));
vi.mock('node:dns/promises', () => ({ lookup: network.lookup }));
vi.mock('node:https', () => ({ request: network.request }));
vi.mock('../../src/operations/tracing.js', () => ({ activeCancellation: network.cancellation }));

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
  network.cancellation.mockReset().mockReturnValue(undefined);
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


describe('poster network boundary', () => {
  it.each([
    '0.0.0.0', '0.255.255.255', '10.0.0.0', '10.255.255.255',
    '100.64.0.0', '100.127.255.255', '127.0.0.0', '127.255.255.255',
    '169.254.0.0', '169.254.255.255', '172.16.0.0', '172.31.255.255',
    '192.0.0.0', '192.0.0.255', '192.0.2.0', '192.0.2.255',
    '192.88.99.0', '192.88.99.255', '192.168.0.0', '192.168.255.255',
    '198.18.0.0', '198.19.255.255', '198.51.100.0', '198.51.100.255',
    '203.0.113.0', '203.0.113.255', '224.0.0.0', '239.255.255.255',
    '240.0.0.0', '255.255.255.255',
    '2001::', '2001:1ff:ffff:ffff:ffff:ffff:ffff:ffff',
    '2001:db8::', '2001:db8:ffff:ffff:ffff:ffff:ffff:ffff',
    '2002::', '2002:ffff:ffff:ffff:ffff:ffff:ffff:ffff',
    '3fff::', '3fff:fff:ffff:ffff:ffff:ffff:ffff:ffff',
    '::', '::1', '::ffff:127.0.0.1', '::ffff:93.184.216.34', 'fc00::1', 'fe80::1',
  ])('rejects protected literal address %s before DNS or HTTPS', async (address) => {
    const host = address.includes(':') ? `[${address}]` : address;
    await expect(posterInput({ url: `https://${host}/poster` }, 4))
      .rejects.toMatchObject({ problem: { code: 'FORBIDDEN' } });
    expect(network.lookup).not.toHaveBeenCalled();
    expect(network.request).not.toHaveBeenCalled();
  });

  it.each([
    '9.255.255.255', '11.0.0.0', '100.63.255.255', '100.128.0.0',
    '172.15.255.255', '172.32.0.0', '192.167.255.255', '192.169.0.0',
    '198.17.255.255', '198.20.0.0', '93.184.216.34', '2606:4700:4700::1111',
  ])('pins permitted literal address %s', async (address) => {
    replies({});
    const host = address.includes(':') ? `[${address}]` : address;
    await expect(posterInput({ url: `https://${host}/poster` }, 4))
      .resolves.toMatchObject({ mediaType: 'image/jpeg' });
    const selected = vi.fn();
    network.request.mock.calls[0]![1].lookup(host, {}, selected);
    expect(selected).toHaveBeenCalledWith(null, address, address.includes(':') ? 6 : 4);
    expect(network.lookup).not.toHaveBeenCalled();
  });

  it.each(['http://example.test/x', 'https://name:secret@example.test/x', 'https://example.test:8443/x'])('rejects unsafe URL shape before network access %s', async (url) => {
    await expect(posterInput({ url }, 4))
      .rejects.toMatchObject({ problem: { code: 'FIELD_INVALID' } });
    expect(network.lookup).not.toHaveBeenCalled();
    expect(network.request).not.toHaveBeenCalled();
  });

  it.each(['caller', 'deadline'])('aborts DNS waiting on %s cancellation', async (source) => {
    const controller = new AbortController();
    const timer = vi.spyOn(AbortSignal, 'timeout').mockReturnValue(controller.signal);
    if (source === 'caller') network.cancellation.mockReturnValue(controller.signal);
    network.lookup.mockImplementation(() => new Promise(() => {}));
    try {
      const pending = posterInput({ url: 'https://images.example.test/poster' }, 4);
      const rejected = expect(pending).rejects.toMatchObject({ problem: { code: 'DEADLINE_EXCEEDED' } });
      controller.abort();
      await rejected;
      expect(timer).toHaveBeenCalledWith(15_000);
      expect(network.request).not.toHaveBeenCalled();
    } finally { timer.mockRestore(); }
  });
});
