import { create } from 'zustand';
import type { Detection, Mask, MaskType, BlurType } from '../domain/types';
import { faceDetector } from '../ai/OnnxFaceDetector';
import { fontRepo } from '../infrastructure/FontRepository';

export interface EditorState {
  image: ImageBitmap | HTMLImageElement | null;
  detections: Detection[];
  masks: Mask[];
  selectedMaskId: string | null;
  isProcessing: boolean;

  currentMaskType: MaskType;
  currentEmoji: string;
  currentBlurType: BlurType;

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
}

export const useEditorStore = create<EditorState>((set, get) => ({
  image: null,
  detections: [],
  masks: [],
  selectedMaskId: null,
  isProcessing: false,
  currentMaskType: 'emoji',
  currentEmoji: '😊',
  currentBlurType: 'gaussian',

  availableFonts: [],
  currentFont: '',

  loadFonts: async () => {
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
      set({ availableFonts: names });
  },

  uploadFont: async (file: File) => {
      const name = file.name.replace(/\.[^/.]+$/, "");
      const buffer = await file.arrayBuffer();
      await fontRepo.saveFont(name, buffer);

      const face = new FontFace(name, buffer);
      await face.load();
      document.fonts.add(face);

      set(state => ({
          availableFonts: [...state.availableFonts, name],
          currentFont: name
      }));
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
    set({ image: bitmap, detections: [], masks: [], selectedMaskId: null });
    get().processImage();
  },

  processImage: async () => {
    const { image, currentEmoji, currentBlurType, currentMaskType, currentFont } = get();
    if (!image) return;

    set({ isProcessing: true });
    try {
      if (!faceDetector.isLoaded()) {
          await faceDetector.load();
      }
      const detections = await faceDetector.detect(image);

      const masks: Mask[] = detections.map(d => ({
        id: crypto.randomUUID(),
        type: currentMaskType,
        detectionId: d.id,
        config: {
          emoji: currentEmoji,
          blurType: currentBlurType,
          scale: 1.2,
          rotation: 0,
          fontFamily: currentFont || undefined
        }
      }));

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
}));
