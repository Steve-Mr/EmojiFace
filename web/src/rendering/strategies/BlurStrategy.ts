import type { MaskStrategy, RenderContext } from './MaskStrategy';

export class BlurStrategy implements MaskStrategy {
  render({ ctx, mask, detection, originalImage, imageScale }: RenderContext): void {
    if (!detection) return;

    const { box } = detection;
    const { scale, blurType } = mask.config;

    const dstW = box.width * imageScale * scale;
    const dstH = box.height * imageScale * scale;
    const cx = (box.x + box.width / 2) * imageScale;
    const cy = (box.y + box.height / 2) * imageScale;
    const dstX = cx - dstW / 2;
    const dstY = cy - dstH / 2;

    const srcW = dstW / imageScale;
    const srcH = dstH / imageScale;
    const srcX = dstX / imageScale;
    const srcY = dstY / imageScale;

    ctx.save();

    ctx.beginPath();
    ctx.ellipse(cx, cy, dstW / 2, dstH / 2, 0, 0, 2 * Math.PI);
    ctx.clip();

    if (blurType === 'pixelate') {
      const pixelSize = 15;
      const tempCanvas = document.createElement('canvas');
      const smallW = Math.max(1, Math.floor(dstW / pixelSize));
      const smallH = Math.max(1, Math.floor(dstH / pixelSize));

      tempCanvas.width = smallW;
      tempCanvas.height = smallH;
      const tempCtx = tempCanvas.getContext('2d');
      if (tempCtx) {
        tempCtx.drawImage(originalImage, srcX, srcY, srcW, srcH, 0, 0, smallW, smallH);

        ctx.imageSmoothingEnabled = false;
        ctx.drawImage(tempCanvas, dstX, dstY, dstW, dstH);
      }
    } else {
        ctx.filter = 'blur(12px)';
        ctx.drawImage(originalImage, srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH);
        ctx.filter = 'none';
    }

    ctx.restore();
  }
}
