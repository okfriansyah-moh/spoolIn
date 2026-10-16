import { TransportFailure } from '../../transport/graphql';
import type { Policy } from '../../policy/types';
import type {
  AccountCommands,
  AccountSubmission,
  FormOutcome,
  IdentityOutcome,
} from './commands';
import {
  identityOf,
  type SessionBootstrap,
  type SessionSource,
  type SessionState,
} from './types';
type IdentityAttempt = {
  id: string;
  kind: AccountSubmission['kind'];
  csrf: string;
  policy: Policy | null;
  generation: number;
  signal: AbortSignal;
};
type LogoutAttempt = {
  id: string;
  csrf: string;
  before: SessionState;
  generation: number;
  signal: AbortSignal;
};
export class SessionController {
  private current: SessionState = Object.freeze({
    kind: 'checking',
    identity: null,
    policy: null,
    message: 'Checking your session',
  });
  private generation = 0;
  private active: AbortController | null = null;
  private hadSession = false;
  private logout: LogoutAttempt | null = null;
  private csrf = '';
  private pendingIdentity: {
    id: string;
    kind: AccountSubmission['kind'];
  } | null = null;
  private readonly listeners = new Set<(state: SessionState) => void>();
  constructor(
    private readonly source: SessionSource & Partial<AccountCommands>,
  ) {}
  get snapshot(): SessionState {
    return this.current;
  }

