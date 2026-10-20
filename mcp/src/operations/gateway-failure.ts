const messages = {
  FIELD_INVALID: 'Correct the indicated input.',
  UNAUTHENTICATED: 'Sign in again.',
  FORBIDDEN: 'This action is not permitted.',
  NOT_FOUND: 'The requested record is unavailable.',
  STALE_VERSION: 'Review the current version.',
  POLICY_CHANGED: 'Review the current policy.',
  IDEMPOTENCY_CONFLICT: 'Use the original request content.',
  RESOURCE_EXHAUSTED: 'Try a smaller request or retry later.',
  DEADLINE_EXCEEDED: 'Check the original operation before retrying.',
  UNAVAILABLE: 'Try again when the service is available.',
} as const;
type Code = keyof typeof messages;

/** Only fixed public classifications survive a failed downstream RPC. */
export class GatewayFailure extends Error {
  readonly problem;
  constructor(code: Code) {
    super(messages[code]);
    this.name = 'GatewayFailure';
    this.problem = Object.freeze({
      code,
      message: messages[code],
      fields: [],
      retryAfterMs: null,
    });
  }
  static fromGrpc(failure: Error): GatewayFailure {
    const status = 'code' in failure ? failure.code : undefined;
    const details = 'details' in failure ? failure.details : undefined;
    if (status === 9 && ['CHALLENGE_MISMATCH', 'REVISION_MISMATCH', 'CONTENT_HASH_MISMATCH'].includes(String(details)))
      return new GatewayFailure('STALE_VERSION');
    if (status === 9 && ['PROPOSAL_EXPIRED', 'PROPOSAL_NOT_REVIEWABLE'].includes(String(details)))
      return new GatewayFailure('NOT_FOUND');
    if (details === 'POLICY_CHANGED' && status === 9)
      return new GatewayFailure('POLICY_CHANGED');
    const codes = new Map<unknown, Code>([
      [3, 'FIELD_INVALID'],
      [4, 'DEADLINE_EXCEEDED'],
      [5, 'NOT_FOUND'],
      [6, 'IDEMPOTENCY_CONFLICT'],
      [7, 'FORBIDDEN'],
      [8, 'RESOURCE_EXHAUSTED'],
      [9, 'FIELD_INVALID'],
      [10, 'STALE_VERSION'],
      [11, 'FIELD_INVALID'],
      [16, 'UNAUTHENTICATED'],
    ]);
    return new GatewayFailure(codes.get(status) ?? 'UNAVAILABLE');
  }
}
