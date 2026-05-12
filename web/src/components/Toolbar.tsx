import React from 'react';
import { useEditorStore } from '../store/editorStore';
import { ImagePlus, Download, Plus, Settings, Trash2 } from 'lucide-react';
import { exportImage } from '../utils/exporter';
import { useTranslation } from '../i18n/TranslationContext';

interface ToolbarProps {
    onToggleSettings: () => void;
    onExportComplete: () => void;
    onRemoveImage: () => void;
}

export const Toolbar: React.FC<ToolbarProps> = ({ onToggleSettings, onExportComplete, onRemoveImage }) => {
    const store = useEditorStore();
    const { t } = useTranslation();

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            store.setImage(e.target.files[0]);
        }
    };

    const handleExport = async () => {
        await exportImage(store);
        onExportComplete();
    };

    return (
        <div className="bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-800 p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1)] z-20 flex justify-between items-center md:justify-center md:gap-8 transition-colors duration-300">

            {store.image ? (
                <button
                    onClick={onRemoveImage}
                    className="flex flex-col items-center gap-1 text-gray-600 hover:text-red-600 dark:text-gray-400 dark:hover:text-red-400 transition-colors"
                >
                    <div className="p-2 bg-gray-100 dark:bg-gray-800 rounded-full transition-colors">
                        <Trash2 className="w-6 h-6" />
                    </div>
                    <span className="text-xs font-medium">{t.remove}</span>
                </button>
            ) : (
                <label className="flex flex-col items-center gap-1 cursor-pointer text-gray-600 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400 transition-colors">
                    <div className="p-2 bg-gray-100 dark:bg-gray-800 rounded-full transition-colors">
                        <ImagePlus className="w-6 h-6" />
                    </div>
                    <span className="text-xs font-medium">{t.open}</span>
                    <input type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
                </label>
            )}

            <button
                onClick={() => store.setIsManualAddMode(!store.isManualAddMode)}
                disabled={!store.image}
                className={`flex flex-col items-center gap-1 transition-colors ${
                    !store.image
                        ? 'opacity-50 cursor-not-allowed text-gray-400 dark:text-gray-600'
                        : (store.isManualAddMode
                            ? 'text-blue-600 dark:text-blue-400'
                            : 'text-gray-600 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400')
                }`}
            >
                <div className={`p-2 rounded-full transition-colors ${store.isManualAddMode ? 'bg-blue-100 dark:bg-blue-900/50' : 'bg-gray-100 dark:bg-gray-800'}`}>
                    <Plus className="w-6 h-6" />
                </div>
                <span className="text-xs font-medium">{store.isManualAddMode ? t.adding : t.add}</span>
            </button>

            {/* Mobile Settings Toggle */}
            <button
                onClick={onToggleSettings}
                className="flex flex-col items-center gap-1 text-gray-600 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400 transition-colors md:hidden"
            >
                <div className="p-2 bg-gray-100 dark:bg-gray-800 rounded-full transition-colors">
                    <Settings className="w-6 h-6" />
                </div>
                <span className="text-xs font-medium">{t.settings}</span>
            </button>

            <button
                onClick={handleExport}
                disabled={!store.image}
                className={`flex flex-col items-center gap-1 transition-colors ${
                    !store.image
                        ? 'opacity-50 cursor-not-allowed text-gray-400 dark:text-gray-600'
                        : 'text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300'
                }`}
            >
                <div className="p-2 bg-blue-50 dark:bg-blue-900/50 rounded-full transition-colors">
                    <Download className="w-6 h-6" />
                </div>
                <span className="text-xs font-medium">{t.save}</span>
            </button>

        </div>
    );
};
