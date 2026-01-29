export interface LogEntry {
  timestamp: string;
  level: 'info' | 'warn' | 'error';
  message: string;
}

export interface InferenceConfig {
  backend: 'wasm-st';
  model: 'fp32' | 'int8';
}
