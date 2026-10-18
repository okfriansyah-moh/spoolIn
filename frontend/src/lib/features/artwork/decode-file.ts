/** Decode the selected file directly, without an image URL subject to page CSP. */
export async function decodeArtworkFile(file: File): Promise<ImageBitmap> {
  if (typeof createImageBitmap !== 'function')
    throw new Error(
      'This browser cannot prepare local artwork. Use a current browser and select the image again.',
    );
  try {
    return await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch {
    throw new Error(
      'This image could not be decoded. Choose a valid JPEG, PNG, or WebP image.',
    );
  }
}
