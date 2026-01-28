import { useEffect } from 'react';
import { openDB } from 'idb';
import { ServiceProvider } from './services/ServiceContext';
import { CanvasView } from './components/CanvasView';
import { Toolbar } from './components/Toolbar';
import { useEditorStore } from './store/editorStore';
import { DebugConsole } from './components/debug/DebugConsole';
import { faceDetector } from './ai/OnnxFaceDetector';

const AppContent = () => {
  const isProcessing = useEditorStore(state => state.isProcessing);
  const setImage = useEditorStore(state => state.setImage);
  const loadFonts = useEditorStore(state => state.loadFonts);

  useEffect(() => {
    // Start loading and warming up the model immediately
    faceDetector.load().catch(console.error);

    loadFonts();
    const params = new URLSearchParams(window.location.search);
    if (params.get('shared') === 'true') {
        (async () => {
             const db = await openDB('facemoji-share', 1);
             const file = await db.get('shared-files', 'latest');
             if (file && file instanceof Blob) { // File is Blob
                 setImage(file);
                 window.history.replaceState({}, '', '/');
             }
        })();
    }
  }, []);

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      <header className="bg-white p-3 shadow-sm z-20 flex justify-center border-b border-gray-200">
        <h1 className="font-bold text-gray-800 text-lg">FaceMoji Web</h1>
      </header>

      <main className="flex-1 relative overflow-hidden flex flex-col">
        <CanvasView />

        {isProcessing && (
          <div className="absolute inset-0 bg-black/30 flex items-center justify-center z-30">
             <div className="bg-white p-4 rounded-lg shadow-xl flex items-center gap-3">
               <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
               <span className="font-medium text-gray-800">Detecting faces...</span>
             </div>
          </div>
        )}
      </main>

      <Toolbar />
      <DebugConsole />
    </div>
  );
};

export default function App() {
  return (
    <ServiceProvider>
      <AppContent />
    </ServiceProvider>
  );
}
