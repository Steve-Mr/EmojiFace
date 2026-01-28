import type { MaskStrategy, RenderContext } from './MaskStrategy';

export class BlurStrategy implements MaskStrategy {
  render({ ctx, mask, detection, originalImage, imageScale }: RenderContext): void {
    if (!detection) return;

    const { box } = detection;
    const { scale, blurType, rotation } = mask.config;

    const dstW = box.width * imageScale * scale;
    const dstH = box.height * imageScale * scale;
    const cx = (box.x + box.width / 2) * imageScale;
    const cy = (box.y + box.height / 2) * imageScale;

    // Rotation in radians
    const rad = ((rotation || 0) * Math.PI) / 180;

    // Calculate a bounding box large enough to cover the rotated ellipse
    // We use the diagonal of the ellipse box as the side length of a square centered at cx,cy
    const maxDim = Math.hypot(dstW, dstH);

    // Coordinates for drawing the image
    const drawDstX = cx - maxDim / 2;
    const drawDstY = cy - maxDim / 2;
    const drawDstW = maxDim;
    const drawDstH = maxDim;

    // Corresponding source coordinates
    const drawSrcX = drawDstX / imageScale;
    const drawSrcY = drawDstY / imageScale;
    const drawSrcW = drawDstW / imageScale;
    const drawSrcH = drawDstH / imageScale;

    ctx.save();

    ctx.beginPath();
    ctx.ellipse(cx, cy, dstW / 2, dstH / 2, rad, 0, 2 * Math.PI);
    ctx.clip();

    if (blurType === 'pixelate') {
      // Dynamic pixel size: 5% of face width (min 4px)
      const pixelSize = Math.max(4, drawDstW * 0.05);

      const tempCanvas = document.createElement('canvas');
      const smallW = Math.max(1, Math.floor(drawDstW / pixelSize));
      const smallH = Math.max(1, Math.floor(drawDstH / pixelSize));

      tempCanvas.width = smallW;
      tempCanvas.height = smallH;
      const tempCtx = tempCanvas.getContext('2d');
      if (tempCtx) {
        // Draw the relevant part of the image to temp canvas
        tempCtx.drawImage(originalImage, drawSrcX, drawSrcY, drawSrcW, drawSrcH, 0, 0, smallW, smallH);

        // Draw back scaled up
        ctx.imageSmoothingEnabled = false;
        ctx.drawImage(tempCanvas, drawDstX, drawDstY, drawDstW, drawDstH);
      }
    } else {
        // Dynamic blur radius: 5% of face width (min 2px)
        const blurRadius = Math.max(2, drawDstW * 0.05);
        ctx.filter = `blur(${blurRadius}px)`;
        ctx.drawImage(originalImage, drawSrcX, drawSrcY, drawSrcW, drawSrcH, drawDstX, drawDstY, drawDstW, drawDstH);
        ctx.filter = 'none';
    }

    ctx.restore();
  }
}
