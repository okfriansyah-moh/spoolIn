import type { Policy } from '../../policy/types';
import { validateArtworkFile } from './file-validation';
import { decodeArtworkFile } from './decode-file';

const MAX_SIDE = 8192;
const MAX_PIXELS = 40_000_000;

function encode(canvas: HTMLCanvasElement, mime: string): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob || blob.type !== mime)
          reject(
            new Error(
              'This browser could not prepare the image format. Try a JPEG or PNG image.',
            ),
          );
        else resolve(blob);
      },
      mime,
      0.9,
    );
  });
}

type PreparedArtwork = { file: File; message: string };

async function resizeArtwork(
  image: ImageBitmap,
  file: File,
  policy: NonNullable<Policy['artwork']>,
  initialScale: number,
): Promise<PreparedArtwork> {
  const canvas = document.createElement('canvas');
  try {
    const context = canvas.getContext('2d');
    if (!context) throw new Error('This browser could not resize the image.');
    let scale = initialScale;
    for (let attempt = 0; attempt < 12; attempt += 1) {
      canvas.width = Math.max(1, Math.floor(image.width * scale));
      canvas.height = Math.max(1, Math.floor(image.height * scale));
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      const blob = await encode(canvas, file.type.toLowerCase());
      const prepared = new File([blob], file.name, {
        type: blob.type,
        lastModified: file.lastModified,
      });
      if (!validateArtworkFile(prepared, policy))
        return {
          file: prepared,
          message: `Artwork resized automatically to ${canvas.width} × ${canvas.height} pixels for upload. Your original file is unchanged.`,
        };
      if (canvas.width === 1 && canvas.height === 1) break;
      scale *= 0.75;
    }
    throw new Error(
      'This image could not be reduced to the upload limit. Choose a smaller image.',
    );
  } finally {
    canvas.width = 0;
    canvas.height = 0;
  }
}

export async function prepareArtworkFile(
  file: File,
  policy: Policy['artwork'] | undefined,
): Promise<PreparedArtwork> {
  const invalid = validateArtworkFile(file, policy, true);
  if (invalid || !policy)
    throw new Error(invalid || 'Artwork rules unavailable.');
  const image = await decodeArtworkFile(file);
  try {
    const width = image.width;
    const height = image.height;
    if (!width || !height)
      throw new Error('This image has no readable dimensions.');
    const scale = Math.min(
      1,
      MAX_SIDE / width,
      MAX_SIDE / height,
      Math.sqrt(MAX_PIXELS / (width * height)),
    );
    if (scale === 1 && BigInt(file.size) <= BigInt(policy.maxInputBytes))
      return { file, message: '' };
    return await resizeArtwork(image, file, policy, scale);
  } finally {
    image.close();
  }
}
