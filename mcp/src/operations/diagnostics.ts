import { DiagnosticSink } from './diagnostic-sink.js';
import { encodeDiagnostic, events } from './diagnostic-encoder.js';
import type { Event, Resource } from './diagnostic-encoder.js';

export class McpDiagnostics {
  private debug = false;
  private resource: Resource = { 'service.name': 'mcp', 'service.namespace': 'spoolin',
    'service.version': '0.0.0-foundation', 'service.env': 'local' };
  constructor(readonly sink = new DiagnosticSink()) {}
  configure(environment: NodeJS.ProcessEnv = process.env): void {
    const level = environment.LOG_LEVEL ?? 'info';
    const version = environment.SPOOLIN_BUILD_VERSION ?? '0.0.0-foundation';
    const selected = environment.SPOOLIN_ENVIRONMENT ?? 'local';
    if (!['debug', 'info'].includes(level) || !/^[A-Za-z0-9][A-Za-z0-9._+-]{0,127}$/.test(version) ||
      !['local', 'test'].includes(selected)) throw new Error('Invalid diagnostic configuration');
    this.debug = level === 'debug';
    this.resource = { ...this.resource, 'service.version': version, 'service.env': selected as 'local' | 'test' };
  }
  emit(event: Event, attributes: Readonly<Record<string, unknown>> = {}): void {
    try {
      if (events[event]?.[0] === 'debug' && !this.debug) return;
      this.sink.send(encodeDiagnostic(event, this.resource, attributes), events[event][0]);
    } catch {
      this.sink.drop('error', 'contract_invalid');
      this.sink.send(encodeDiagnostic('logger_contract', this.resource, {}), 'error');
    }
  }
}
export const mcpDiagnostics = new McpDiagnostics();

/** Startup and process failures share the diagnostic encoder and preserve a failing exit. */
class ProcessLifecycle {
  private close = async () => {};
  private stopping: Promise<void> | undefined;
  constructor() {
    process.on('uncaughtException', this.fatal);
    process.on('unhandledRejection', this.fatal);
    process.once('SIGTERM', () => { void this.stop(false); });
    process.once('SIGINT', () => { void this.stop(false); });
  }
  private readonly fatal = (): void => { mcpDiagnostics.emit('process_failed'); void this.stop(true); };
  async start(start: (register: (close: () => Promise<void>) => void) => Promise<void>): Promise<void> {
    try { mcpDiagnostics.configure(); await start((shutdown) => { this.close = shutdown; }); }
    catch { mcpDiagnostics.emit('startup_failed'); await this.stop(true); }
  }
  private stop(failed: boolean): Promise<void> {
    if (failed) process.exitCode = 1;
    this.stopping ??= this.shutdown();
    return this.stopping;
  }
  private async shutdown(): Promise<void> {
    const deadline = setTimeout(() => {
      mcpDiagnostics.emit('process_failed');
      void mcpDiagnostics.sink.flush().finally(() => process.exit(1));
    }, 30_000).unref();
    try { await this.close(); mcpDiagnostics.emit('stopped'); }
    catch { process.exitCode = 1; mcpDiagnostics.emit('process_failed'); }
    finally { clearTimeout(deadline); await mcpDiagnostics.sink.flush(); }
  }
}
export async function runMcpProcess(start: (register: (close: () => Promise<void>) => void) => Promise<void>): Promise<void> {
  await new ProcessLifecycle().start(start);
}
