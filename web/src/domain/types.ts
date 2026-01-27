export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface Point {
  x: number;
  y: number;
}

export interface Detection {
  id: string;
  box: Rect;
  score: number;
  keypoints: Point[];
}

export type MaskType = 'emoji' | 'blur';

export type BlurType = 'gaussian' | 'pixelate';

export interface MaskConfig {
  emoji?: string;
  blurType?: BlurType;
  scale: number; // Scale factor relative to the detection box
  rotation: number; // Degrees
  fontFamily?: string;
}

export interface Mask {
  id: string;
  type: MaskType;
  detectionId: string; // Linked detection
  config: MaskConfig;
}

export interface AppSettings {
  defaultEmoji: string;
  defaultBlurType: BlurType;
}

export interface FontMetadata {
  name: string;
  family: string;
}
