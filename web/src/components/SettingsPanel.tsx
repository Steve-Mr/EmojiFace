import React from 'react';
import { useEditorStore } from '../store/editorStore';
import { Cpu, Smile, Sparkles, Type, Upload } from 'lucide-react';

interface SettingsPanelProps {
    isOpen: boolean;
    onClose: () => void;
}

export const SettingsPanel: React.FC<SettingsPanelProps> = ({ isOpen, onClose }) => {
    const store = useEditorStore();

    // Handlers
    const handleFontUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            store.uploadFont(e.target.files[0]);
        }
    };

    return (
        <>
        {/* Backdrop for mobile */}
        {isOpen && (
            <div
                className="fixed inset-0 bg-black/20 z-30 md:hidden animate-fade-in"
                onClick={onClose}
            />
        )}

        <div className={`
            fixed inset-y-0 right-0 w-80 bg-white shadow-xl z-40
            transition-all duration-300 ease-in-out
            ${isOpen ? 'translate-x-0' : 'translate-x-full'}
            md:translate-x-0 md:static md:h-full md:border-l md:shadow-none
            flex flex-col
        `}>
            {/* Header */}
            <div className="p-4 pt-[calc(1rem+env(safe-area-inset-top))] border-b flex justify-between items-center bg-gray-50">
                <h2 className="font-semibold text-gray-800">Settings</h2>
                <button onClick={onClose} className="md:hidden text-gray-500 hover:text-gray-700">
                    ✕
                </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-6">

                {/* Model Selection */}
                <section>
                    <div className="flex items-center gap-2 mb-3 text-sm font-medium text-gray-700">
                        <Cpu className="w-4 h-4" />
                        Model
                    </div>
                    <div className="flex rounded-lg border border-gray-200 p-1 bg-gray-50">
                        <button
                            onClick={() => store.setModelType('fp32')}
                            className={`flex-1 py-1.5 text-sm rounded-md transition-colors ${store.currentModelType === 'fp32' ? 'bg-white shadow-sm text-blue-600 font-medium' : 'text-gray-500 hover:text-gray-700'}`}
                        >
                            FP32
                        </button>
                        <button
                            onClick={() => store.setModelType('int8')}
                            className={`flex-1 py-1.5 text-sm rounded-md transition-colors ${store.currentModelType === 'int8' ? 'bg-white shadow-sm text-blue-600 font-medium' : 'text-gray-500 hover:text-gray-700'}`}
                        >
                            INT8
                        </button>
                    </div>
                </section>

                <hr className="border-gray-100" />

                {/* Mask Mode */}
                <section>
                    <h3 className="text-sm font-medium text-gray-700 mb-3">Default Mask Mode</h3>
                    <div className="grid grid-cols-2 gap-3">
                        <button
                            onClick={() => store.setMaskType('emoji')}
                            className={`flex flex-col items-center gap-2 p-3 rounded-xl border-2 transition-all ${store.currentMaskType === 'emoji' ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-transparent bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
                        >
                            <Smile className="w-6 h-6" />
                            <span className="text-sm font-medium">Emoji</span>
                        </button>
                        <button
                            onClick={() => store.setMaskType('blur')}
                            className={`flex flex-col items-center gap-2 p-3 rounded-xl border-2 transition-all ${store.currentMaskType === 'blur' ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-transparent bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
                        >
                            <Sparkles className="w-6 h-6" />
                            <span className="text-sm font-medium">Blur</span>
                        </button>
                    </div>
                </section>

                {/* Emoji Settings */}
                {store.currentMaskType === 'emoji' && (
                    <div className="space-y-6 animate-fade-in">
                        <section>
                            <div className="flex items-center justify-between mb-2">
                                <label className="text-sm font-medium text-gray-700">Random Emoji List</label>
                                <span className="text-xs text-gray-400">Comma separated</span>
                            </div>
                            <textarea
                                value={store.randomEmojiList.join(', ')}
                                onChange={(e) => {
                                    const list = e.target.value.split(',').map(s => s.trim()).filter(s => s.length > 0);
                                    store.setRandomEmojiList(list);
                                }}
                                className="w-full h-24 p-3 rounded-lg border border-gray-300 text-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
                                placeholder="😂, 😎, 😆..."
                            />
                        </section>

                        <section>
                            <div className="flex items-center gap-2 mb-3 text-sm font-medium text-gray-700">
                                <Type className="w-4 h-4" />
                                Font
                            </div>
                            <div className="space-y-3">
                                <select
                                    value={store.currentFont}
                                    onChange={(e) => store.setCurrentFont(e.target.value)}
                                    className="w-full p-2.5 rounded-lg border border-gray-300 bg-white text-sm"
                                >
                                    <option value="">System Default</option>
                                    {store.availableFonts.map(f => (
                                        <option key={f} value={f}>{f}</option>
                                    ))}
                                </select>
                                <label className="flex items-center justify-center gap-2 w-full p-2.5 rounded-lg border border-dashed border-gray-300 text-gray-500 hover:bg-gray-50 hover:border-gray-400 cursor-pointer transition-colors">
                                    <Upload className="w-4 h-4" />
                                    <span className="text-sm">Upload Font File</span>
                                    <input type="file" accept=".ttf,.otf,.woff,.woff2" className="hidden" onChange={handleFontUpload} />
                                </label>
                            </div>
                        </section>
                    </div>
                )}

                {/* Blur Settings */}
                {store.currentMaskType === 'blur' && (
                    <div className="space-y-4 animate-fade-in">
                        <section>
                            <h3 className="text-sm font-medium text-gray-700 mb-3">Blur Type</h3>
                            <div className="flex flex-col gap-2">
                                {['gaussian', 'pixelate'].map((type) => (
                                    <label key={type} className={`flex items-center p-3 rounded-lg border cursor-pointer transition-colors ${store.currentBlurType === type ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:bg-gray-50'}`}>
                                        <input
                                            type="radio"
                                            name="blurType"
                                            value={type}
                                            checked={store.currentBlurType === type}
                                            onChange={() => store.setBlurType(type as any)}
                                            className="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500"
                                        />
                                        <span className="ml-3 text-sm font-medium text-gray-700 capitalize">{type}</span>
                                    </label>
                                ))}
                            </div>
                        </section>
                    </div>
                )}
            </div>

            <div className="p-4 pb-[calc(1rem+env(safe-area-inset-bottom))] border-t bg-gray-50 text-xs text-center text-gray-400">
                FaceMoji Web Editor
            </div>
        </div>
        </>
    );
};
