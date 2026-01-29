import React from 'react';
import { useEditorStore } from '../store/editorStore';
import { Trash2, RotateCw, ZoomIn } from 'lucide-react';
import { useTranslation } from '../i18n/TranslationContext';

export const MaskControls: React.FC = () => {
    const store = useEditorStore();
    const { t } = useTranslation();
    const mask = store.masks.find(m => m.id === store.selectedMaskId);

    if (!mask) return null;

    return (
        <div className="
            fixed bottom-0 left-0 right-0 z-30
            bg-white dark:bg-gray-900 rounded-t-xl shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1)] border-t border-gray-200 dark:border-gray-800
            p-4 pb-[calc(1rem+env(safe-area-inset-bottom))]
            flex flex-col gap-4 animate-slide-up-sheet
            md:absolute md:bottom-20 md:left-1/2 md:transform md:-translate-x-1/2 md:w-[400px] md:mx-0
            md:rounded-xl md:shadow-lg md:border md:pb-4 md:animate-slide-up
        ">
            <div className="flex justify-between items-center border-b border-gray-100 dark:border-gray-800 pb-2">
                <span className="text-sm font-semibold text-gray-700 dark:text-gray-200">{t.editMask}</span>
                <button
                    onClick={() => store.deleteMask(mask.id)}
                    className="text-red-500 hover:text-red-700 bg-red-50 dark:bg-red-900/20 dark:hover:bg-red-900/40 p-2 rounded-full hover:bg-red-100 transition-colors"
                    title={t.deleteMask}
                >
                    <Trash2 className="w-4 h-4" />
                </button>
            </div>

            <div className="space-y-4">
                <div className="flex items-center gap-3">
                    <ZoomIn className="w-4 h-4 text-gray-500 dark:text-gray-400" />
                    <input
                        type="range"
                        min="0.5"
                        max="3"
                        step="0.1"
                        value={mask.config.scale}
                        onChange={(e) => store.updateMask(mask.id, { scale: parseFloat(e.target.value) })}
                        className="flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-blue-600"
                    />
                    <span className="text-xs text-gray-500 dark:text-gray-400 w-8 text-right">{mask.config.scale.toFixed(1)}x</span>
                </div>

                <div className="flex items-center gap-3">
                    <RotateCw className="w-4 h-4 text-gray-500 dark:text-gray-400" />
                    <input
                        type="range"
                        min="-180"
                        max="180"
                        value={mask.config.rotation}
                        onChange={(e) => store.updateMask(mask.id, { rotation: parseInt(e.target.value) })}
                        className="flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-blue-600"
                    />
                    <span className="text-xs text-gray-500 dark:text-gray-400 w-8 text-right">{Math.round(mask.config.rotation)}°</span>
                </div>

                {mask.type === 'emoji' && (
                    <div className="flex items-center gap-3 pt-1">
                        <span className="text-xs font-medium text-gray-500 dark:text-gray-400 w-8">{t.emoji}</span>
                        <input
                            type="text"
                            value={mask.config.emoji || ''}
                            onChange={(e) => store.updateMask(mask.id, { emoji: e.target.value })}
                            className="flex-1 min-w-0 border border-gray-300 dark:border-gray-700 rounded px-2 py-1 text-center focus:ring-2 focus:ring-blue-500 outline-none
                                bg-white dark:bg-gray-800 dark:text-white"
                        />
                    </div>
                )}
            </div>
        </div>
    );
};
