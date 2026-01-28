import React from 'react';
import { useEditorStore } from '../store/editorStore';
import { ImagePlus, Download, Plus, Settings } from 'lucide-react';
import { exportImage } from '../utils/exporter';

interface ToolbarProps {
    onToggleSettings: () => void;
    onExportComplete: () => void;
}

export const Toolbar: React.FC<ToolbarProps> = ({ onToggleSettings, onExportComplete }) => {
    const store = useEditorStore();

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
        <div className="bg-white border-t border-gray-200 p-3 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1)] z-20 flex justify-between items-center md:justify-center md:gap-8">

            <label className="flex flex-col items-center gap-1 cursor-pointer text-gray-600 hover:text-blue-600 transition-colors">
                <div className="p-2 bg-gray-100 rounded-full">
                    <ImagePlus className="w-6 h-6" />
                </div>
                <span className="text-xs font-medium">Open</span>
                <input type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
            </label>

            <button
                onClick={() => store.setIsManualAddMode(!store.isManualAddMode)}
                className={`flex flex-col items-center gap-1 transition-colors ${store.isManualAddMode ? 'text-blue-600' : 'text-gray-600 hover:text-blue-600'}`}
            >
                <div className={`p-2 rounded-full ${store.isManualAddMode ? 'bg-blue-100' : 'bg-gray-100'}`}>
                    <Plus className="w-6 h-6" />
                </div>
                <span className="text-xs font-medium">{store.isManualAddMode ? 'Adding...' : 'Add'}</span>
            </button>

            {/* Mobile Settings Toggle */}
            <button
                onClick={onToggleSettings}
                className="flex flex-col items-center gap-1 text-gray-600 hover:text-blue-600 transition-colors md:hidden"
            >
                <div className="p-2 bg-gray-100 rounded-full">
                    <Settings className="w-6 h-6" />
                </div>
                <span className="text-xs font-medium">Settings</span>
            </button>

            <button
                onClick={handleExport}
                disabled={!store.image}
                className={`flex flex-col items-center gap-1 transition-colors ${!store.image ? 'opacity-50 cursor-not-allowed text-gray-400' : 'text-blue-600 hover:text-blue-700'}`}
            >
                <div className="p-2 bg-blue-50 rounded-full">
                    <Download className="w-6 h-6" />
                </div>
                <span className="text-xs font-medium">Save</span>
            </button>

        </div>
    );
};
