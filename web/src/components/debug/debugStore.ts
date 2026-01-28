import { create } from 'zustand';
import type { LogEntry, InferenceConfig } from './types';
import { faceDetector } from '../../ai/OnnxFaceDetector';

interface DebugState {
  logs: LogEntry[];
  isOpen: boolean;
  config: InferenceConfig;

  toggleOpen: () => void;
  addLog: (level: LogEntry['level'], message: string) => void;
  setConfig: (config: Partial<InferenceConfig>) => void;
  clearLogs: () => void;
}

export const useDebugStore = create<DebugState>((set, get) => ({
  logs: [],
  isOpen: false,
  config: {
    backend: 'wasm-st',
    model: 'fp32'
  },

  toggleOpen: () => set(state => ({ isOpen: !state.isOpen })),

  addLog: (level, message) => set(state => ({
    logs: [...state.logs, {
      timestamp: new Date().toLocaleTimeString(),
      level,
      message
    }]
  })),

  setConfig: (updates) => {
    const newConfig = { ...get().config, ...updates };
    set({ config: newConfig });

    // Apply changes
    const path = newConfig.model === 'int8' ? '/models/yolov8n-face-int8.onnx' : '/models/yolov8n-face.onnx';

    // Pass backend preference to detector
    faceDetector.configure(path, newConfig.backend as any);
  },

  clearLogs: () => set({ logs: [] })
}));
