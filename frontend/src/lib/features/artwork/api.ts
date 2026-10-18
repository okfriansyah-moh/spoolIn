import type { GraphqlRequest } from '../../transport/graphql';
import { TransportFailure } from '../../transport/graphql';
import { record } from '../session/parse';
import { operation } from '../session/outcome';

const UPLOAD_ARTWORK = `mutation UploadArtwork(
  $operationId: UUID!
  $titleId: UUID!
  $expectedVersion: Long!
  $policyRevision: String!
  $content: Base64!
) {
  uploadArtwork(
    operationId: $operationId
    titleId: $titleId
    expectedVersion: $expectedVersion
    policyRevision: $policyRevision
    content: $content
  ) {
    operation { id state resultExpired problem { code } }
    problem { code }
  }
}`;

const CANCEL_ARTWORK = `mutation CancelArtwork($operationId: UUID!, $uploadOperationId: UUID!) {
  cancelArtwork(operationId: $operationId, uploadOperationId: $uploadOperationId) {
    cancelled
    operation { id state resultExpired problem { code retryAfterMs } }
    uploadOperation { id state resultExpired problem { code retryAfterMs } }
  }
}`;

export type UploadArtworkInput = Readonly<{
  operationId: string;
  titleId: string;
  expectedVersion: string;
  policyRevision: string;
  contentBase64: string;
}>;

export type UploadArtworkMutationResult = Readonly<{
  state: string;
  operationId: string;
  problemCode: string | null;
}>;

function parseUploadResponse(
  data: unknown,
  operationId: string,
): UploadArtworkMutationResult {
  const root = record(data);
  const result = record(root.uploadArtwork);
  const problem = result.problem;
  const problemCode =
    problem && typeof record(problem).code === 'string'
      ? String(record(problem).code)
      : null;
  const op = record(result.operation);
  if (
    op.id !== operationId ||
    !['PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED'].includes(String(op.state)) ||
    typeof op.resultExpired !== 'boolean'
  ) {
    throw new TransportFailure('uncertain');
  }
  if (op.resultExpired) throw new TransportFailure('uncertain');
  return Object.freeze({
    state: String(op.state),
    operationId,
    problemCode,
  });
}

function artworkUploadBody(input: UploadArtworkInput): string {
  return JSON.stringify({
    query: UPLOAD_ARTWORK,
    variables: {
      operationId: input.operationId,
      titleId: input.titleId,
      expectedVersion: input.expectedVersion,
      policyRevision: input.policyRevision,
      content: input.contentBase64,
    },
  });
}

function handleArtworkUploadLoad(
  xhr: XMLHttpRequest,
  operationId: string,
  resolve: (value: UploadArtworkMutationResult) => void,
  reject: (reason?: unknown) => void,
): void {
  try {
    const parsed = JSON.parse(xhr.responseText) as {
      data?: unknown;
      errors?: unknown;
    };
    if (parsed.errors) {
      reject(new TransportFailure('rejected'));
      return;
    }
    if (xhr.status < 200 || xhr.status >= 300 || !parsed.data) {
      reject(new TransportFailure('uncertain'));
      return;
    }
    resolve(parseUploadResponse(parsed.data, operationId));
  } catch {
    reject(new TransportFailure('uncertain'));
  }
}

export function uploadArtworkWithProgress(
  csrf: string,
  input: UploadArtworkInput,
  onProgress: (loaded: number, total: number) => void,
  onXhr?: (xhr: XMLHttpRequest) => void,
): Promise<UploadArtworkMutationResult> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    onXhr?.(xhr);
    xhr.open('POST', '/graphql');
    xhr.withCredentials = true;
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.setRequestHeader(
      'Accept',
      'application/graphql-response+json, application/json',
    );
    xhr.setRequestHeader('X-Spoolin-Client', 'web');
    xhr.setRequestHeader('X-CSRF-Token', csrf);
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(event.loaded, event.total);
    };
    xhr.onerror = () => reject(new TransportFailure('uncertain'));
    xhr.onabort = () => reject(new DOMException('Aborted', 'AbortError'));
    xhr.onload = () =>
      handleArtworkUploadLoad(xhr, input.operationId, resolve, reject);
    xhr.send(artworkUploadBody(input));
  });
}

export async function uploadArtwork(
  request: GraphqlRequest,
  csrf: string,
  input: UploadArtworkInput,
): Promise<{ state: string; operationId: string }> {
  const result = await uploadArtworkWithProgress(csrf, input, () => {});
  if (result.problemCode)
    throw new TransportFailure('rejected', result.problemCode);
  return { state: result.state, operationId: result.operationId };
}

export async function cancelArtwork(
  request: GraphqlRequest,
  csrf: string,
  operationId: string,
  uploadOperationId: string,
): Promise<boolean> {
  const data = record(
    await request(
      CANCEL_ARTWORK,
      { operationId, uploadOperationId },
      { mutation: true, csrf },
    ),
  );
  const result = record(data.cancelArtwork);
  return confirmedCancellation(result, operationId, uploadOperationId);
}

function confirmedCancellation(
  result: Record<string, unknown>,
  operationId: string,
  uploadOperationId: string,
): boolean {
  const receipt = operation(result.operation, operationId);
  const upload =
    result.uploadOperation === null
      ? null
      : operation(result.uploadOperation, uploadOperationId);
  if (
    receipt.resultExpired ||
    receipt.state === 'EXPIRED' ||
    receipt.state === 'PENDING'
  )
    throw new TransportFailure('uncertain');
  if (receipt.state === 'FAILED') return false;
  if (typeof result.cancelled !== 'boolean')
    throw new TransportFailure('uncertain');
  if (!result.cancelled) return false;
  return cancelledUpload(upload);
}

function cancelledUpload(upload: ReturnType<typeof operation> | null): boolean {
  if (
    !upload ||
    upload.resultExpired ||
    upload.state !== 'FAILED' ||
    upload.problem?.code !== 'CANCELLED'
  )
    throw new TransportFailure('uncertain');
  return true;
}

export function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const value = reader.result;
      if (typeof value !== 'string') {
        reject(new Error('Artwork read failed.'));
        return;
      }
      const comma = value.indexOf(',');
      resolve(comma >= 0 ? value.slice(comma + 1) : value);
    };
    reader.onerror = () =>
      reject(reader.error ?? new Error('Artwork read failed.'));
    reader.readAsDataURL(file);
  });
}
