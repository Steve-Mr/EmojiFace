import type { EditorState } from '../store/editorStore'; // Need to export EditorState
import { canvasRenderer } from '../rendering/CanvasRenderer';
import type { PrivacyExportSettings } from '../domain/types';

const MIME_TYPES: Record<PrivacyExportSettings['format'], string> = {
    png: 'image/png',
    jpeg: 'image/jpeg',
    webp: 'image/webp',
};

export async function exportImage(state: EditorState, settings: PrivacyExportSettings): Promise<void> { // state: EditorState (avoid circular dependency if possible)
    const { image, masks, detections } = state;
    if (!image) return;

    const canvas = document.createElement('canvas');
    // Ensure we use the original image dimensions
    const w = 'width' in image ? image.width : (image as HTMLImageElement).naturalWidth;
    const h = 'height' in image ? image.height : (image as HTMLImageElement).naturalHeight;

    canvas.width = w;
    canvas.height = h;

    canvasRenderer.render(
        canvas,
        image,
        masks,
        detections,
        settings.preserveTransparency ? undefined : { backgroundColor: '#ffffff' }
    );

    const mimeType = MIME_TYPES[settings.format];
    const extension = settings.format === 'jpeg' ? 'jpg' : settings.format;
    const quality = settings.format === 'png' ? undefined : settings.quality;

    canvas.toBlob(async (blob) => {
        if (!blob) return;

        if (navigator.share && navigator.canShare && navigator.canShare({ files: [new File([blob], `facemoji-privacy.${extension}`, {type: mimeType})] })) {
            try {
                const file = new File([blob], `facemoji-privacy.${extension}`, { type: mimeType });
                await navigator.share({
                    files: [file],
                    title: 'FaceMoji Privacy Export',
                    text: 'Privacy-enhanced image processed on device.'
                });
                return;
            } catch (e) {
                console.log('Share failed, falling back to download', e);
            }
        }

        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `facemoji-privacy-${Date.now()}.${extension}`;
        a.click();
        URL.revokeObjectURL(url);
    }, mimeType, quality);
}
