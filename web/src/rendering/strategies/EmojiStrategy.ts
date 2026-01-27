import type { MaskStrategy, RenderContext } from './MaskStrategy';

export class EmojiStrategy implements MaskStrategy {
  render({ ctx, mask, detection, imageScale }: RenderContext): void {
    if (!detection) return;
    if (!mask.config.emoji) return;

    const { box } = detection;
    const { scale, rotation, emoji, fontFamily } = mask.config;

    const cx = (box.x + box.width / 2) * imageScale;
    const cy = (box.y + box.height / 2) * imageScale;

    const diameter = Math.max(box.width, box.height) * imageScale * scale;

    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate((rotation * Math.PI) / 180);

    const defaultFonts = '"Noto Color Emoji", "Apple Color Emoji", "Segoe UI Emoji", sans-serif';
    const family = fontFamily ? `"${fontFamily}", ${defaultFonts}` : defaultFonts;

    ctx.font = `${diameter}px ${family}`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';

    ctx.fillText(emoji, 0, diameter * 0.08); // Slight offset adjustment
    ctx.restore();
  }
}
