import type { Detection } from '../domain/types';

export interface FaceDetector {
  load(): Promise<void>;
  detect(image: ImageBitmap | HTMLImageElement): Promise<Detection[]>;
  isLoaded(): boolean;
}
