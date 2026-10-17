import type { GraphqlRequest } from '../../transport/graphql';
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

export async function uploadArtwork(
  request: GraphqlRequest,
  csrf: string,
  input: {
    operationId: string;
    titleId: string;
    expectedVersion: string;
    policyRevision: string;
    contentBase64: string;
  },
): Promise<{ state: string; operationId: string }> {
  const data = record(
    await request(
      UPLOAD_ARTWORK,
      {
        operationId: input.operationId,
        titleId: input.titleId,
        expectedVersion: Number(input.expectedVersion),
        policyRevision: input.policyRevision,
        content: input.contentBase64,
      },
      { csrf },
    ),
  );
  const result = record(data.uploadArtwork);
  const op = record(result.operation);
  return { state: String(op.state), operationId: String(op.id) };
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
    reader.onerror = () => reject(reader.error ?? new Error('Artwork read failed.'));
    reader.readAsDataURL(file);
  });
}
