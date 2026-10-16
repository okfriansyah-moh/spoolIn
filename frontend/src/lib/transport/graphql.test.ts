import { expect, it, vi } from "vitest";
import { createTransport, TransportFailure } from "./graphql";

it("uses same-origin POST no-store and the exact mutation CSRF header without changing a password", async () => {
  const fetcher = vi
    .fn<typeof fetch>()
    .mockResolvedValue(new Response('{"data":{"ok":true}}'));
  const request = createTransport(fetcher);
  const password = "  exact e\u0301 🦉  ";
  await expect(
    request(
      "mutation Test { signIn }",
      { password },
      { mutation: true, csrf: "memory-only" },
    ),
  ).resolves.toEqual({ ok: true });
  const [url, init] = fetcher.mock.calls[0] ?? [];
  expect(url).toBe("/graphql");
  expect(init?.method).toBe("POST");
  expect(init?.credentials).toBe("same-origin");
  expect(init?.cache).toBe("no-store");
  expect(init?.headers).toMatchObject({
    "X-CSRF-Token": "memory-only",
    "X-Spoolin-Client": "web",
  });
  expect(JSON.parse(String(init?.body)).variables.password).toBe(password);
});

it("does not require or send a mutation-only token for reads", async () => {
  const fetcher = vi
    .fn<typeof fetch>()
    .mockResolvedValue(new Response('{"data":{"session":null}}'));
  await createTransport(fetcher)("query Session { session }", {});
  const [, init] = fetcher.mock.calls[0] ?? [];
  expect(init?.headers).not.toHaveProperty("X-CSRF-Token");
});

it("classifies a lost mutation response as uncertain and does not expose transport text", async () => {
  const fetcher = vi
    .fn<typeof fetch>()
    .mockRejectedValue(new Error("private server or credential detail"));
  const failure = await createTransport(fetcher)(
    "mutation Logout { signOut }",
    {},
    { mutation: true, csrf: "memory-only" },
  ).catch((error: unknown) => error);
  expect(failure).toBeInstanceOf(TransportFailure);
  expect(failure).toMatchObject({
    kind: "uncertain",
    message: "The request could not be completed.",
  });
});

it("retains bounded retry guidance while discarding a server supplied private message", async () => {
  const fetcher = vi.fn<typeof fetch>().mockResolvedValue(
    new Response(
      JSON.stringify({
        errors: [
          {
            message: "private account detail",
            extensions: { code: "RATE_LIMITED", retryAfterMs: 1200 },
          },
        ],
      }),
      { status: 429 },
    ),
  );
  await expect(
    createTransport(fetcher)("query Policy { policy }", {}),
  ).rejects.toMatchObject({
    kind: "rejected",
    code: "RATE_LIMITED",
    retryAfterMs: 1200,
  });
});

it("gives identity bootstrap its five second deadline while cancellation still aborts it", async () => {
  vi.useFakeTimers();
  const stop = new AbortController();
  const fetcher = vi.fn<typeof fetch>(
    (_url, init) =>
      new Promise((_resolve, reject) => {
        init?.signal?.addEventListener("abort", () => {
          reject(new Error("aborted"));
        });
      }),
  );
  const result = createTransport(fetcher)(
    "query Session { session }",
    {},
    { identity: true, signal: stop.signal },
  ).catch((error: unknown) => error);
  try {
    await vi.advanceTimersByTimeAsync(3000);
    expect(fetcher.mock.calls[0]?.[1]?.signal?.aborted).toBe(false);
  } finally {
    stop.abort();
    await result;
    vi.useRealTimers();
  }
});

it("rejects a mutation before sending when the in-memory anti-forgery value is missing", async () => {
  const fetcher = vi.fn<typeof fetch>();
  await expect(
    createTransport(fetcher)(
      "mutation Logout { signOut }",
      {},
      { mutation: true },
    ),
  ).rejects.toMatchObject({ kind: "rejected", code: "FORBIDDEN" });
  expect(fetcher).not.toHaveBeenCalled();
});
