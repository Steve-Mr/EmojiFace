import React from 'react';
import { useEditorStore } from '../store/editorStore';
import { Trash2, RotateCw, ZoomIn } from 'lucide-react';

export const MaskControls: React.FC = () => {
    const store = useEditorStore();
    const mask = store.masks.find(m => m.id === store.selectedMaskId);

    if (!mask) return null;

    return (
        <div className="absolute bottom-20 left-1/2 transform -translate-x-1/2 bg-white rounded-xl shadow-lg border border-gray-200 p-4 flex flex-col gap-4 w-[calc(100%-2rem)] max-w-md z-30 animate-slide-up">
            <div className="flex justify-between items-center border-b pb-2">
                <span className="text-sm font-semibold text-gray-700">Edit Mask</span>
                <button
                    onClick={() => store.deleteMask(mask.id)}
                    className="text-red-500 hover:text-red-700 bg-red-50 p-2 rounded-full hover:bg-red-100 transition-colors"
                    title="Delete Mask"
                >
                    <Trash2 className="w-4 h-4" />
                </button>
            </div>

            <div className="space-y-4">
                <div className="flex items-center gap-3">
                    <ZoomIn className="w-4 h-4 text-gray-500" />
                    <input
                        type="range"
                        min="0.5"
                        max="3"
                        step="0.1"
                        value={mask.config.scale}
                        onChange={(e) => store.updateMask(mask.id, { scale: parseFloat(e.target.value) })}
                        className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
                    />
                    <span className="text-xs text-gray-500 w-8 text-right">{mask.config.scale.toFixed(1)}x</span>
                </div>

                <div className="flex items-center gap-3">
                    <RotateCw className="w-4 h-4 text-gray-500" />
                    <input
                        type="range"
                        min="-180"
                        max="180"
                        value={mask.config.rotation}
                        onChange={(e) => store.updateMask(mask.id, { rotation: parseInt(e.target.value) })}
                        className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
                    />
                    <span className="text-xs text-gray-500 w-8 text-right">{Math.round(mask.config.rotation)}°</span>
                </div>

                {mask.type === 'emoji' && (
                    <div className="flex items-center gap-3 pt-1">
                        <span className="text-xs font-medium text-gray-500 w-8">Emoji</span>
                        <input
                            type="text"
                            value={mask.config.emoji || ''}
                            onChange={(e) => store.updateMask(mask.id, { emoji: e.target.value })}
                            className="flex-1 min-w-0 border border-gray-300 rounded px-2 py-1 text-center focus:ring-2 focus:ring-blue-500 outline-none"
                        />
                    </div>
                )}
            </div>
        </div>
    );
};
