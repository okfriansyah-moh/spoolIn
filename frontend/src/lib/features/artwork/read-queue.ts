import { cancelRead } from '../../transport/read-recovery';

type Waiting = { start: () => void; cancel: () => void };

/** Limit visible poster downloads; obsolete queued cards never start a request. */
export class ArtworkReadQueue {
  private active = 0;
  private waiting: Waiting[] = [];

  constructor(private readonly limit = 2) {}

  async run<T>(read: () => Promise<T>, signal: AbortSignal): Promise<T> {
    await this.acquire(signal);
    try {
      if (signal.aborted) throw cancelRead();
      return await read();
    } finally {
      this.active--;
      this.drain();
    }
  }

  private acquire(signal: AbortSignal): Promise<void> {
    if (signal.aborted) return Promise.reject(cancelRead());
    return new Promise((resolve, reject) => {
      const entry: Waiting = {
        start: () => {
          signal.removeEventListener('abort', entry.cancel);
          this.active++;
          resolve();
        },
        cancel: () => {
          this.waiting = this.waiting.filter((item) => item !== entry);
          reject(cancelRead());
        },
      };
      signal.addEventListener('abort', entry.cancel, { once: true });
      this.waiting.push(entry);
      this.drain();
    });
  }

  private drain(): void {
    while (this.active < this.limit && this.waiting.length)
      this.waiting.shift()?.start();
  }
}
