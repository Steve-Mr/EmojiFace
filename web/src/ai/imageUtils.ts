import type { Detection, Point, Rect } from '../domain/types';

export const MODEL_INPUT_SIZE = 640;

export interface PreprocessResult {
  tensor: Float32Array;
  scale: number;
  xPadding: number;
  yPadding: number;
}

export function preprocess(
  image: ImageBitmap | HTMLImageElement | HTMLCanvasElement
): PreprocessResult {
  let canvas: OffscreenCanvas | HTMLCanvasElement;
  let ctx: OffscreenCanvasRenderingContext2D | CanvasRenderingContext2D | null;

  if (typeof OffscreenCanvas !== 'undefined') {
    canvas = new OffscreenCanvas(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);
    ctx = canvas.getContext('2d');
  } else {
    canvas = document.createElement('canvas');
    canvas.width = MODEL_INPUT_SIZE;
    canvas.height = MODEL_INPUT_SIZE;
    ctx = canvas.getContext('2d');
  }

  if (!ctx) throw new Error('Cannot get 2d context');

  // Need to handle ImageBitmap vs HTMLImageElement for dimensions
  const w = 'width' in image ? (image.width as number) : (image as HTMLImageElement).naturalWidth;
  const h = 'height' in image ? (image.height as number) : (image as HTMLImageElement).naturalHeight;

  const scale = Math.min(MODEL_INPUT_SIZE / w, MODEL_INPUT_SIZE / h);
  const nw = w * scale;
  const nh = h * scale;
  const xPadding = (MODEL_INPUT_SIZE - nw) / 2;
  const yPadding = (MODEL_INPUT_SIZE - nh) / 2;

  ctx.fillStyle = '#808080';
  ctx.fillRect(0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);
  ctx.drawImage(image, xPadding, yPadding, nw, nh);

  const imageData = ctx.getImageData(0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);
  const { data } = imageData;

  // CHW format: [R, G, B]
  const tensor = new Float32Array(3 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE);
  const size = MODEL_INPUT_SIZE * MODEL_INPUT_SIZE;

  for (let i = 0; i < size; i++) {
    const r = data[i * 4 + 0] / 255.0;
    const g = data[i * 4 + 1] / 255.0;
    const b = data[i * 4 + 2] / 255.0;

    tensor[i] = r;
    tensor[i + size] = g;
    tensor[i + 2 * size] = b;
  }

  return { tensor, scale, xPadding, yPadding };
}

function iou(box1: Rect, box2: Rect): number {
  const x1 = Math.max(box1.x, box2.x);
  const y1 = Math.max(box1.y, box2.y);
  const x2 = Math.min(box1.x + box1.width, box2.x + box2.width);
  const y2 = Math.min(box1.y + box1.height, box2.y + box2.height);

  const intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
  const area1 = box1.width * box1.height;
  const area2 = box2.width * box2.height;

  if (area1 + area2 - intersection === 0) return 0;
  return intersection / (area1 + area2 - intersection);
}

export function postprocess(
  output: Float32Array,
  scale: number,
  xPadding: number,
  yPadding: number,
  scoreThreshold: number,
  iouThreshold: number
): Detection[] {
  const boxes: Detection[] = [];
  const numAnchors = 8400;

  // Calculate channel count based on total output size
  const totalElements = output.length;
  const numChannels = Math.floor(totalElements / numAnchors);

  // Supported formats:
  // 5 channels: cx, cy, w, h, score
  // 15 channels: cx, cy, w, h, score, [x,y]*5
  // 20 channels: cx, cy, w, h, score, [x,y,conf]*5

  const hasKeypoints = numChannels >= 15;

  for (let i = 0; i < numAnchors; i++) {
    const score = output[4 * numAnchors + i];
    if (score < scoreThreshold) continue;

    const cx = output[0 * numAnchors + i];
    const cy = output[1 * numAnchors + i];
    const w = output[2 * numAnchors + i];
    const h = output[3 * numAnchors + i];

    const x_model = cx - w / 2;
    const y_model = cy - h / 2;

    const x = (x_model - xPadding) / scale;
    const y = (y_model - yPadding) / scale;
    const width = w / scale;
    const height = h / scale;

    const keypoints: Point[] = [];

    if (hasKeypoints) {
        const kptStep = numChannels === 20 ? 3 : 2;
        // 5 keypoints
        for (let k = 0; k < 5; k++) {
            const kx = output[(5 + k * kptStep) * numAnchors + i];
            const ky = output[(5 + k * kptStep + 1) * numAnchors + i];

            keypoints.push({
                x: (kx - xPadding) / scale,
                y: (ky - yPadding) / scale
            });
        }
    }

    boxes.push({
      id: crypto.randomUUID(),
      score,
      box: { x, y, width, height },
      keypoints
    });
  }

  // NMS
  boxes.sort((a, b) => b.score - a.score);
  const result: Detection[] = [];
  const active = new Array(boxes.length).fill(true);

  for (let i = 0; i < boxes.length; i++) {
    if (!active[i]) continue;
    result.push(boxes[i]);
    for (let j = i + 1; j < boxes.length; j++) {
      if (!active[j]) continue;
      if (iou(boxes[i].box, boxes[j].box) > iouThreshold) {
        active[j] = false;
      }
    }
  }

  return result;
}
