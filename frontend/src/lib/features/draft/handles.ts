/** Tracks object URLs and pending reads so concealment can release them. */
export class DraftHandles {
  private readonly urls = new Set<string>();
  private readonly reads = new Set<AbortController>();
  private revoked = 0;

  trackObjectUrl(url: string): string {
    this.urls.add(url);
    return url;
  }

  trackRead(controller: AbortController): AbortSignal {
    this.reads.add(controller);
    return controller.signal;
  }

  release(): void {
    for (const url of this.urls) {
      URL.revokeObjectURL(url);
      this.revoked += 1;
    }
    this.urls.clear();
    for (const read of this.reads) read.abort();
    this.reads.clear();
  }

  get releasedCount(): number {
    return this.revoked;
  }
}
