import type { EditorState } from '../store/editorStore';
import { canvasRenderer } from '../rendering/CanvasRenderer';
import piexif from 'piexifjs';

export async function exportImage(state: EditorState): Promise<void> {
    const { image, imageBlob, masks, detections } = state;
    if (!image) return;

    const canvas = document.createElement('canvas');
    // Ensure we use the original image dimensions
    const w = 'width' in image ? image.width : (image as HTMLImageElement).naturalWidth;
    const h = 'height' in image ? image.height : (image as HTMLImageElement).naturalHeight;

    canvas.width = w;
    canvas.height = h;

    canvasRenderer.render(canvas, image, masks, detections);

    let mimeType = 'image/png';
    let extension = 'png';
    if (imageBlob && imageBlob.type === 'image/jpeg') {
        mimeType = 'image/jpeg';
        extension = 'jpg';
    } else if (imageBlob && imageBlob.type === 'image/webp') {
        mimeType = 'image/webp';
        extension = 'webp';
    }

    const quality = mimeType === 'image/jpeg' || mimeType === 'image/webp' ? 0.95 : undefined;

    canvas.toBlob(async (blob) => {
        if (!blob) return;

        let finalBlob = blob;

        // Try to inject EXIF if it's a JPEG
        if (mimeType === 'image/jpeg' && imageBlob) {
            try {
                const originalDataUrl = await blobToDataURL(imageBlob);

                const exifObj = piexif.load(originalDataUrl);

                // Reset Orientation to avoid double rotation since canvas handles initial rotation
                if (exifObj && exifObj["0th"]) {
                    exifObj["0th"][piexif.ImageIFD.Orientation] = 1;
                }

                const exifStr = piexif.dump(exifObj);

                // If original has EXIF, inject it into the new image
                if (exifStr && exifStr !== 'Exif\x00\x00MM\x00*\x00\x00\x00\x08\x00\x00\x00\x00\x00\x00') {
                     const newDataUrl = await blobToDataURL(blob);

                     const finalDataUrl = piexif.insert(exifStr, newDataUrl);
                     finalBlob = dataURLToBlob(finalDataUrl);
                }
            } catch (e) {
                console.error('Failed to copy EXIF data:', e);
            }
        }

        const fileName = `facemoji-${Date.now()}.${extension}`;

        if (navigator.share && navigator.canShare && navigator.canShare({ files: [new File([finalBlob], fileName, {type: mimeType})] })) {
            try {
                const file = new File([finalBlob], fileName, { type: mimeType });
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

        const url = URL.createObjectURL(finalBlob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
    }, mimeType, quality);
}

function blobToDataURL(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = () => reject(reader.error);
        reader.readAsDataURL(blob);
    });
}

function dataURLToBlob(dataurl: string): Blob {
    const arr = dataurl.split(',');
    const mimeMatch = arr[0].match(/:(.*?);/);
    const mime = mimeMatch ? mimeMatch[1] : '';
    const bstr = window.atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
        u8arr[n] = bstr.charCodeAt(n);
    }
    return new Blob([u8arr], { type: mime });
}
