import * as ort from 'onnxruntime-web';
import type { Detection } from '../domain/types';
import type { FaceDetector } from './FaceDetector';
import { preprocess, postprocess, MODEL_INPUT_SIZE } from './imageUtils';
import { useDebugStore } from '../components/debug/debugStore';

// Default configuration (Will be overridden by configure())
ort.env.wasm.numThreads = 1;
ort.env.wasm.simd = true;

export class OnnxFaceDetector implements FaceDetector {
  private session: ort.InferenceSession | null = null;
  private loadingPromise: Promise<void> | null = null;
  private modelPath: string;
  private backend: 'wasm-st' = 'wasm-st';

  constructor(modelPath: string = '/models/yolov8n-face.onnx') {
    this.modelPath = modelPath;
  }

  configure(path: string, backend?: 'wasm-st') {
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
    if (this.loadingPromise) return this.loadingPromise;

    this.loadingPromise = (async () => {
        const logger = useDebugStore.getState().addLog;
        logger('info', `Loading model... Path: ${this.modelPath}, Backend: ${this.backend}`);

        const options: ort.InferenceSession.SessionOptions = {
            graphOptimizationLevel: 'all',
            executionProviders: ['wasm']
        };

        // Force single thread
        ort.env.wasm.numThreads = 1;
        ort.env.wasm.simd = true;

        try {
          const start = performance.now();
          this.session = await ort.InferenceSession.create(this.modelPath, options);
          const end = performance.now();
          logger('info', `Model loaded successfully in ${(end - start).toFixed(0)}ms`);

          // Warmup
          logger('info', 'Warming up model...');
          const warmupStart = performance.now();
          const zeroTensor = new ort.Tensor('float32', new Float32Array(1 * 3 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE), [1, 3, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE]);
          const feeds: Record<string, ort.Tensor> = {};
          feeds[this.session.inputNames[0]] = zeroTensor;
          await this.session.run(feeds);
          const warmupEnd = performance.now();
          logger('info', `Warmup completed in ${(warmupEnd - warmupStart).toFixed(0)}ms`);

        } catch (e: any) {
          logger('error', `Load failed: ${e.message}`);
          throw e;
        } finally {
          this.loadingPromise = null;
        }
    })();

    return this.loadingPromise;
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
