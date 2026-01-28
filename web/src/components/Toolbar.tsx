import React from 'react';
import { useEditorStore } from '../store/editorStore';
import { ImagePlus, Download, Sparkles, Smile, Type, Cpu } from 'lucide-react';
import { exportImage } from '../utils/exporter';

export const Toolbar: React.FC = () => {
  const store = useEditorStore();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      store.setImage(e.target.files[0]);
    }
  };

  const handleFontUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      store.uploadFont(e.target.files[0]);
    }
  };

  const selectedMask = store.masks.find(m => m.id === store.selectedMaskId);
  const activeEmoji = selectedMask?.config.emoji || store.currentEmoji;
  const activeBlurType = selectedMask?.config.blurType || store.currentBlurType;

  return (
    <div className="bg-white border-t border-gray-200 p-4 pb-8 flex flex-col gap-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1)] z-10">
      {/* Settings Row */}
      <div className="flex flex-col gap-3 min-h-[60px]">
         {/* Model Selector */}
         <div className="flex items-center gap-3 absolute top-[-40px] right-4 bg-white/90 p-2 rounded-lg shadow-sm text-xs">
            <Cpu className="w-4 h-4 text-gray-600" />
            <span className="font-medium text-gray-600">Model:</span>
            <div className="flex rounded overflow-hidden border border-gray-300">
                <button
                    onClick={() => store.setModelType('fp32')}
                    className={`px-2 py-1 ${store.currentModelType === 'fp32' ? 'bg-blue-100 text-blue-700 font-bold' : 'bg-white text-gray-600'}`}
                >FP32</button>
                <div className="w-[1px] bg-gray-300"></div>
                <button
                    onClick={() => store.setModelType('int8')}
                    className={`px-2 py-1 ${store.currentModelType === 'int8' ? 'bg-blue-100 text-blue-700 font-bold' : 'bg-white text-gray-600'}`}
                >INT8</button>
            </div>
         </div>

         {store.currentMaskType === 'emoji' && (
             <>
             <div className="flex items-center gap-3">
                 <span className="font-medium text-sm text-gray-700">Emoji</span>
                 <input
                    type="text"
                    value={activeEmoji}
                    onChange={(e) => {
                        const val = e.target.value;
                        if(selectedMask) store.updateMask(selectedMask.id, { emoji: val });
                        else store.setEmoji(val);
                    }}
                    className="border border-gray-300 rounded px-2 py-1 w-24 text-center text-xl"
                 />
             </div>
             <div className="flex items-center gap-3">
                 <span className="font-medium text-sm text-gray-700">Font</span>
                 <select
                    value={selectedMask?.config.fontFamily || store.currentFont}
                    onChange={(e) => {
                        const val = e.target.value;
                        if(selectedMask) store.updateMask(selectedMask.id, { fontFamily: val || undefined });
                        else store.setCurrentFont(val);
                    }}
                    className="border border-gray-300 rounded px-2 py-1 max-w-[120px] text-sm"
                 >
                    <option value="">System</option>
                    {store.availableFonts.map(f => (
                        <option key={f} value={f}>{f}</option>
                    ))}
                 </select>
                 <label className="cursor-pointer bg-gray-100 p-1 rounded hover:bg-gray-200" title="Upload Font">
                    <Type className="w-4 h-4 text-gray-600" />
                    <input type="file" accept=".ttf,.otf,.woff,.woff2" className="hidden" onChange={handleFontUpload} />
                 </label>
             </div>
             </>
         )}

         {store.currentMaskType === 'blur' && (
             <div className="flex items-center gap-3">
                 <span className="font-medium text-sm text-gray-700">Type</span>
                 <div className="flex rounded-md shadow-sm" role="group">
                    <button
                        onClick={() => {
                            if(selectedMask) store.updateMask(selectedMask.id, { blurType: 'gaussian' });
                            else store.setBlurType('gaussian');
                        }}
                        className={`px-3 py-1 text-sm border rounded-l-md ${activeBlurType === 'gaussian' ? 'bg-blue-50 border-blue-200 text-blue-700' : 'bg-white border-gray-300 text-gray-700'}`}
                    >Gaussian</button>
                    <button
                        onClick={() => {
                            if(selectedMask) store.updateMask(selectedMask.id, { blurType: 'pixelate' });
                            else store.setBlurType('pixelate');
                        }}
                        className={`px-3 py-1 text-sm border-t border-b border-r rounded-r-md ${activeBlurType === 'pixelate' ? 'bg-blue-50 border-blue-200 text-blue-700' : 'bg-white border-gray-300 text-gray-700'}`}
                    >Pixelate</button>
                 </div>
             </div>
         )}

         {selectedMask && (
             <div className="flex items-center gap-3">
                 <span className="font-medium text-sm text-gray-700 w-10">Scale</span>
                 <input
                    type="range" min="0.5" max="3" step="0.1"
                    value={selectedMask.config.scale}
                    onChange={(e) => store.updateMask(selectedMask.id, { scale: parseFloat(e.target.value) })}
                    className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                 />
             </div>
         )}
      </div>

      {/* Main Actions Row */}
      <div className="flex justify-between items-center border-t pt-4">
        <label className="flex items-center justify-center w-10 h-10 rounded-full bg-gray-100 hover:bg-gray-200 cursor-pointer">
          <ImagePlus className="w-5 h-5 text-gray-700" />
          <input type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
        </label>

        <div className="flex gap-4 bg-gray-100 rounded-full p-1">
            <button
                className={`p-2 rounded-full transition-colors ${store.currentMaskType === 'emoji' ? 'bg-white shadow text-blue-600' : 'text-gray-500'}`}
                onClick={() => store.setMaskType('emoji')}
            >
                <Smile className="w-6 h-6" />
            </button>
             <button
                className={`p-2 rounded-full transition-colors ${store.currentMaskType === 'blur' ? 'bg-white shadow text-blue-600' : 'text-gray-500'}`}
                onClick={() => store.setMaskType('blur')}
            >
                <Sparkles className="w-6 h-6" />
            </button>
        </div>

        <button
            className="flex items-center justify-center w-10 h-10 rounded-full bg-blue-600 hover:bg-blue-700 text-white shadow-md"
            onClick={() => exportImage(store)}
        >
          <Download className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
