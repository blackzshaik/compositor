import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { PNG } from 'pngjs';
import {
  comparePngBuffers,
  BaselineManager,
} from '../diff/visual-diff.js';

function createSolidPng(
  width: number,
  height: number,
  r: number,
  g: number,
  b: number,
  a = 255
): Buffer {
  const png = new PNG({ width, height });
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const idx = (y * width + x) * 4;
      png.data[idx] = r;
      png.data[idx + 1] = g;
      png.data[idx + 2] = b;
      png.data[idx + 3] = a;
    }
  }
  return PNG.sync.write(png);
}

function createModifiedPng(
  baseBuffer: Buffer,
  box: { x: number; y: number; w: number; h: number; r: number; g: number; b: number }
): Buffer {
  const png = PNG.sync.read(baseBuffer);
  for (let y = box.y; y < box.y + box.h; y++) {
    for (let x = box.x; x < box.x + box.w; x++) {
      if (x < png.width && y < png.height) {
        const idx = (y * png.width + x) * 4;
        png.data[idx] = box.r;
        png.data[idx + 1] = box.g;
        png.data[idx + 2] = box.b;
        png.data[idx + 3] = 255;
      }
    }
  }
  return PNG.sync.write(png);
}

describe('Visual Regression Engine (comparePngBuffers)', () => {
  it('reports 0% difference on identical image fixtures', () => {
    const imgA = createSolidPng(100, 100, 50, 100, 200);
    const imgB = createSolidPng(100, 100, 50, 100, 200);

    const diff = comparePngBuffers(imgA, imgB);

    expect(diff.hasVisualDifferences).toBe(false);
    expect(diff.differencePercentage).toBe(0);
    expect(diff.pixelCountDifferent).toBe(0);
    expect(diff.totalPixels).toBe(10000);
    expect(diff.summary).toContain('No visual regression detected');
  });

  it('accurately calculates diff percentage on modified images and generates overlay', () => {
    // 100x100 = 10,000 pixels
    const baseImg = createSolidPng(100, 100, 240, 240, 240);
    // 20x20 = 400 pixels (4.0% difference)
    const modifiedImg = createModifiedPng(baseImg, {
      x: 10,
      y: 10,
      w: 20,
      h: 20,
      r: 255,
      g: 0,
      b: 0,
    });

    const diff = comparePngBuffers(modifiedImg, baseImg);

    expect(diff.hasVisualDifferences).toBe(true);
    expect(diff.pixelCountDifferent).toBe(400);
    expect(diff.differencePercentage).toBe(4);
    expect(diff.totalPixels).toBe(10000);
    expect(diff.diffImageBase64).toBeDefined();
    expect(diff.diffImageBase64!.length).toBeGreaterThan(50);
    expect(diff.summary).toContain('Visual regression detected: 4% difference');
  });

  it('normalizes and compares images with different dimensions without crashing', () => {
    const smallImg = createSolidPng(50, 50, 255, 255, 255);
    const largeImg = createSolidPng(100, 100, 255, 255, 255);

    const diff = comparePngBuffers(smallImg, largeImg);

    expect(diff.hasVisualDifferences).toBe(true);
    expect(diff.totalPixels).toBe(10000);
    expect(diff.differencePercentage).toBeGreaterThan(0);
  });
});

describe('BaselineManager', () => {
  let tempDir: string;
  let manager: BaselineManager;

  beforeEach(() => {
    tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'compositor-baseline-test-'));
    manager = new BaselineManager(tempDir);
  });

  afterEach(() => {
    if (fs.existsSync(tempDir)) {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('saves, checks existence, and loads baseline snapshots', () => {
    const previewId = 'com.example.app.TestPreview';
    expect(manager.hasBaseline(previewId)).toBe(false);

    const sampleBuffer = createSolidPng(20, 20, 10, 20, 30);
    const savedPath = manager.saveBaseline(previewId, sampleBuffer);

    expect(fs.existsSync(savedPath)).toBe(true);
    expect(manager.hasBaseline(previewId)).toBe(true);

    const loaded = manager.loadBaseline(previewId);
    expect(loaded).not.toBeNull();
    expect(Buffer.compare(loaded!, sampleBuffer)).toBe(0);
  });

  it('handles timestamped historical baseline snapshots', () => {
    const previewId = 'com.example.app.TimestampPreview';
    const ts = 1726500000000;
    const sampleBuffer = createSolidPng(10, 10, 100, 150, 200);

    manager.saveBaseline(previewId, sampleBuffer, ts);

    const loadedTs = manager.loadBaseline(previewId, ts);
    expect(loadedTs).not.toBeNull();
    expect(Buffer.compare(loadedTs!, sampleBuffer)).toBe(0);

    const loadedDefault = manager.loadBaseline(previewId);
    expect(loadedDefault).not.toBeNull();
    expect(Buffer.compare(loadedDefault!, sampleBuffer)).toBe(0);
  });
});
