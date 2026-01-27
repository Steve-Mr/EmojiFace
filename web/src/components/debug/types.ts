export interface LogEntry {
  timestamp: string;
  level: 'info' | 'warn' | 'error';
  message: string;
}

export interface InferenceConfig {
  backend: 'webgpu' | 'wasm-mt' | 'wasm-st';
  model: 'fp32' | 'int8';
}
