import type { EditorState } from '../store/editorStore'; // Need to export EditorState
import { canvasRenderer } from '../rendering/CanvasRenderer';

export async function exportImage(state: EditorState): Promise<void> { // state: EditorState (avoid circular dependency if possible)
    const { image, masks, detections } = state;
    if (!image) return;

    const canvas = document.createElement('canvas');
    // Ensure we use the original image dimensions
    const w = 'width' in image ? image.width : (image as HTMLImageElement).naturalWidth;
    const h = 'height' in image ? image.height : (image as HTMLImageElement).naturalHeight;

    canvas.width = w;
    canvas.height = h;

    canvasRenderer.render(canvas, image, masks, detections);

    canvas.toBlob(async (blob) => {
        if (!blob) return;

        if (navigator.share && navigator.canShare && navigator.canShare({ files: [new File([blob], 'test.png', {type:'image/png'})] })) {
            try {
                const file = new File([blob], 'facemoji.png', { type: 'image/png' });
                await navigator.share({
                    files: [file],
                    title: 'FaceMoji Export',
                    text: 'Check out this image!'
                });
                return;
            } catch (e) {
                console.log('Share failed, falling back to download', e);
            }
        }

        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `facemoji-${Date.now()}.png`;
        a.click();
        URL.revokeObjectURL(url);
    }, 'image/png');
}
