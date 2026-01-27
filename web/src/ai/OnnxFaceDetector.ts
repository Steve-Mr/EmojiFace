import * as ort from 'onnxruntime-web';
import type { Detection } from '../domain/types';
import type { FaceDetector } from './FaceDetector';
import { preprocess, postprocess, MODEL_INPUT_SIZE } from './imageUtils';

export class OnnxFaceDetector implements FaceDetector {
  private session: ort.InferenceSession | null = null;
  private modelPath: string;

  constructor(modelPath: string = '/models/yolov8n-face.onnx') {
    this.modelPath = modelPath;
  }

  async load(): Promise<void> {
    if (this.session) return;
    try {
      this.session = await ort.InferenceSession.create(this.modelPath, {
        executionProviders: ['wasm'],
        graphOptimizationLevel: 'all'
      });
      console.log('Face Detector Model Loaded');
    } catch (e) {
      console.error('Failed to load model', e);
      throw e;
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
