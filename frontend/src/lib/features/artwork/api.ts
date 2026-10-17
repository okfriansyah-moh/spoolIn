import type { GraphqlRequest } from '../../transport/graphql';
import { TransportFailure } from '../../transport/graphql';
import { record } from '../session/parse';

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
    operation { id state }
    uploadOperation { id state }
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
  return Object.freeze({
    state: String(op.state),
    operationId: String(op.id ?? operationId),
    problemCode,
  });
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
    const body = JSON.stringify({
      query: UPLOAD_ARTWORK,
      variables: {
        operationId: input.operationId,
        titleId: input.titleId,
        expectedVersion: Number(input.expectedVersion),
        policyRevision: input.policyRevision,
        content: input.contentBase64,
      },
    });
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
    xhr.onload = () => {
      try {
        const text = xhr.responseText;
        const parsed = JSON.parse(text) as { data?: unknown; errors?: unknown };
        if (parsed.errors) {
          reject(new TransportFailure('rejected'));
          return;
        }
        if (xhr.status < 200 || xhr.status >= 300 || !parsed.data) {
          reject(new TransportFailure('uncertain'));
          return;
        }
        resolve(parseUploadResponse(parsed.data, input.operationId));
      } catch {
        reject(new TransportFailure('uncertain'));
      }
    };
    xhr.send(body);
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
  return result.cancelled === true;
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
