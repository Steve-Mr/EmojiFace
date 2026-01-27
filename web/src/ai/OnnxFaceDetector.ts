import * as ort from 'onnxruntime-web';
import type { Detection } from '../domain/types';
import type { FaceDetector } from './FaceDetector';
import { preprocess, postprocess, MODEL_INPUT_SIZE } from './imageUtils';

// Configure ONNX Runtime Web
// Try to enable multi-threading for WASM backend (requires COOP/COEP headers)
// If headers are missing, the browser might ignore this or warn.
ort.env.wasm.numThreads = navigator.hardwareConcurrency || 4;
ort.env.wasm.simd = true;

export class OnnxFaceDetector implements FaceDetector {
  private session: ort.InferenceSession | null = null;
  private modelPath: string;

  constructor(modelPath: string = '/models/yolov8n-face.onnx') {
    this.modelPath = modelPath;
  }

  setModelPath(path: string) {
      if (this.modelPath !== path) {
          this.modelPath = path;
          this.session = null; // Force reload
      }
  }

  async load(): Promise<void> {
    if (this.session) return;
    try {
      // Priority: WebGPU -> WASM (Multi-threaded)
      this.session = await ort.InferenceSession.create(this.modelPath, {
        executionProviders: ['webgpu', 'wasm'],
        graphOptimizationLevel: 'all'
      });
      console.log('Face Detector Model Loaded');
    } catch (e) {
      console.error('Failed to load model with WebGPU/WASM-MT', e);

      // Fallback 1: WASM Single-threaded (for environments without COOP/COEP)
      try {
          console.warn('Retrying with WASM Single-threaded...');
          ort.env.wasm.numThreads = 1;
          this.session = await ort.InferenceSession.create(this.modelPath, {
            executionProviders: ['wasm'],
            graphOptimizationLevel: 'all'
          });
      } catch (e2) {
          // Fallback 2: No SIMD
          try {
              console.warn('Retrying with SIMD disabled...');
              ort.env.wasm.simd = false;
              this.session = await ort.InferenceSession.create(this.modelPath, {
                executionProviders: ['wasm'],
                graphOptimizationLevel: 'basic'
              });
              console.log('Face Detector Model Loaded (No SIMD)');
          } catch (retryError) {
              console.error('Failed to load model even without SIMD', retryError);
              throw retryError;
          }
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

    const results = await this.session.run(feeds);

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
