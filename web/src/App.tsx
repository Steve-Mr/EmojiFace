import { useEffect, useState } from 'react';
import { openDB } from 'idb';
import { ServiceProvider } from './services/ServiceContext';
import { CanvasView } from './components/CanvasView';
import { Toolbar } from './components/Toolbar';
import { SettingsPanel } from './components/SettingsPanel';
import { MaskControls } from './components/MaskControls';
import { ConfirmDialog } from './components/ConfirmDialog';
import { useEditorStore } from './store/editorStore';
import { faceDetector } from './ai/OnnxFaceDetector';
import { useTranslation } from './i18n/TranslationContext';

const AppContent = () => {
  const isProcessing = useEditorStore(state => state.isProcessing);
  const setImage = useEditorStore(state => state.setImage);
  const loadFonts = useEditorStore(state => state.loadFonts);
  const restoreState = useEditorStore(state => state.restoreState);
  const clearWorkspace = useEditorStore(state => state.clearWorkspace);

  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [showClearDialog, setShowClearDialog] = useState(false);
  const { t } = useTranslation();

  useEffect(() => {
    faceDetector.load().catch(console.error);
    loadFonts();
    restoreState();

    const params = new URLSearchParams(window.location.search);
    if (params.get('shared') === 'true') {
        (async () => {
             const db = await openDB('facemoji-share', 1);
             const file = await db.get('shared-files', 'latest');
             if (file && file instanceof Blob) {
                 setImage(file);
                 window.history.replaceState({}, '', '/');
             }
        })();
    }
  }, []);

  const handleClearConfirm = () => {
      clearWorkspace();
      setShowClearDialog(false);
  };

  return (
    <div className="flex flex-col md:flex-row h-[100dvh] bg-gray-50 dark:bg-black overflow-hidden transition-colors duration-300">
      {/* Main Content Area */}
      <div className="flex-1 flex flex-col relative h-full min-w-0">
          <header className="bg-white dark:bg-gray-900 p-3 pt-[calc(0.75rem+env(safe-area-inset-top))] shadow-sm z-20 flex justify-center border-b border-gray-200 dark:border-gray-800 md:hidden transition-colors">
            <h1 className="font-bold text-gray-800 dark:text-gray-100 text-lg">{t.appTitle}</h1>
          </header>

          <main className="flex-1 relative bg-gray-100 dark:bg-gray-950 flex flex-col overflow-hidden transition-colors">
            <CanvasView />
            <MaskControls />

            {isProcessing && (
              <div className="absolute inset-0 bg-black/30 dark:bg-black/50 flex items-center justify-center z-30">
                 <div className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow-xl flex items-center gap-3">
                   <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
                   <span className="font-medium text-gray-800 dark:text-gray-200">{t.detectingFaces}</span>
                 </div>
              </div>
            )}
          </main>

          <Toolbar
            onToggleSettings={() => setIsSettingsOpen(!isSettingsOpen)}
            onExportComplete={() => setShowClearDialog(true)}
            onRemoveImage={() => setShowClearDialog(true)}
          />
      </div>

      {/* Right Sidebar / Settings Drawer */}
      <SettingsPanel isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />

      <ConfirmDialog
        isOpen={showClearDialog}
        title={t.removeImageTitle}
        message={t.removeImageMessage}
        onConfirm={handleClearConfirm}
        onCancel={() => setShowClearDialog(false)}
      />
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
