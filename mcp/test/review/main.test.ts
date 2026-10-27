// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest';

type ToolReply = {
  structuredContent?: unknown;
  _meta?: unknown;
  isError?: boolean;
};
const host = vi.hoisted(() => ({
  app: {
    connect: vi.fn(),
    callServerTool: vi.fn(),
    ontoolresult: undefined as ((result: ToolReply) => void) | undefined,
    ontoolcancelled: undefined as (() => void) | undefined,
    onteardown: undefined as (() => Promise<unknown>) | undefined,
  },
}));
vi.mock('@modelcontextprotocol/ext-apps', () => ({
  App: class {
    constructor() {
      return host.app;
    }
  },
}));

const handlers = new Map<HTMLElement, EventListener>();
const proposal = () => ({
  proposalId: 'proposal',
  revision: 2,
  contentHash: 'ab'.repeat(32),
  normalizedMovie: {
    title: 'A movie',
    release: { year: 2024 },
    countries: ['ID'],
  },
  credits: [
    {
      personName: 'Alex',
      roleLabel: 'Actor',
      characterNames: ['Lead'],
      billingOrder: 1,
    },
  ],
  sources: [{ field: 'title', url: 'https://source.example/title' }],
  reviewUrl: 'https://spoolin.example/manage/proposals/proposal?revision=2',
});

beforeEach(async () => {
  vi.resetModules();
  host.app.connect.mockReset().mockResolvedValue(undefined);
  host.app.callServerTool
    .mockReset()
    .mockResolvedValue({ structuredContent: { state: 'SUCCEEDED' } });
  handlers.clear();
  document.body.innerHTML = '<main id="app"></main>';
  const original = HTMLElement.prototype.addEventListener;
  vi.spyOn(HTMLElement.prototype, 'addEventListener').mockImplementation(
    function (type, listener, options) {
      if (
        type === 'click' &&
        this.classList.contains('review-card') &&
        typeof listener === 'function'
      ) {
        handlers.set(this, listener);
      }
      original.call(this, type, listener, options);
    },
  );
  await import('../../src/review/main.js');
});
afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
  document.body.replaceChildren();
});

function show(
  metadata: Record<string, unknown> = { challenge: 'private-challenge' },
  changed = {},
) {
  host.app.ontoolresult!({
    structuredContent: { ...proposal(), ...changed },
    _meta: { ui: metadata },
  });
}

function decide(action: 'approve' | 'reject') {
  const card = document.querySelector<HTMLElement>('.review-card')!;
  const button = card.querySelector<HTMLButtonElement>(
    `[data-action="${action}"]`,
  )!;
  // Native-event input is isolated here; these unit tests do not prove host/browser trust.
  handlers
    .get(card)!
    .call(card, { target: button, isTrusted: true } as unknown as Event);
}

it('renders exact values and private controls without displaying the challenge', () => {
  show();
  expect(document.body.textContent).toContain('A movie');
  expect(document.body.textContent).toContain('Alex');
  expect(document.body.textContent).toContain('Lead');
  expect(document.body.textContent).not.toContain('private-challenge');
  expect(document.querySelector('a')?.getAttribute('rel')).toBe(
    'noopener noreferrer',
  );
  expect(document.querySelector('a')?.href).toContain(
    'https://spoolin.example/',
  );
});

it('rejects synthetic approvals and disables controls without a private challenge', () => {
  show();
  document.querySelector<HTMLButtonElement>('[data-action="approve"]')!.click();
  expect(host.app.callServerTool).not.toHaveBeenCalled();
  show({});
  expect(
    document.querySelector<HTMLButtonElement>('[data-action="approve"]')!
      .disabled,
  ).toBe(true);
  expect(document.body.textContent).toContain(
    'Private review controls unavailable',
  );
});

it.each(['approve', 'reject'] as const)(
  'submits one exact %s decision and keeps completed controls disabled',
  async (action) => {
    show();
    decide(action);
    await vi.waitFor(() =>
      expect(document.body.textContent).toContain(
        action === 'approve' ? 'Approved. Movie' : 'Proposal rejected.',
      ),
    );
    expect(host.app.callServerTool).toHaveBeenCalledWith({
      name: 'review_movie_proposal',
      arguments: {
        proposalId: 'proposal',
        revision: 2,
        contentHash: 'ab'.repeat(32),
        challenge: 'private-challenge',
        action,
      },
    });
    decide(action);
    expect(host.app.callServerTool).toHaveBeenCalledTimes(1);
    expect(
      document.querySelector<HTMLButtonElement>('[data-action="approve"]')!
        .disabled,
    ).toBe(true);
  },
);

