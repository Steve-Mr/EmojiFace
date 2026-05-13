import { create } from 'zustand';
import type { Detection, Mask, MaskType, BlurType } from '../domain/types';
import { faceDetector } from '../ai/OnnxFaceDetector';
import { fontRepo } from '../infrastructure/FontRepository';
import { persistenceRepo } from '../infrastructure/PersistenceRepository';

export interface EditorState {
  image: ImageBitmap | HTMLImageElement | null;
  imageBlob: Blob | null;
  detections: Detection[];
  masks: Mask[];
  selectedMaskId: string | null;
  isProcessing: boolean;
  fontsLoaded: boolean; // Add flag to track if fonts are ready

  currentMaskType: MaskType;
  currentEmoji: string;
  currentBlurType: BlurType;

  randomEmojiList: string[];
  isManualAddMode: boolean;

  // Model Selection
  currentModelType: 'fp32' | 'int8';
  setModelType: (type: 'fp32' | 'int8') => void;

  availableFonts: string[];
  currentFont: string;

  loadFonts: () => Promise<void>;
  uploadFont: (file: File) => Promise<void>;
  setCurrentFont: (fontName: string) => void;

  setImage: (file: File | Blob) => Promise<void>;
  processImage: () => Promise<void>;
  updateMask: (id: string, updates: Partial<Mask['config']>) => void;
  setMaskType: (type: MaskType) => void;
  setBlurType: (type: BlurType) => void;
  setEmoji: (emoji: string) => void;
  selectMask: (id: string | null) => void;

  restoreState: () => Promise<void>;
  clearWorkspace: () => Promise<void>;
  setRandomEmojiList: (list: string[]) => void;
  setIsManualAddMode: (val: boolean) => void;
  addManualMask: (x: number, y: number) => void;
  deleteMask: (id: string) => void;
}

const shuffleArray = <T>(array: T[]): T[] => {
    const newArray = [...array];
    for (let i = newArray.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [newArray[i], newArray[j]] = [newArray[j], newArray[i]];
    }
    return newArray;
};

const calculateRotation = (keypoints: {x: number, y: number}[]) => {
    if (!keypoints || keypoints.length < 2) return 0;
    const leftEye = keypoints[0];
    const rightEye = keypoints[1];
    if (!leftEye || !rightEye) return 0;

    // Calculate angle in radians and convert to degrees
    const deltaY = rightEye.y - leftEye.y;
    const deltaX = rightEye.x - leftEye.x;
    return Math.atan2(deltaY, deltaX) * (180 / Math.PI);
};

