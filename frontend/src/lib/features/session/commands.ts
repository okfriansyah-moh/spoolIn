import type { ActorRole } from '../../generated/graphql';
export type AccountSubmission =
  | { kind: 'register'; name: string; email: string; password: string }
  | { kind: 'signIn'; email: string; password: string };
export type SafeProblem = Readonly<{ code: string; retryAfterMs: number }>;
export type IdentityOutcome = Readonly<{
  operationId: string;
  state: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED';
  resultExpired: boolean;
  problem: SafeProblem | null;
}>;
export type FormOutcome = Readonly<{
  kind: 'registered' | 'signedIn' | 'failed' | 'uncertain';
  operationId: string;
  message: string;
  retryAfterMs: number;
  role?: ActorRole;
}>;
export interface AccountCommands {
  submit(
    input: AccountSubmission,
    operationId: string,
    csrf: string,
    signal: AbortSignal,
  ): Promise<IdentityOutcome>;
  signOut(
    operationId: string,
    csrf: string,
    signal: AbortSignal,
  ): Promise<IdentityOutcome>;
  operation(
    operationId: string,
    csrf: string,
    signal: AbortSignal,
  ): Promise<IdentityOutcome | null>;
}
