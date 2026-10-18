import { Writable } from 'node:stream';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { DiagnosticSink } from '../../src/operations/diagnostic-sink.js';
import { encodeDiagnostic } from '../../src/operations/diagnostic-encoder.js';
import type { Event, Resource } from '../../src/operations/diagnostic-encoder.js';
import { McpDiagnostics, mcpDiagnostics } from '../../src/operations/diagnostics.js';
import { McpMetrics } from '../../src/operations/metrics.js';

const resource: Resource = { 'service.name': 'mcp', 'service.namespace': 'spoolin',
  'service.version': 'test-build', 'service.env': 'test' };
function memory() {
  const lines: string[] = [];
  const output = new Writable({ write(chunk, _encoding, done) { lines.push(chunk.toString()); done(); } });
  const sink = new DiagnosticSink(output);
  return { lines, sink, logger: new McpDiagnostics(sink) };
}

describe('bounded MCP stderr diagnostics', () => {
  it('serializes fixed summaries and resource while omitting hostile fields and multiline private content', async () => {
    const fixture = memory();
    fixture.logger.configure({ LOG_LEVEL: 'debug', SPOOLIN_ENVIRONMENT: 'test', SPOOLIN_BUILD_VERSION: 'verified-build' });
    fixture.logger.emit('request_failed', { operation: 'propose_movie', code: 'UNAVAILABLE', password: 'secret\nFORGED',
      message: 'private draft', method: 'POST\r\nsecret', error_class: new Error('secret exception'),
      correlation_id: '11111111-1111-1111-1111-111111111111' });
    await fixture.sink.flush();
    expect(fixture.lines).toHaveLength(1);
    const line = fixture.lines[0]!;
    expect(Buffer.byteLength(line)).toBeLessThanOrEqual(16384);
    expect(line.split('\n')).toHaveLength(2);
    expect(line).not.toMatch(/secret|FORGED|private draft|stack/);
    const value = JSON.parse(line);
    expect(value).toMatchObject({ level: 'error', message: 'MCP request failed', trace_id: '0'.repeat(32),
      span_id: '0'.repeat(16), resource: { 'service.name': 'mcp', 'service.version': 'verified-build' },
      dropped_attributes_count: 4 });
  });
  it('validates configuration and suppresses optional debug while preserving mandatory outcomes', async () => {
    const fixture = memory();
    for (const LOG_LEVEL of ['', 'trace', 'off']) expect(() => fixture.logger.configure({ LOG_LEVEL })).toThrow();
    fixture.logger.emit('branch_debug'); fixture.logger.emit('ready');
    await fixture.sink.flush();
    expect(fixture.lines).toHaveLength(1);
    fixture.logger.configure({ LOG_LEVEL: 'debug' }); fixture.logger.emit('branch_debug');
    await fixture.sink.flush();
    expect(fixture.lines.map((line) => JSON.parse(line).level)).toEqual(['info', 'debug']);
    expect(() => encodeDiagnostic('ready', { ...resource, 'service.version': 'x'.repeat(129) }, {})).toThrow();
  });
  it('replaces invalid records with a small contract event without echoing rejected values', async () => {
    const fixture = memory();
    fixture.logger.emit('secret-event' as Event, { secret: 'private canary' });
    await fixture.sink.flush();
    expect(fixture.lines.join('')).not.toContain('private canary');
    expect(JSON.parse(fixture.lines[0]!).attributes.event).toBe('logger_contract');
    expect(fixture.sink.render().join('\n')).toContain('reason="contract_invalid"} 1');
    expect(() => encodeDiagnostic('toString' as Event, resource, {})).toThrow();
  });
  it('bounds a stalled sink queue and counts actual losses without blocking request code', async () => {
    const output = new Writable({ write() { /* Deliberately blocked owned sink. */ } });
    const sink = new DiagnosticSink(output);
    for (let index = 0; index < 1026; index += 1) sink.send(encodeDiagnostic('ready', resource, {}), 'info');
    expect(sink.render().join('\n')).toContain('reason="queue_full"} 1');
    await sink.flush(0);
    expect(sink.render().join('\n')).toContain('reason="sink_failed"} 1025');
    output.destroy();
  });
  it('handles real Writable failures and exposes drop counters through the private exporter render', async () => {
    const output = new Writable({ write(_chunk, _encoding, done) { done(new Error('secret sink detail')); } });
    const sink = new DiagnosticSink(output);
    new McpDiagnostics(sink).emit('ready');
    await sink.flush();
    expect(sink.render().join('\n')).toContain('reason="sink_failed"} 1');
    mcpDiagnostics.sink.drop('warning', 'queue_full');
    expect(new McpMetrics().render(0, 0, true)).toContain('spoolin_log_dropped_total{service="mcp",level="warning",reason="queue_full"}');
  });
  it('fails stdio startup with JSON stderr and empty protocol stdout without leaking raw configuration errors', async () => {
    const script = fileURLToPath(new URL('../../dist/transport/stdio.js', import.meta.url));
    const env = { ...process.env, LOG_LEVEL: 'invalid-private-canary' };
    delete env.MCP_CONNECTOR_TOKEN_FILE;
    const child = spawn(process.execPath, [script], { env, stdio: ['pipe', 'pipe', 'pipe'] });
    let stdout = ''; let stderr = '';
    child.stdout.on('data', (chunk) => { stdout += String(chunk); });
    child.stderr.on('data', (chunk) => { stderr += String(chunk); });
    child.stdin.end();
    const exit = await new Promise<number | null>((resolve) => child.once('close', resolve));
    expect(exit).toBe(1); expect(stdout).toBe('');
    expect(stderr).not.toMatch(/invalid-private-canary|Error:| at |MCP_CONNECTOR_TOKEN_FILE/);
    const records = stderr.trim().split('\n').map((line) => JSON.parse(line));
    expect(records.some((record) => record.level === 'fatal')).toBe(true);
  });
  it('redacts uncaught asynchronous failures and exits after actual registered cleanup', async () => {
    const module = new URL('../../dist/operations/diagnostics.js', import.meta.url).href;
    const code = `import { runMcpProcess } from ${JSON.stringify(module)};
      await runMcpProcess(async (register) => {
        register(async () => {});
        queueMicrotask(() => { throw new Error('private asynchronous canary'); });
      });`;
    const child = spawn(process.execPath, ['--input-type=module', '-e', code], {
      env: { ...process.env, LOG_LEVEL: 'info' }, stdio: ['pipe', 'pipe', 'pipe'],
    });
    let stdout = ''; let stderr = '';
    child.stdout.on('data', (chunk) => { stdout += String(chunk); });
    child.stderr.on('data', (chunk) => { stderr += String(chunk); }); child.stdin.end();
    expect(await new Promise<number | null>((resolve) => child.once('close', resolve))).toBe(1);
    expect(stdout).toBe(''); expect(stderr).not.toMatch(/private asynchronous canary|Error:| at /);
    expect(stderr.trim().split('\n').map((line) => JSON.parse(line).level)).toContain('fatal');
  });
  it('clamps diagnostic drop labels even for an invalid internal caller', () => {
    const fixture = memory();
    fixture.sink.drop('secret-level' as 'error', 'secret-reason' as 'contract_invalid');
    const metric = fixture.sink.render().join('\n');
    expect(metric).toContain('level="error",reason="contract_invalid"} 1');
    expect(metric).not.toContain('secret');
  });

});
