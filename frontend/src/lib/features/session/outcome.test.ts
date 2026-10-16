import { expect, it } from 'vitest';
import { mutation, operation, safeProblem } from './outcome';
const id = '10000000-0000-4000-8000-000000000001';
const result = { id, state: 'SUCCEEDED', resultExpired: false, problem: null };
it.each(['PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED'])(
  'accepts bounded operation state %s without adding private data',
  (state) => {
    expect(operation({ ...result, state, password: 'ignored' }, id)).toEqual({
      operationId: id,
      state,
      resultExpired: false,
      problem: null,
    });
  },
);
it.each([
  { id: 'wrong' },
  { id: '20000000-0000-4000-8000-000000000002' },
  { state: 'OTHER' },
  { resultExpired: null },
  { problem: { code: 'FAILED', retryAfterMs: null } },
])('rejects incoherent or foreign operation projection', (change) => {
  expect(() => operation({ ...result, ...change }, id)).toThrow();
});
it.each([
  { code: 'unsafe text', retryAfterMs: null },
  { code: 'FAILED', retryAfterMs: -1 },
  { code: 'FAILED', retryAfterMs: 1.1 },
  { code: 'FAILED', retryAfterMs: 2147483648 },
])('rejects unsafe feedback fields', (value) => {
  expect(() => safeProblem(value)).toThrow();
});
it('retains bounded retry hints and rejects contradictory mutation success', () => {
  const problem = { code: 'RATE_LIMITED', retryAfterMs: 1200 };
  expect(safeProblem({ code: 'FAILED', retryAfterMs: null })).toEqual({
    code: 'FAILED',
    retryAfterMs: 0,
  });
  expect(
    mutation({ operation: { ...result, state: 'FAILED' }, problem }, id)
      .problem,
  ).toEqual(problem);
  expect(() => mutation({ operation: result, problem }, id)).toThrow();
});
