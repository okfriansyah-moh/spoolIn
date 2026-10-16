import type { SessionQuery } from "../../generated/graphql";
import { TransportFailure } from "../../transport/graphql";
export function record(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== "object" || Array.isArray(value))
    throw new TransportFailure("unavailable");
  return Object.fromEntries(Object.entries(value));
}
export function uuid(value: unknown): string {
  if (
    typeof value !== "string" ||
    !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(
      value,
    )
  )
    throw new TransportFailure("unavailable");
  return value;
}
export function session(value: unknown): SessionQuery["session"] {
  const input = record(value);
  const fields = [
    "authenticated",
    "role",
    "accountId",
    "displayName",
    "expiresAt",
    "csrfToken",
  ];
  if (
    Object.keys(input).length !== fields.length ||
    fields.some((key) => !(key in input))
  )
    throw new TransportFailure("unavailable");
  if (
    typeof input.authenticated !== "boolean" ||
    typeof input.csrfToken !== "string" ||
    !/^[A-Za-z0-9_-]{43}$/.test(input.csrfToken)
  )
    throw new TransportFailure("unavailable");
  if (!input.authenticated) return anonymous(input);
  return authenticated(input, input.csrfToken);
}
function authenticated(
  input: Record<string, unknown>,
  csrfToken: string,
): SessionQuery["session"] {
  if (input.role !== "VIEWER" && input.role !== "MAINTAINER")
    throw new TransportFailure("unavailable");
  if (
    typeof input.displayName !== "string" ||
    !input.displayName ||
    input.displayName.includes(String.fromCharCode(0)) ||
    /[\uD800-\uDFFF]/u.test(input.displayName)
  )
    throw new TransportFailure("unavailable");
  if (
    typeof input.expiresAt !== "string" ||
    !Number.isFinite(Date.parse(input.expiresAt))
  )
    throw new TransportFailure("unavailable");
  return {
    authenticated: true,
    role: input.role,
    accountId: uuid(input.accountId),
    displayName: input.displayName,
    expiresAt: input.expiresAt,
    csrfToken,
  };
}
function anonymous(input: Record<string, unknown>): SessionQuery["session"] {
  if (
    ["role", "accountId", "displayName", "expiresAt"].some(
      (key) => input[key] !== null,
    )
  )
    throw new TransportFailure("unavailable");
  const csrfToken = input.csrfToken;
  if (typeof csrfToken !== "string") throw new TransportFailure("unavailable");
  return {
    authenticated: false,
    role: null,
    accountId: null,
    displayName: null,
    expiresAt: null,
    csrfToken,
  };
}