  subscribe(listener: (state: SessionState) => void): () => void {
    this.listeners.add(listener);
    listener(this.current);
    return () => {
      this.listeners.delete(listener);
    };
  }
  async check(): Promise<void> {
    if (this.logout) return this.reconcileSignOut();
    const generation = this.begin();
    const signal = this.active?.signal;
    if (!signal) return;
    try {
      const response = await this.source.bootstrap(signal);
      if (generation !== this.generation) return;
      this.accept(response);
    } catch {
      if (generation === this.generation)
        this.publish({
          kind: 'unavailable',
          identity: null,
          policy: null,
          message: 'Sign-in is temporarily unavailable',
        });
    }
  }
  private accept(response: SessionBootstrap): void {
    this.csrf = response.session.csrfToken;
    const identity = identityOf(response.session);
    const expired = identity === null && this.hadSession;
    if (identity) this.hadSession = true;
    this.publish({
      kind: identity ? 'verified' : expired ? 'expired' : 'anonymous',
      identity,
      policy: response.policy,
      message: expired ? 'Your session expired' : '',
    });
  }
  async submit(input: AccountSubmission): Promise<FormOutcome> {
    const source = this.source;
    if (!source.submit || !this.canSubmit(input)) {
      return {
        kind: 'failed',
        operationId: '',
        message: 'Check your current session before submitting.',
        retryAfterMs: 0,
      };
    }
    const id = globalThis.crypto.randomUUID();
    const csrf = this.csrf;
    const policy = this.current.policy;
    const generation = this.begin();
    this.pendingIdentity = { id, kind: input.kind };
    const attempt: IdentityAttempt = {
      id,
      kind: input.kind,
      csrf,
      policy,
      generation,
      signal: this.signal(),
    };
    try {
      const result = await source.submit(input, id, csrf, attempt.signal);
      if (generation !== this.generation) return this.stale(id);
      return await this.finishIdentity(result, attempt);
    } catch (error) {
      if (generation !== this.generation) return this.stale(id);
      return error instanceof TransportFailure && error.kind === 'rejected'
        ? this.failed(attempt, error.retryAfterMs)
        : this.uncertain(attempt);
    }
  }
  private canSubmit(input: AccountSubmission): boolean {
    return (
      this.csrf !== '' &&
      this.current.identity === null &&
      this.pendingIdentity === null &&
      (input.kind === 'signIn' || this.logout === null)
    );
  }
  private async finishIdentity(
    result: IdentityOutcome | null,
    attempt: IdentityAttempt,
  ): Promise<FormOutcome> {
    if (result?.state === 'SUCCEEDED') return this.confirmed(attempt);
    if (result?.state === 'FAILED')
      return this.failed(attempt, result.problem?.retryAfterMs ?? 0);
    return this.uncertain(attempt);
  }
  private async confirmed(attempt: IdentityAttempt): Promise<FormOutcome> {
    if (attempt.kind === 'register') {
      this.pendingIdentity = null;
      this.csrf = attempt.csrf;
      const message = 'Your Viewer account is ready. Sign in to continue.';
      this.publish({
        kind: 'anonymous',
        identity: null,
        policy: attempt.policy,
        message,
      });
      return {
        kind: 'registered',
        operationId: attempt.id,
        message,
        retryAfterMs: 0,
      };
    }
    const response = await this.source.bootstrap(attempt.signal);
    if (attempt.generation !== this.generation) return this.stale(attempt.id);
    this.accept(response);
    this.pendingIdentity = null;
    const identity = this.current.identity;
    if (!identity) return this.stale(attempt.id);
    this.logout = null;
    const message = `Signed in as ${identity.role === 'VIEWER' ? 'Viewer' : 'Maintainer'}.`;
    this.publish({ ...this.current, message });
    return {
      kind: 'signedIn',
      operationId: attempt.id,
      message,
      retryAfterMs: 0,
      role: identity.role,
    };
  }
  private failed(attempt: IdentityAttempt, retryAfterMs: number): FormOutcome {
    this.pendingIdentity = null;
    this.csrf = attempt.csrf;
    const message =
      retryAfterMs > 0
        ? 'The request limit has been reached. Wait before trying again.'
        : attempt.kind === 'register'
          ? 'The account could not be created. Check your details or sign in.'
          : 'Sign-in did not complete. Check your email and password.';
    this.publish({
      kind: 'anonymous',
      identity: null,
      policy: attempt.policy,
      message,
    });
    return { kind: 'failed', operationId: attempt.id, message, retryAfterMs };
  }
  private uncertain(attempt: IdentityAttempt): FormOutcome {
    this.csrf = attempt.csrf;
    const message =
      'The result is not confirmed. Check the original request before continuing.';
    this.publish({
      kind: 'uncertain',
      identity: null,
      policy: attempt.policy,
      message,
    });
    return {
      kind: 'uncertain',
      operationId: attempt.id,
      message,
      retryAfterMs: 0,
    };
  }
  private stale(operationId: string): FormOutcome {
    return {
      kind: 'uncertain',
      operationId,
      message: 'The session changed. Check before continuing.',
      retryAfterMs: 0,
    };
  }
  private signal(): AbortSignal {
    if (!this.active) throw new TransportFailure('unavailable');
    return this.active.signal;
  }
  get hasPendingIdentity(): boolean {
    return this.pendingIdentity !== null;
  }
  get hasUnconfirmedLogout(): boolean {
    return this.logout !== null;
  }
  async reconcileIdentity(): Promise<FormOutcome> {
    const pending = this.pendingIdentity;
    if (!pending || !this.source.operation || !this.csrf) return this.stale('');
    const csrf = this.csrf;
    const policy = this.current.policy;
    const generation = this.begin();
    const attempt: IdentityAttempt = {
      ...pending,
      csrf,
      policy,
      generation,
      signal: this.signal(),
    };
    try {
      const result = await this.source.operation(
        pending.id,
        csrf,
        attempt.signal,
      );
      if (generation !== this.generation) return this.stale(pending.id);
      return await this.finishIdentity(result, attempt);
    } catch {
      return generation === this.generation
        ? this.uncertain(attempt)
        : this.stale(pending.id);
    }
  }
  async signOut(): Promise<void> {
    if (this.logout) return this.reconcileSignOut();
    const before = this.current;
    const csrf = this.csrf;
    const source = this.source;
    if (!source.signOut || !this.canSignOut()) return;
    const generation = this.begin();
    const attempt: LogoutAttempt = {
      id: globalThis.crypto.randomUUID(),
      csrf,
      before,
      generation,
      signal: this.signal(),
    };
    this.logout = attempt;
    this.publish({
      kind: 'checking',
      identity: null,
      policy: before.policy,
      message: 'Signing out…',
    });
    try {
      const result = await source.signOut(attempt.id, csrf, attempt.signal);
      if (generation !== this.generation) return;
      if (result.state === 'SUCCEEDED') this.endLogout(attempt);
      else if (result.state === 'FAILED') this.restoreLogout(attempt);
      else this.uncertainLogout(attempt);
    } catch (error) {
      await this.logoutFailure(error, attempt);
    }
  }
  private async logoutFailure(
    error: unknown,
    attempt: LogoutAttempt,
  ): Promise<void> {
    if (attempt.generation !== this.generation) return;
    if (error instanceof TransportFailure && error.kind === 'rejected')
      await this.rejectedLogout(attempt);
    else this.uncertainLogout(attempt);
  }
  private canSignOut(): boolean {
    return (
      this.current.kind === 'verified' &&
      this.current.identity !== null &&
      this.csrf !== ''
    );
  }
  async reconcileSignOut(): Promise<void> {
    const previous = this.logout;
    if (!previous || !this.source.operation) return;
    const generation = this.begin();
    const attempt = { ...previous, generation, signal: this.signal() };
    this.logout = attempt;
    try {
      const result = await this.source.operation(
        attempt.id,
        attempt.csrf,
        attempt.signal,
      );
      const current = await this.source.bootstrap(attempt.signal);
      if (generation !== this.generation) return;
      if (result?.state === 'SUCCEEDED' && !current.session.authenticated)
        this.endLogout(attempt);
      else this.reconcileActiveLogout(attempt, result?.state, current);
    } catch {
      if (generation === this.generation) this.uncertainLogout(attempt);
    }
  }
  private reconcileActiveLogout(
    attempt: LogoutAttempt,
    result: string | undefined,
    current: SessionBootstrap,
  ): void {
    if (
      result === 'FAILED' &&
      current.session.authenticated &&
      current.session.accountId === attempt.before.identity?.accountId
    ) {
      this.logout = null;
      this.accept(current);
      this.publish({ ...this.current, message: 'Sign-out did not complete.' });
    } else this.uncertainLogout(attempt);
  }
  private async rejectedLogout(attempt: LogoutAttempt): Promise<void> {
    try {
      const current = await this.source.bootstrap(attempt.signal);
      if (attempt.generation !== this.generation) return;
      this.logout = null;
      this.accept(current);
      this.publish({
        ...this.current,
        message: current.session.authenticated
          ? 'Sign-out did not complete.'
          : 'Your session expired',
      });
    } catch {
      if (attempt.generation === this.generation) this.uncertainLogout(attempt);
    }
  }
  private restoreLogout(attempt: LogoutAttempt): void {
    this.logout = null;
    const expires = Date.parse(attempt.before.identity?.expiresAt ?? '');
    if (expires <= Date.now()) {
      this.publish({
        kind: 'expired',
        identity: null,
        policy: attempt.before.policy,
        message: 'Your session expired',
      });
      return;
    }
    this.csrf = attempt.csrf;
    this.publish({ ...attempt.before, message: 'Sign-out did not complete.' });
  }
  private endLogout(attempt: LogoutAttempt): void {
    this.logout = null;
    this.pendingIdentity = null;
    this.csrf = '';
    this.hadSession = false;
    this.publish({
      kind: 'anonymous',
      identity: null,
      policy: attempt.before.policy,
      message: 'Signed out.',
    });
  }
  private uncertainLogout(attempt: LogoutAttempt): void {
    this.logout = attempt;
    this.publish({
      kind: 'uncertain',
      identity: null,
      policy: attempt.before.policy,
      message:
        'Sign-out is not confirmed. Protected content stays concealed while the original result is checked.',
    });
  }
  async prepareSignIn(): Promise<void> {
    if (!this.logout) return this.check();
    const generation = this.begin();
    try {
      const current = await this.source.bootstrap(this.signal());
      if (generation !== this.generation) return;
      if (current.session.authenticated) {
        this.uncertainLogout(this.logout);
        return;
      }
      this.accept(current);
      this.publish({
        ...this.current,
        message: 'The earlier sign-out is unconfirmed. You may sign in again.',
      });
    } catch {
      if (generation === this.generation && this.logout)
        this.uncertainLogout(this.logout);
    }
  }
  conceal(): void {
    this.begin();
  }
  dispose(): void {
    this.begin();
    this.listeners.clear();
  }
  private begin(): number {
    this.csrf = '';
    this.generation += 1;
    this.active?.abort();
    this.active = new AbortController();
    this.publish({
      kind: 'checking',
      identity: null,
      policy: null,
      message: 'Checking your session',
    });
    return this.generation;
  }
  private publish(state: SessionState): void {
    this.current = Object.freeze(state);
    this.listeners.forEach((listener) => listener(this.current));
  }
}
