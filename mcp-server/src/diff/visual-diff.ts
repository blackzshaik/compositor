import fs from 'node:fs';
import path from 'node:path';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';
import { VisualDiffResult } from '../types.js';

export interface VisualDiffOptions {
  threshold?: number;
  includeAa?: boolean;
  alpha?: number;
  diffColor?: [number, number, number];
}

/**
 * Normalizes two PNG images to equal dimensions by padding the smaller image onto a transparent canvas.
 */
function normalizeImageDimensions(
  img1: PNG,
  img2: PNG
): { img1Data: Buffer; img2Data: Buffer; width: number; height: number } {
  const width = Math.max(img1.width, img2.width);
  const height = Math.max(img1.height, img2.height);

  if (img1.width === width && img1.height === height && img2.width === width && img2.height === height) {
    return {
      img1Data: img1.data,
      img2Data: img2.data,
      width,
      height,
    };
  }

  function padImage(src: PNG, targetW: number, targetH: number): Buffer {
    const padded = new PNG({ width: targetW, height: targetH });
    // Fill transparent by default (0, 0, 0, 0)
    padded.data.fill(0);

    for (let y = 0; y < src.height; y++) {
      for (let x = 0; x < src.width; x++) {
        const srcIdx = (y * src.width + x) * 4;
        const targetIdx = (y * targetW + x) * 4;
        padded.data[targetIdx] = src.data[srcIdx];
        padded.data[targetIdx + 1] = src.data[srcIdx + 1];
        padded.data[targetIdx + 2] = src.data[srcIdx + 2];
        padded.data[targetIdx + 3] = src.data[srcIdx + 3];
      }
    }
    return padded.data;
  }

  return {
    img1Data: img1.width === width && img1.height === height ? img1.data : padImage(img1, width, height),
    img2Data: img2.width === width && img2.height === height ? img2.data : padImage(img2, width, height),
    width,
    height,
  };
}

/**
 * Compares two PNG buffers pixel by pixel and returns visual diff metrics and overlay image.
 */
export function comparePngBuffers(
  currentBuffer: Buffer,
  baselineBuffer: Buffer,
  options: VisualDiffOptions = {}
): VisualDiffResult {
  const currentImg = PNG.sync.read(currentBuffer);
  const baselineImg = PNG.sync.read(baselineBuffer);

  const { img1Data, img2Data, width, height } = normalizeImageDimensions(currentImg, baselineImg);
  const diffImg = new PNG({ width, height });

  const threshold = options.threshold ?? 0.1;
  const diffColor: [number, number, number] = options.diffColor ?? [255, 0, 128]; // Magenta

  const numDiffPixels = pixelmatch(
    img1Data,
    img2Data,
    diffImg.data,
    width,
    height,
    {
      threshold,
      includeAA: options.includeAa ?? false,
      diffColor,
    }
  );

  const totalPixels = width * height;
  const diffPercentage = totalPixels > 0 ? Number(((numDiffPixels / totalPixels) * 100).toFixed(2)) : 0;
  const hasVisualDifferences = numDiffPixels > 0;

  const diffBuffer = PNG.sync.write(diffImg);
  const diffImageBase64 = diffBuffer.toString('base64');

  const summary = hasVisualDifferences
    ? `Visual regression detected: ${diffPercentage}% difference (${numDiffPixels} of ${totalPixels} pixels modified).`
    : `No visual regression detected (0.0% difference, ${totalPixels} pixels match baseline).`;

  return {
    hasVisualDifferences,
    differencePercentage: diffPercentage,
    diffImageBase64,
    pixelCountDifferent: numDiffPixels,
    totalPixels,
    summary,
  };
}

/**
 * Manages baseline snapshot images stored under .compositor/baselines/
 */
export class BaselineManager {
  private baselinesDir: string;

  constructor(customProjectRoot?: string) {
    const root = customProjectRoot ?? process.cwd();
    this.baselinesDir = path.join(root, '.compositor', 'baselines');
    if (!fs.existsSync(this.baselinesDir)) {
      try {
        fs.mkdirSync(this.baselinesDir, { recursive: true });
      } catch {
        // Ignored if unable to create in readonly or mock environments
      }
    }
  }

  public static sanitizeId(id: string): string {
    return id.replace(/[:#./\\ ]/g, '_');
  }

  public getBaselinesDirectory(): string {
    return this.baselinesDir;
  }

  public getBaselinePath(previewId: string, timestamp?: number): string {
    const sanitized = BaselineManager.sanitizeId(previewId);
    if (timestamp) {
      return path.join(this.baselinesDir, `${sanitized}_${timestamp}.png`);
    }
    return path.join(this.baselinesDir, `${sanitized}.png`);
  }

  public hasBaseline(previewId: string): boolean {
    const target = this.getBaselinePath(previewId);
    return fs.existsSync(target);
  }

  public saveBaseline(previewId: string, imageBuffer: Buffer, timestamp?: number): string {
    if (!fs.existsSync(this.baselinesDir)) {
      fs.mkdirSync(this.baselinesDir, { recursive: true });
    }
    const target = this.getBaselinePath(previewId, timestamp);
    fs.writeFileSync(target, imageBuffer);

    // Also update default latest baseline for this previewId if saving timestamped
    if (timestamp) {
      const defaultPath = this.getBaselinePath(previewId);
      fs.writeFileSync(defaultPath, imageBuffer);
    }

    return target;
  }

  public loadBaseline(previewId: string, timestamp?: number): Buffer | null {
    const target = this.getBaselinePath(previewId, timestamp);
    if (!fs.existsSync(target)) {
      // Fallback to default without timestamp
      const fallback = this.getBaselinePath(previewId);
      if (fs.existsSync(fallback)) {
        return fs.readFileSync(fallback);
      }
      return null;
    }
    return fs.readFileSync(target);
  }
}
