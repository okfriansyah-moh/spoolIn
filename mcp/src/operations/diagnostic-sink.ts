import { compareText } from './order.js';
import type { Writable } from 'node:stream';
import type { Level } from './diagnostic-encoder.js';
export type DropReason = 'queue_full' | 'sink_failed' | 'contract_invalid';
const levels = new Set(['debug', 'info', 'warning', 'error', 'fatal']);
const reasons = new Set(['queue_full', 'sink_failed', 'contract_invalid']);
type Pending = { line: string; level: Level };

/** One in-flight write and a fixed queue; never writes diagnostics to protocol stdout. */
export class DiagnosticSink {
  private readonly queue: Pending[] = [];
  private pending: Pending | undefined;
  private failed = false;
  private readonly drops = new Map<string, number>();
  constructor(private readonly output: Writable = process.stderr) {
    output.on('error', this.onError);
  }
  drop(level: Level, reason: DropReason): void {
    const valid = levels.has(level) && reasons.has(reason);
    const key = `service="mcp",level="${valid ? level : 'error'}",reason="${valid ? reason : 'contract_invalid'}"`;
    this.drops.set(key, (this.drops.get(key) ?? 0) + 1);
  }
  render(): string[] {
    return [
      '# TYPE spoolin_log_dropped_total counter',
      ...[...this.drops]
        .sort(([left], [right]) => compareText(left, right))
        .map(
          ([labels, count]) => `spoolin_log_dropped_total{${labels}} ${count}`,
        ),
    ];
  }
  send(line: string, level: Level): void {
    if (this.failed) {
      this.drop(level, 'sink_failed');
      return;
    }
    if (this.queue.length >= 1024) {
      this.drop(level, 'queue_full');
      return;
    }
    this.queue.push({ line, level });
    this.pump();
  }
  private pump(): void {
    if (this.pending || this.failed) return;
    const item = this.queue.shift();
    if (!item) return;
    this.pending = item;
    try {
      this.output.write(item.line, (error) => {
        if (this.pending !== item) return;
        this.pending = undefined;
        if (error) {
          this.drop(item.level, 'sink_failed');
          this.onError();
          return;
        }
        setImmediate(() => this.pump()).unref();
      });
    } catch {
      this.onError();
    }
  }
  private readonly onError = (): void => {
    this.failed = true;
    if (this.pending) this.drop(this.pending.level, 'sink_failed');
    this.pending = undefined;
    this.queue
      .splice(0)
      .forEach((item) => this.drop(item.level, 'sink_failed'));
  };
  async flush(timeoutMs = 100): Promise<void> {
    const until = Date.now() + Math.min(100, Math.max(0, timeoutMs));
    while ((this.pending || this.queue.length) && Date.now() < until)
      await new Promise<void>((resolve) => setTimeout(resolve, 1));
    if (this.pending || this.queue.length) this.onError();
  }
}
