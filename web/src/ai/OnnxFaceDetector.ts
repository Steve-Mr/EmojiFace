import * as ort from 'onnxruntime-web';
import type { Detection } from '../domain/types';
import type { FaceDetector } from './FaceDetector';
import { preprocess, postprocess, MODEL_INPUT_SIZE } from './imageUtils';
import { useDebugStore } from '../components/debug/debugStore';

// Default configuration (Will be overridden by configure())
ort.env.wasm.numThreads = navigator.hardwareConcurrency || 4;
ort.env.wasm.simd = true;

export class OnnxFaceDetector implements FaceDetector {
  private session: ort.InferenceSession | null = null;
  private modelPath: string;
  private backend: 'webgpu' | 'wasm-mt' | 'wasm-st' = 'webgpu';

  constructor(modelPath: string = '/models/yolov8n-face.onnx') {
    this.modelPath = modelPath;
  }

  configure(path: string, backend?: 'webgpu' | 'wasm-mt' | 'wasm-st') {
      let needsReload = false;
      if (path && this.modelPath !== path) {
          this.modelPath = path;
          needsReload = true;
      }
      if (backend && this.backend !== backend) {
          this.backend = backend;
          needsReload = true;
      }
      if (needsReload) {
          this.session = null; // Force reload on next detect call
          useDebugStore.getState().addLog('info', `Config changed: ${(backend || this.backend).toUpperCase()} | ${path}`);
      }
  }

  async load(): Promise<void> {
    if (this.session) return;

    const logger = useDebugStore.getState().addLog;
    logger('info', `Loading model... Path: ${this.modelPath}, Backend: ${this.backend}`);

    const options: ort.InferenceSession.SessionOptions = {
        graphOptimizationLevel: 'all'
    };

    // Configure backend specific environment
    if (this.backend === 'wasm-st') {
        ort.env.wasm.numThreads = 1;
        ort.env.wasm.simd = true;
        options.executionProviders = ['wasm'];
    } else if (this.backend === 'wasm-mt') {
        ort.env.wasm.numThreads = navigator.hardwareConcurrency || 4;
        ort.env.wasm.simd = true;
        options.executionProviders = ['wasm'];
    } else {
        // webgpu (auto)
        options.executionProviders = ['webgpu', 'wasm'];
    }

    try {
      const start = performance.now();
      this.session = await ort.InferenceSession.create(this.modelPath, options);
      const end = performance.now();
      logger('info', `Model loaded successfully in ${(end - start).toFixed(0)}ms`);
    } catch (e: any) {
      logger('error', `Load failed: ${e.message}`);

      // Auto-fallback logic if explicit selection fails?
      // For now, respect user choice, but if it was 'webgpu' (auto), we can try fallback.
      if (this.backend === 'webgpu') {
           logger('warn', 'WebGPU failed, falling back to WASM-MT...');
           try {
               options.executionProviders = ['wasm'];
               this.session = await ort.InferenceSession.create(this.modelPath, options);
               logger('info', 'Fallback to WASM-MT successful');
           } catch(e2: any) {
               logger('error', `Fallback failed: ${e2.message}`);
               throw e2;
           }
      } else {
          throw e;
      }
    }
  }

  isLoaded(): boolean {
    return !!this.session;
  }

  async detect(image: ImageBitmap | HTMLImageElement): Promise<Detection[]> {
    if (!this.session) await this.load();
    if (!this.session) throw new Error('Model not loaded');

    const { tensor, scale, xPadding, yPadding } = preprocess(image);

    const inputTensor = new ort.Tensor(
      'float32',
      tensor,
      [1, 3, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE]
    );

    const feeds: Record<string, ort.Tensor> = {};
    feeds[this.session.inputNames[0]] = inputTensor;

    const start = performance.now();
    const results = await this.session.run(feeds);
    const end = performance.now();

    useDebugStore.getState().addLog('info', `Inference time: ${(end - start).toFixed(1)}ms`);

    const outputName = this.session.outputNames[0];
    const outputTensor = results[outputName];

    return postprocess(
      outputTensor.data as Float32Array,
      scale,
      xPadding,
      yPadding,
      0.45, // Score threshold
      0.5   // IOU threshold
    );
  }
}

export const faceDetector = new OnnxFaceDetector();
