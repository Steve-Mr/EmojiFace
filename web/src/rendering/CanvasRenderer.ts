import type { Mask, Detection } from '../domain/types';
import type { MaskStrategy } from './strategies/MaskStrategy';
import { EmojiStrategy } from './strategies/EmojiStrategy';
import { BlurStrategy } from './strategies/BlurStrategy';

export class CanvasRenderer {
  private strategies: Record<string, MaskStrategy> = {};

  constructor() {
    this.strategies['emoji'] = new EmojiStrategy();
    this.strategies['blur'] = new BlurStrategy();
  }

  render(
    canvas: HTMLCanvasElement,
    image: HTMLImageElement | ImageBitmap,
    masks: Mask[],
    detections: Detection[]
  ) {
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw original image to fill the canvas
    ctx.drawImage(image, 0, 0, canvas.width, canvas.height);

    // Calculate scale assuming aspect ratio is preserved
    // The UI should ensure canvas.width/height matches image aspect ratio
    const scaleX = canvas.width / image.width;

    const detectionMap = new Map(detections.map(d => [d.id, d]));

    masks.forEach(mask => {
      const strategy = this.strategies[mask.type];
      if (strategy && mask.detectionId) {
        const detection = detectionMap.get(mask.detectionId);
        if (detection) {
             strategy.render({
                 ctx,
                 mask,
                 detection,
                 originalImage: image,
                 imageScale: scaleX
             });
        }
      }
    });
  }
}

export const canvasRenderer = new CanvasRenderer();
