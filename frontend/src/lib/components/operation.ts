export type OperationState =
  | { kind: 'pending'; stage: string }
  | { kind: 'confirmed'; recordId: string }
  | { kind: 'failed'; correction: string }
  | { kind: 'uncertain'; operationId: string }
  | { kind: 'denied'; reason: string };
export function operationMessage(state: OperationState) {
  switch (state.kind) {
    case 'pending':
      return {
        title: state.stage,
        detail: 'Wait for the current operation.',
        tone: 'info' as const,
      };
    case 'confirmed':
      return {
        title: 'Changes saved',
        detail: `Record: ${state.recordId}`,
        tone: 'success' as const,
      };
    case 'failed':
      return {
        title: 'Changes were not saved',
        detail: state.correction,
        tone: 'error' as const,
      };
    case 'uncertain':
      return {
        title: 'Save result not confirmed',
        detail: `Check the original operation before submitting again. Operation: ${state.operationId}`,
        tone: 'warning' as const,
      };
    case 'denied':
      return {
        title: 'Action not permitted',
        detail: state.reason,
        tone: 'error' as const,
      };
  }
}
