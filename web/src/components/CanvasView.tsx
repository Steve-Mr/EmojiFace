import React, { useRef, useEffect, useState } from 'react';
import { useEditorStore } from '../store/editorStore';
import { canvasRenderer } from '../rendering/CanvasRenderer';
import { useTranslation } from '../i18n/TranslationContext';

export const CanvasView: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const { image, masks, detections, selectMask, isManualAddMode, addManualMask, fontsLoaded } = useEditorStore();
  const { t } = useTranslation();

  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });

  useEffect(() => {
    if (image && containerRef.current) {
        const containerW = containerRef.current.clientWidth;
        const containerH = containerRef.current.clientHeight;

        // Ensure at least 20px padding
        const maxW = containerW - 40;
        const maxH = containerH - 40;

        const imgRatio = image.width / image.height;
        const containerRatio = maxW / maxH;

        let w, h;
        if (imgRatio > containerRatio) {
            w = maxW;
            h = maxW / imgRatio;
        } else {
            h = maxH;
            w = maxH * imgRatio;
        }
        setDimensions({ width: w, height: h });
    }
  }, [image]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (canvas && image) {
      canvas.width = image.width;
      canvas.height = image.height;

      canvasRenderer.render(canvas, image, masks, detections);
    }
  }, [image, masks, detections, dimensions, fontsLoaded]);

  const handlePointerDown = (e: React.PointerEvent) => {
    if (!canvasRef.current || !image) return;

    const rect = canvasRef.current.getBoundingClientRect();
    const scaleX = image.width / rect.width;
    const scaleY = image.height / rect.height;

    const x = (e.clientX - rect.left) * scaleX;
    const y = (e.clientY - rect.top) * scaleY;

    if (isManualAddMode) {
        addManualMask(x, y);
        return;
    }

    const clickedMask = [...masks].reverse().find(mask => {
       const detection = detections.find(d => d.id === mask.detectionId);
       if (!detection) return false;

       const box = detection.box;
       return x >= box.x && x <= box.x + box.width &&
              y >= box.y && y <= box.y + box.height;
    });

    selectMask(clickedMask ? clickedMask.id : null);
  };

  if (!image) return (
      <div className="flex-1 flex items-center justify-center bg-gray-50 dark:bg-gray-900 text-gray-400 dark:text-gray-600 transition-colors">
        <div className="text-center">
            <p className="text-lg font-medium mb-2">{t.noImageLoaded}</p>
            <p className="text-sm">{t.clickToStart}</p>
        </div>
      </div>
  );

  return (
    <div ref={containerRef} className="flex-1 flex items-center justify-center overflow-hidden bg-gray-100 dark:bg-gray-950 w-full h-full p-4 relative transition-colors">
      <canvas
        ref={canvasRef}
        style={{ width: dimensions.width, height: dimensions.height, touchAction: 'none' }}
        onPointerDown={handlePointerDown}
        className={`shadow-lg max-w-full max-h-full object-contain ${isManualAddMode ? 'cursor-crosshair' : 'cursor-default'}`}
      />
    </div>
  );
};
