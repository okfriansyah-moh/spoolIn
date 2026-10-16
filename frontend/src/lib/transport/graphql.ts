export type Fetcher = typeof globalThis.fetch;
export type RequestOptions = {
  mutation?: boolean;
  identity?: boolean;
  csrf?: string;
  signal?: AbortSignal;
};
export class TransportFailure extends Error {
  constructor(
    readonly kind: "rejected" | "unavailable" | "uncertain",
    readonly code = "UNAVAILABLE",
    readonly retryAfterMs = 0,
  ) {
    super("The request could not be completed.");
  }
}
function object(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== "object" || Array.isArray(value))
    throw new TransportFailure("unavailable");
  return Object.fromEntries(Object.entries(value));
}
function rejected(errors: unknown, ambiguous: boolean): TransportFailure {
  const first = Array.isArray(errors) ? errors[0] : undefined;
  const extension = object(object(first).extensions);
  const allowed = [
    "RATE_LIMITED",
    "UNAUTHENTICATED",
    "FORBIDDEN",
    "VALIDATION_FAILED",
    "CONFLICT",
  ];
  const code =
    typeof extension.code === "string" && allowed.includes(extension.code)
      ? extension.code
      : "UNAVAILABLE";
  const retry = extension.retryAfterMs;
  const retryAfterMs =
    typeof retry === "number" &&
    Number.isInteger(retry) &&
    retry > 0 &&
    retry <= 2147483647
      ? retry
      : 0;
  return new TransportFailure(
    ambiguous ? "uncertain" : "rejected",
    code,
    retryAfterMs,
  );
}
async function decode(response: Response, mutation: boolean): Promise<unknown> {
  const text = await response.text();
  if (new TextEncoder().encode(text).length > 1048576)
    throw new TransportFailure(mutation ? "uncertain" : "unavailable");
  const body = object(JSON.parse(text));
  if (body.errors !== undefined)
    throw rejected(body.errors, mutation && response.status >= 500);
  if (!response.ok || !("data" in body))
    throw new TransportFailure(mutation ? "uncertain" : "unavailable");
  return body.data;
}
function requestHeaders(options: RequestOptions): Record<string, string> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/graphql-response+json, application/json",
    "X-Spoolin-Client": "web",
  };
  if (options.csrf) headers["X-CSRF-Token"] = options.csrf;
  return headers;
}
function requestDeadline(options: RequestOptions) {
  const controller = new AbortController();
  const abort = () => controller.abort();
  const signal = options.signal;
  if (signal) {
    if (signal.aborted) abort();
    signal.addEventListener("abort", abort, { once: true });
  }
  const timer = setTimeout(
    abort,
    options.mutation || options.identity ? 5000 : 3000,
  );
  return {
    signal: controller.signal,
    cleanup() {
      clearTimeout(timer);
      signal?.removeEventListener("abort", abort);
    },
  };
}
class GraphqlTransport {
  constructor(private readonly fetcher: Fetcher) {}

  async request(
    query: string,
    variables: Record<string, unknown>,
    options: RequestOptions = {},
  ): Promise<unknown> {
    const mutation = options.mutation === true;
    if (mutation && !options.csrf)
      throw new TransportFailure("rejected", "FORBIDDEN");
    const deadline = requestDeadline(options);
    try {
      const response = await this.fetcher("/graphql", {
        method: "POST",
        credentials: "same-origin",
        cache: "no-store",
        headers: requestHeaders(options),
        body: JSON.stringify({ query, variables }),
        signal: deadline.signal,
      });
      return await decode(response, mutation);
    } catch (error) {
      if (error instanceof TransportFailure && error.kind !== "unavailable")
        throw error;
      throw new TransportFailure(mutation ? "uncertain" : "unavailable");
    } finally {
      deadline.cleanup();
    }
  }
}
export function createTransport(fetcher: Fetcher = globalThis.fetch) {
  const transport = new GraphqlTransport(fetcher);
  return transport.request.bind(transport);
}
export type GraphqlRequest = ReturnType<typeof createTransport>;
