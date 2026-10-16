import type { ActorRole, SessionQuery } from "../../generated/graphql";
import type { Policy } from "../../policy/types";
export type Identity = Readonly<{
  accountId: string;
  displayName: string;
  role: ActorRole;
  expiresAt: string;
}>;
export type SessionKind =
  | "checking"
  | "anonymous"
  | "verified"
  | "unavailable"
  | "expired"
  | "uncertain";
export type SessionState = Readonly<{
  kind: SessionKind;
  identity: Identity | null;
  policy: Policy | null;
  message: string;
}>;
export type SessionBootstrap = {
  session: SessionQuery["session"];
  policy: Policy;
};
export interface SessionSource {
  bootstrap(signal: AbortSignal): Promise<SessionBootstrap>;
}
export function identityOf(session: SessionQuery["session"]): Identity | null {
  if (!session.authenticated) return null;
  const { accountId, displayName, role, expiresAt } = session;
  if (!accountId || !displayName || !role || !expiresAt)
    throw new Error("Session verification unavailable.");
  return Object.freeze({ accountId, displayName, role, expiresAt });
}
