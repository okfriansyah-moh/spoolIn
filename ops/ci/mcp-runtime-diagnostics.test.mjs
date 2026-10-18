import { test } from 'node:test';
import assert from 'node:assert/strict';
import { BoundaryFailure, failureReport, protocolClassification, publicProblem, safeDiagnostics } from './mcp-runtime-diagnostics.mjs';

test('actual finite protocol, HTTP and public problem classifications survive redaction', () => {
  assert.deepEqual(protocolClassification({ code: 503, message: 'private URL' }), { httpStatus: 503 });
  assert.deepEqual(protocolClassification({ code: -32602, data: { token: 'private' } }), { protocolCode: -32602 });
  assert.equal(publicProblem('FORBIDDEN'), 'FORBIDDEN');
  assert.equal(publicProblem('private-key-material'), 'UNCLASSIFIED');
});

test('arbitrary exception properties and body content never enter boundary report', () => {
  const failure = Object.assign(new Error('private credential'), { code: 'secret', details: 'private bearer' });
  assert.deepEqual(failureReport('context', failure), { failureStage: 'context', failureCode: 'PROTOCOL_FAILED' });
  const own = new BoundaryFailure('CONNECTION_FAILED', { httpStatus: 401, token: 'secret', problemCode: 'FORBIDDEN' });
  assert.deepEqual(failureReport('initialize', own), {
    failureStage: 'initialize', failureCode: 'CONNECTION_FAILED', problemCode: 'FORBIDDEN', httpStatus: 401,
  });
});

test('bridge diagnostics retain only fixed fields and cap retained records', () => {
  const record = JSON.stringify({ message: 'private bearer', attributes: {
    event: 'transport_failed', error_class: 'transport', code: 'UNAVAILABLE', token: 'secret',
  } });
  const selected = safeDiagnostics('private\n' + Array(40).fill(record).join('\n'));
  assert.equal(selected.length, 16);
  assert.deepEqual(selected[0], { event: 'transport_failed', errorClass: 'transport', problemCode: 'UNAVAILABLE' });
  assert(!JSON.stringify(selected).includes('secret'));
  assert.deepEqual(safeDiagnostics(JSON.stringify({ attributes: { event: 'private', code: 'secret' } })), []);
});
