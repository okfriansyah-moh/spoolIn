import type { GraphqlRequest } from '../../../transport/graphql';
import { TransportFailure } from '../../../transport/graphql';
import { fetchManagedPeople } from './api';
import type { ListState } from './types';

export class PersonListController {
  private current: ListState = { kind: 'loading' };
  private readonly listeners = new Set<(state: ListState) => void>();

  constructor(private readonly request: GraphqlRequest) {}

  subscribe(listener: (state: ListState) => void): () => void {
    this.listeners.add(listener);
    listener(this.current);
    return () => this.listeners.delete(listener);
  }

  async load(csrf: string, signal: AbortSignal): Promise<void> {
    this.publish({ kind: 'loading' });
    try {
      const people = await fetchManagedPeople(this.request, csrf, signal);
      if (signal.aborted) return;
      this.publish({ kind: 'ready', people });
    } catch {
      if (signal.aborted) return;
      void TransportFailure;
      this.publish({ kind: 'failure' });
    }
  }

  dispose(): void {
    this.listeners.clear();
  }

  private publish(state: ListState): void {
    this.current = Object.freeze(state);
    this.listeners.forEach((listener) => listener(this.current));
  }
}

export function createPersonListController(
  request: GraphqlRequest,
): PersonListController {
  return new PersonListController(request);
}