it('requires the exact included poster preview to load before approval', async () => {
  show(
    {
      challenge: 'private-challenge',
      posterPreview: { data: 'AQID', mimeType: 'image/png' },
    },
    { poster: { assetId: 'poster' } },
  );
  const approve = document.querySelector<HTMLButtonElement>(
    '[data-action="approve"]',
  )!;
  expect(approve.disabled).toBe(true);
  decide('approve');
  expect(host.app.callServerTool).not.toHaveBeenCalled();
  const image = document.querySelector('img')!;
  expect(image.src).toBe('data:image/png;base64,AQID');
  image.dispatchEvent(new Event('load'));
  expect(approve.disabled).toBe(false);
  decide('approve');
  await vi.waitFor(() =>
    expect(host.app.callServerTool).toHaveBeenCalledTimes(1),
  );
});

it('keeps approval disabled when the poster preview is unavailable or fails to load', () => {
  show({ challenge: 'private-challenge' }, { poster: { assetId: 'poster' } });
  expect(
    document.querySelector<HTMLButtonElement>('[data-action="approve"]')!
      .disabled,
  ).toBe(true);
  show(
    {
      challenge: 'private-challenge',
      posterPreview: { data: 'invalid', mimeType: 'image/webp' },
    },
    { poster: {} },
  );
  document.querySelector('img')!.dispatchEvent(new Event('error'));
  expect(document.body.textContent).toContain('Poster preview unavailable');
  expect(
    document.querySelector<HTMLButtonElement>('[data-action="approve"]')!
      .disabled,
  ).toBe(true);
});

it.each([
  ['STALE_VERSION', 'This revision or its review controls changed'],
  ['NOT_FOUND', 'This proposal expired or was already decided'],
  ['UNAUTHENTICATED', 'Reconnect SpoolIn'],
  ['FORBIDDEN', 'Use the owning Maintainer account'],
  ['POLICY_CHANGED', 'The movie policy changed'],
  ['UNKNOWN', 'The decision could not be submitted'],
])(
  'shows safe recovery guidance for %s without upstream private text',
  async (code, message) => {
    host.app.callServerTool.mockResolvedValue({
      isError: true,
      structuredContent: { problem: { code, message: 'private detail' } },
    });
    show();
    decide('approve');
    await vi.waitFor(() =>
      expect(document.body.textContent).toContain(message),
    );
    expect(document.body.textContent).not.toContain('private detail');
  },
);

it('polls a pending decision and reports persisted completion', async () => {
  vi.useFakeTimers();
  host.app.callServerTool
    .mockResolvedValueOnce({
      structuredContent: { state: 'PENDING', operationId: 'operation' },
    })
    .mockResolvedValueOnce({ structuredContent: { state: 'SUCCEEDED' } });
  show();
  decide('approve');
  await vi.advanceTimersByTimeAsync(2000);
  expect(host.app.callServerTool).toHaveBeenLastCalledWith({
    name: 'movie_operation',
    arguments: { operationId: 'operation' },
  });
  expect(document.body.textContent).toContain('Approved. Movie');
});

it('does not report an expired result as successful', async () => {
  host.app.callServerTool.mockResolvedValue({
    structuredContent: { state: 'SUCCEEDED', resultExpired: true },
  });
  show();
  decide('approve');
  await vi.waitFor(() =>
    expect(document.body.textContent).toContain('Decision was not completed'),
  );
});

it('renders an empty queue, ignores malformed entries and cancels stale content', async () => {
  host.app.ontoolresult!({ structuredContent: { proposals: [] } });
  expect(document.body.textContent).toContain(
    'No proposals are awaiting your review',
  );
  host.app.ontoolresult!({
    structuredContent: {
      proposals: [null, { proposalId: 'invalid' }, proposal()],
    },
    _meta: { ui: { items: { proposal: { challenge: 'private' } } } },
  });
  expect(document.querySelectorAll('.review-card')).toHaveLength(1);
  host.app.ontoolcancelled!();
  expect(document.body.textContent).toBe('Proposal review cancelled.');
  await expect(host.app.onteardown!()).resolves.toEqual({});
});
