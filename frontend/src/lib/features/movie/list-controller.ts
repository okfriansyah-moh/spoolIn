import type { GraphqlRequest } from '../../transport/graphql';
import { fetchManagedTitles } from './api';
import type { ListState } from './types';

export class MovieListController {
  private current: ListState = { kind: 'loading' };
  private readonly listeners = new Set<(state: ListState) => void>();

  constructor(private readonly request: GraphqlRequest) {}

  get snapshot(): ListState {
    return this.current;
  }

  subscribe(listener: (state: ListState) => void): () => void {
    this.listeners.add(listener);
    listener(this.current);
    return () => this.listeners.delete(listener);
  }

  async load(csrf: string, signal: AbortSignal): Promise<void> {
    this.publish({ kind: 'loading' });
    try {
      const titles = await fetchManagedTitles(this.request, csrf, signal);
      if (signal.aborted) return;
      this.publish({ kind: 'ready', titles });
    } catch (_error) {
      if (signal.aborted) return;
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

export function createMovieListController(
  request: GraphqlRequest,
): MovieListController {
  return new MovieListController(request);
}