export const useEditorStore = create<EditorState>((set, get) => ({
  image: null,
  imageBlob: null,
  detections: [],
  masks: [],
  selectedMaskId: null,
  isProcessing: false,
  fontsLoaded: false,
  currentMaskType: 'emoji',
  currentEmoji: '😊',
  currentBlurType: 'gaussian',

  randomEmojiList: ['😂', '😎', '😆', '😋', '🫡', '😊', '😜', '🤠'],
  isManualAddMode: false,

  currentModelType: 'fp32',
  setModelType: (type) => {
      set({ currentModelType: type });
      const path = type === 'int8' ? '/models/yolov8n-face-int8.onnx' : '/models/yolov8n-face.onnx';
      faceDetector.configure(path);

      const { image } = get();
      if (image) {
          get().processImage();
      }
  },

  availableFonts: [],
  currentFont: '',

  loadFonts: async () => {
      if (typeof document === 'undefined') return;

      const fonts = await fontRepo.getAllFonts();
      const names = fonts.map(f => f.name);
      for (const f of fonts) {
          const face = new FontFace(f.name, f.buffer);
          try {
              await face.load();
              document.fonts.add(face);
          } catch(e) {
              console.error(`Failed to load font ${f.name}`, e);
          }
      }
      // Force update to trigger re-render if masks already exist
      set({ availableFonts: names, fontsLoaded: true });
  },

  uploadFont: async (file: File) => {
      if (typeof document === 'undefined') return;

      const name = file.name.replace(/\.[^/.]+$/, "");
      const buffer = await file.arrayBuffer();
      await fontRepo.saveFont(name, buffer);

      const face = new FontFace(name, buffer);
      await face.load();
      document.fonts.add(face);

      // Update available fonts AND current font AND update all masks
      set(state => {
           const newMasks = state.masks.map(m => ({
              ...m,
              config: { ...m.config, fontFamily: name }
          }));

          return {
            availableFonts: [...state.availableFonts, name],
            currentFont: name,
            masks: newMasks
          };
      });
  },

  setCurrentFont: (name) => {
      set(state => {
          const newMasks = state.masks.map(m => ({
              ...m,
              config: { ...m.config, fontFamily: name }
          }));
          return { currentFont: name, masks: newMasks };
      });
  },

  setImage: async (file: File | Blob) => {
    const bitmap = await createImageBitmap(file);
    set({ image: bitmap, imageBlob: file, detections: [], masks: [], selectedMaskId: null });
    get().processImage();
  },

  processImage: async () => {
    const { image, currentBlurType, currentMaskType, currentFont, randomEmojiList } = get();
    if (!image) return;

    set({ isProcessing: true });
    try {
      if (!faceDetector.isLoaded()) {
          await faceDetector.load();
      }
      const detections = await faceDetector.detect(image);

      const shuffledEmojis = shuffleArray(randomEmojiList);

      const masks: Mask[] = detections.map((d, index) => {
        const emoji = shuffledEmojis.length > 0
            ? shuffledEmojis[index % shuffledEmojis.length]
            : '😊';

        return {
            id: crypto.randomUUID(),
            type: currentMaskType,
            detectionId: d.id,
            config: {
                emoji: emoji,
                blurType: currentBlurType,
                scale: 1.2,
                rotation: calculateRotation(d.keypoints),
                fontFamily: currentFont || undefined
            }
        };
      });

      set({ detections, masks, isProcessing: false });
    } catch (e) {
      console.error(e);
      set({ isProcessing: false });
    }
  },

  updateMask: (id, updates) => {
    set(state => ({
      masks: state.masks.map(m =>
        m.id === id ? { ...m, config: { ...m.config, ...updates } } : m
      )
    }));
  },

  setMaskType: (type) => {
    set(state => {
        const newMasks = state.masks.map(m => ({ ...m, type }));
        return { currentMaskType: type, masks: newMasks };
    });
  },

  setBlurType: (type) => {
      set(state => {
          const newMasks = state.masks.map(m => ({
              ...m,
              config: { ...m.config, blurType: type }
          }));
          return { currentBlurType: type, masks: newMasks };
      });
  },

  setEmoji: (emoji) => {
      set(state => {
          const newMasks = state.masks.map(m => ({
              ...m,
              config: { ...m.config, emoji: emoji }
          }));
          return { currentEmoji: emoji, masks: newMasks };
      });
  },

  selectMask: (id) => set({ selectedMaskId: id }),

  restoreState: async () => {
    const state = await persistenceRepo.loadState();

    // Always restore settings
    if (state.settings) {
        set({
            randomEmojiList: state.settings.randomEmojiList || get().randomEmojiList,
            currentMaskType: state.settings.currentMaskType || get().currentMaskType,
            currentBlurType: state.settings.currentBlurType || get().currentBlurType,
            currentEmoji: state.settings.currentEmoji || get().currentEmoji,
            currentFont: state.settings.currentFont || get().currentFont,
        });
    }

    if (state.image) {
        lastSavedImageBlob = state.image; // Sync last saved image
        const bitmap = await createImageBitmap(state.image);
        set({
            image: bitmap,
            imageBlob: state.image,
            detections: state.detections,
            masks: state.masks,
        });
    }
  },

  clearWorkspace: async () => {
    set({ image: null, imageBlob: null, detections: [], masks: [], selectedMaskId: null, isManualAddMode: false });
    await persistenceRepo.clearState();
  },

  setRandomEmojiList: (list) => set({ randomEmojiList: list }),
  setIsManualAddMode: (val) => set({ isManualAddMode: val }),

  addManualMask: (x, y) => {
      const { image, currentMaskType, currentBlurType, randomEmojiList, currentFont } = get();
      if (!image) return;

      const w = 'width' in image ? image.width : (image as HTMLImageElement).naturalWidth;
      const h = 'height' in image ? image.height : (image as HTMLImageElement).naturalHeight;
      const size = Math.min(w, h) * 0.15;

      const detectionId = `manual-${crypto.randomUUID()}`;
      const newDetection: Detection = {
          id: detectionId,
          score: 1.0,
          box: { x: x - size/2, y: y - size/2, width: size, height: size },
          keypoints: []
      };

      const usedEmojis = new Set(get().masks.map(m => m.config.emoji).filter((e): e is string => !!e));
      const availableEmojis = randomEmojiList.filter(e => !usedEmojis.has(e));
      const pool = availableEmojis.length > 0 ? availableEmojis : randomEmojiList;

      const emoji = pool.length > 0
          ? pool[Math.floor(Math.random() * pool.length)]
          : '😊';

      const newMask: Mask = {
          id: crypto.randomUUID(),
          type: currentMaskType,
          detectionId: detectionId,
          config: {
              emoji: emoji,
              blurType: currentBlurType,
              scale: 1.0,
              rotation: 0,
              fontFamily: currentFont || undefined
          }
      };

      set(state => ({
          detections: [...state.detections, newDetection],
          masks: [...state.masks, newMask],
          isManualAddMode: false
      }));
  },

  deleteMask: (id) => {
      const state = get();
      const mask = state.masks.find(m => m.id === id);
      if (!mask) return;

      const newMasks = state.masks.filter(m => m.id !== id);

      let newDetections = state.detections;
      if (mask.detectionId.startsWith('manual-')) {
          newDetections = state.detections.filter(d => d.id !== mask.detectionId);
      }

      set({ masks: newMasks, detections: newDetections, selectedMaskId: null });
  },
}));

// Subscription for persistence
let saveTimer: ReturnType<typeof setTimeout> | null = null;
let lastSavedImageBlob: Blob | null = null;

const performSave = (state: EditorState) => {
    // Only save image if it has changed
    const imageToSave = state.imageBlob !== lastSavedImageBlob ? state.imageBlob : undefined;

    // Update reference if we are saving a new image
    if (imageToSave !== undefined) {
        lastSavedImageBlob = imageToSave;
    }

    persistenceRepo.saveState(
        imageToSave,
        state.detections,
        state.masks,
        {
            randomEmojiList: state.randomEmojiList,
            currentMaskType: state.currentMaskType,
            currentBlurType: state.currentBlurType,
            currentEmoji: state.currentEmoji,
            currentFont: state.currentFont
        }
    );
};

useEditorStore.subscribe((state) => {
    if (saveTimer) clearTimeout(saveTimer);
    saveTimer = setTimeout(() => {
        saveTimer = null;
        performSave(state);
    }, 500);
});

if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden') {
            if (saveTimer) {
                clearTimeout(saveTimer);
                saveTimer = null;
                performSave(useEditorStore.getState());
            }
        }
    });
}
