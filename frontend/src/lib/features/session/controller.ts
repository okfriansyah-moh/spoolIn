import {
  identityOf,
  type SessionBootstrap,
  type SessionSource,
  type SessionState,
} from "./types";
export class SessionController {
  private current: SessionState = Object.freeze({
    kind: "checking",
    identity: null,
    policy: null,
    message: "Checking your session",
  });
  private generation = 0;
  private active: AbortController | null = null;
  private hadSession = false;
  private readonly listeners = new Set<(state: SessionState) => void>();
  constructor(private readonly source: SessionSource) {}
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
          kind: "unavailable",
          identity: null,
          policy: null,
          message: "Sign-in is temporarily unavailable",
        });
    }
  }
  private accept(response: SessionBootstrap): void {
    const identity = identityOf(response.session);
    const expired = identity === null && this.hadSession;
    if (identity) this.hadSession = true;
    this.publish({
      kind: identity ? "verified" : expired ? "expired" : "anonymous",
      identity,
      policy: response.policy,
      message: expired ? "Your session expired" : "",
    });
  }
  conceal(): void {
    this.begin();
  }
  dispose(): void {
    this.begin();
    this.listeners.clear();
  }
  private begin(): number {
    this.generation += 1;
    this.active?.abort();
    this.active = new AbortController();
    this.publish({
      kind: "checking",
      identity: null,
      policy: null,
      message: "Checking your session",
    });
    return this.generation;
  }
  private publish(state: SessionState): void {
    this.current = Object.freeze(state);
    this.listeners.forEach((listener) => listener(this.current));
  }
}
