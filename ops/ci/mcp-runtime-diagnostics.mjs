const stages = new Set(['configuration', 'initialize', 'list_tools', 'context', 'proposal', 'replay',
  'resource', 'revision', 'receipt', 'publication_denial', 'reconnect', 'reconnect_receipt', 'revoked_initialize']);
const problems = new Set(['FIELD_INVALID', 'UNAUTHENTICATED', 'FORBIDDEN', 'NOT_FOUND', 'STALE_VERSION',
  'POLICY_CHANGED', 'IDEMPOTENCY_CONFLICT', 'RESOURCE_EXHAUSTED', 'DEADLINE_EXCEEDED', 'UNAVAILABLE']);
const kinds = new Set(['ASSERTION_FAILED', 'TOOL_REFUSED', 'CONNECTION_FAILED', 'PROTOCOL_FAILED']);
const events = new Set(['ready', 'stopped', 'startup_failed', 'process_failed', 'request_denied',
  'request_failed', 'transport_failed', 'session_cleanup_failed', 'logger_contract']);
const errorClasses = new Set(['dependency', 'transport', 'configuration', 'internal']);

export class BoundaryFailure extends Error {
  constructor(kind, evidence = {}) {
    super('Actual MCP boundary verification failed');
    this.kind = kinds.has(kind) ? kind : 'PROTOCOL_FAILED';
    this.evidence = evidence;
  }
}

export function publicProblem(value) {
  return problems.has(value) ? value : 'UNCLASSIFIED';
}

export function protocolClassification(failure) {
  const code = failure?.code;
  if (Number.isInteger(code) && code >= 100 && code <= 599) return { httpStatus: code };
  if ([-32700, -32600, -32601, -32602, -32603, -32000, -32001].includes(code)) return { protocolCode: code };
  return {};
}

export function safeDiagnostics(raw) {
  const result = [];
  for (const line of raw.slice(-16_384).split('\n')) {
    try {
      const record = JSON.parse(line);
      const attributes = record.attributes;
      if (!attributes || !events.has(attributes.event)) continue;
      const selected = { event: attributes.event };
      if (errorClasses.has(attributes.error_class)) selected.errorClass = attributes.error_class;
      if (problems.has(attributes.code)) selected.problemCode = attributes.code;
      result.push(selected);
    } catch { /* Non-JSON and private text is never retained. */ }
  }
  return result.slice(-16);
}

export function failureReport(stage, failure) {
  const evidence = failure instanceof BoundaryFailure ? failure.evidence : {};
  const report = { failureStage: stages.has(stage) ? stage : 'configuration',
    failureCode: failure instanceof BoundaryFailure ? failure.kind : 'PROTOCOL_FAILED',
    ...protocolClassification(failure) };
  if (problems.has(evidence.problemCode) || evidence.problemCode === 'UNCLASSIFIED')
    report.problemCode = evidence.problemCode;
  if (Number.isInteger(evidence.httpStatus) && evidence.httpStatus >= 100 && evidence.httpStatus <= 599)
    report.httpStatus = evidence.httpStatus;
  if ([-32700, -32600, -32601, -32602, -32603, -32000, -32001].includes(evidence.protocolCode))
    report.protocolCode = evidence.protocolCode;
  // Re-encode the already bounded classifications, never a caller-supplied diagnostic object.
  if (typeof evidence.diagnostics === 'string') report.bridgeDiagnostics = safeDiagnostics(evidence.diagnostics);
  return report;
}
