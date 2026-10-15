import { it, expect } from 'vitest';
import { operationMessage, type OperationState } from './operation';
const rows: [OperationState, string, string][] = [
  [
    { kind: 'pending', stage: 'Checking artwork' },
    'Checking artwork',
    'Wait for the current operation.',
  ],
  [
    { kind: 'confirmed', recordId: 'record-1' },
    'Changes saved',
    'Record: record-1',
  ],
  [
    { kind: 'failed', correction: 'Correct the title' },
    'Changes were not saved',
    'Correct the title',
  ],
  [
    { kind: 'uncertain', operationId: 'original-op' },
    'Save result not confirmed',
    'Check the original operation before submitting again. Operation: original-op',
  ],
  [
    { kind: 'denied', reason: 'Access changed' },
    'Action not permitted',
    'Access changed',
  ],
];
for (const [state, title, detail] of rows)
  it(`distinguishes ${state.kind}`, () => {
    expect(operationMessage(state)).toMatchObject({ title, detail });
  });
