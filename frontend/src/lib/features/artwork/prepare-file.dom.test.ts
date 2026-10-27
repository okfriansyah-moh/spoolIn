import { afterEach, expect, it, vi } from 'vitest';
import { prepareArtworkFile } from './prepare-file';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';

const policy = {
  ...specimenPolicy().artwork,
  formats: ['PNG'],
  maxInputBytes: '12',
};
const imageFile = () =>
  new File(['original'], 'poster.png', { type: 'image/png', lastModified: 42 });
function decoded(width = 100, height = 100) {
  const image = { width, height, close: vi.fn() };
  vi.stubGlobal(
    'createImageBitmap',
    vi.fn(async () => image),
  );
  return image;
}
function encoder(bytes = 'small', type = 'image/png') {
  const drawImage = vi.fn();
  // Only the browser drawing boundary is simulated; preparation and validation stay real.
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({
    drawImage,
  } as unknown as CanvasRenderingContext2D);
  vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(
    (callback) => callback(new Blob([bytes], { type })),
  );
  return drawImage;
}
afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

it('keeps a compliant original image unchanged and closes its decoded resource', async () => {
  const image = decoded();
  const file = imageFile();
  expect(await prepareArtworkFile(file, policy)).toEqual({ file, message: '' });
  expect(image.close).toHaveBeenCalledOnce();
});

it('uses orientation-aware browser decoding and reports unreadable image failures', async () => {
  const bitmap = decoded();
  const file = imageFile();
  await prepareArtworkFile(file, policy);
  expect(createImageBitmap).toHaveBeenCalledWith(file, {
    imageOrientation: 'from-image',
  });
  expect(bitmap.close).toHaveBeenCalledOnce();
  vi.stubGlobal(
    'createImageBitmap',
    vi.fn(async () => {
      throw new Error('codec details');
    }),
  );
  await expect(prepareArtworkFile(file, policy)).rejects.toThrow(
    'This image could not be decoded.',
  );
  vi.stubGlobal('createImageBitmap', undefined);
  await expect(prepareArtworkFile(file, policy)).rejects.toThrow(
    'This browser cannot prepare local artwork.',
  );
});

it('rejects unknown rules and unsupported files before invoking a browser decoder', async () => {
  const image = decoded();
  await expect(prepareArtworkFile(imageFile(), undefined)).rejects.toThrow(
    'Current artwork rules are unavailable.',
  );
  await expect(
    prepareArtworkFile(
      new File(['x'], 'file.gif', { type: 'image/gif' }),
      policy,
    ),
  ).rejects.toThrow('format');
  expect(image.close).not.toHaveBeenCalled();
  expect(createImageBitmap).not.toHaveBeenCalled();
});

it.each([
  [0, 10],
  [10, 0],
])(
  'rejects an unreadable %s by %s image and still closes it',
  async (width, height) => {
    const image = decoded(width, height);
    await expect(prepareArtworkFile(imageFile(), policy)).rejects.toThrow(
      'no readable dimensions',
    );
    expect(image.close).toHaveBeenCalledOnce();
  },
);

it('resizes dimensions to the bounded side and preserves the original file metadata', async () => {
  const image = decoded(16384, 4096);
  const draw = encoder();
  const file = imageFile();
  const result = await prepareArtworkFile(file, policy);
  expect(result.file.name).toBe('poster.png');
  expect(result.file.lastModified).toBe(42);
  expect(result.file.size).toBe(5);
  expect(result.message).toContain('8192 × 2048');
  expect(draw).toHaveBeenCalledWith(image, 0, 0, 8192, 2048);
  expect(file.size).toBe(8);
  expect(image.close).toHaveBeenCalledOnce();
});

it('retries compression until its encoded bytes satisfy the effective upload limit', async () => {
  const image = decoded();
  encoder();
  vi.spyOn(HTMLCanvasElement.prototype, 'toBlob')
    .mockImplementationOnce((callback) =>
      callback(new Blob(['too large to upload'], { type: 'image/png' })),
    )
    .mockImplementationOnce((callback) =>
      callback(new Blob(['ok'], { type: 'image/png' })),
    );
  const result = await prepareArtworkFile(
    new File(['larger than current bytes'], 'poster.png', {
      type: 'image/png',
    }),
    policy,
  );
  expect(result.file.size).toBe(2);
  expect(result.message).toContain('75 × 75');
  expect(image.close).toHaveBeenCalledOnce();
});

it('rejects incompatible browser encoding without changing the selected original', async () => {
  const image = decoded(9000, 100);
  encoder('small', 'image/jpeg');
  const file = imageFile();
  await expect(prepareArtworkFile(file, policy)).rejects.toThrow(
    'could not prepare the image format',
  );
  expect(file.type).toBe('image/png');
  expect(image.close).toHaveBeenCalledOnce();
});

it('rejects unavailable canvas support and encoding failure and releases resources', async () => {
  const image = decoded(9000, 100);
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
  await expect(prepareArtworkFile(imageFile(), policy)).rejects.toThrow(
    'could not resize',
  );
  encoder();
  vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(
    (callback) => callback(null),
  );
  await expect(prepareArtworkFile(imageFile(), policy)).rejects.toThrow(
    'could not prepare the image format',
  );
  expect(image.close).toHaveBeenCalledTimes(2);
});

it('bounds repeated unsuccessful compression instead of retaining an oversized upload', async () => {
  const image = decoded(100, 100);
  const draw = encoder('always larger than the limit');
  await expect(
    prepareArtworkFile(
      new File(['file exceeds policy'], 'poster.png', { type: 'image/png' }),
      policy,
    ),
  ).rejects.toThrow('could not be reduced');
  expect(draw).toHaveBeenCalledTimes(12);
  expect(image.close).toHaveBeenCalledOnce();
});

it('stops at one pixel when the browser cannot produce acceptable encoded bytes', async () => {
  decoded(1, 1);
  const draw = encoder('always larger than the limit');
  await expect(
    prepareArtworkFile(
      new File(['file exceeds policy'], 'poster.png', { type: 'image/png' }),
      policy,
    ),
  ).rejects.toThrow('could not be reduced');
  expect(draw).toHaveBeenCalledOnce();
});
