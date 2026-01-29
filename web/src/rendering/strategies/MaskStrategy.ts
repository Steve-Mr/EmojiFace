import type { Mask, Detection } from '../../domain/types';

export interface RenderContext {
  ctx: CanvasRenderingContext2D;
  mask: Mask;
  detection?: Detection;
  originalImage: HTMLImageElement | ImageBitmap;
  imageScale: number; // canvas width / image width
}

export interface MaskStrategy {
  render(context: RenderContext): void;
}
