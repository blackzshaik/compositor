import { exec } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { PreviewItem } from '../models/preview.js';
import { PreviewRegistry } from '../registry/preview-registry.js';

export interface RenderResult {
  previewId: string;
  success: boolean;
  durationMs: number;
  imagePath?: string;
  imageUrl?: string;
  error?: string;
}

export type RenderRunner = (preview: PreviewItem) => Promise<{
  success: boolean;
  imagePath?: string;
  error?: string;
}>;

export interface RenderDispatcherOptions {
  projectRoot: string;
  customRunner?: RenderRunner;
  gradleCommand?: string;
}

export class RenderDispatcher {
  private projectRoot: string;
  private registry: PreviewRegistry;
  private customRunner?: RenderRunner;
  private gradleCommand: string;
  private queue: string[] = [];
  private isProcessing = false;

  private onRenderStartedCallbacks: ((preview: PreviewItem) => void)[] = [];
  private onRenderCompletedCallbacks: ((result: RenderResult) => void)[] = [];

  constructor(registry: PreviewRegistry, options: RenderDispatcherOptions) {
    this.registry = registry;
    this.projectRoot = options.projectRoot;
    this.customRunner = options.customRunner;
    this.gradleCommand =
      options.gradleCommand ??
      (process.platform === 'win32' ? 'cmd.exe /c .\\gradlew.bat' : './gradlew');
  }

  public onRenderStarted(callback: (preview: PreviewItem) => void): this {
    this.onRenderStartedCallbacks.push(callback);
    return this;
  }

  public onRenderCompleted(callback: (result: RenderResult) => void): this {
    this.onRenderCompletedCallbacks.push(callback);
    return this;
  }

  /**
   * Sanitizes a preview ID for safe filesystem naming.
   */
  public static sanitizeId(id: string): string {
    return id.replace(/[:#./\\ ]/g, '_');
  }

  /**
   * Enqueues a preview for incremental rendering.
   */
  public enqueue(previewId: string): void {
    if (!this.queue.includes(previewId)) {
      this.queue.push(previewId);
    }
    this.processQueue();
  }

  /**
   * Enqueues multiple previews (e.g. all previews modified in a file).
   */
  public enqueueMany(previewIds: string[]): void {
    for (const id of previewIds) {
      if (!this.queue.includes(id)) {
        this.queue.push(id);
      }
    }
    this.processQueue();
  }

  public getQueueLength(): number {
    return this.queue.length;
  }

  public isBusy(): boolean {
    return this.isProcessing;
  }

  private async processQueue(): Promise<void> {
    if (this.isProcessing || this.queue.length === 0) {
      return;
    }

    this.isProcessing = true;
    const previewId = this.queue.shift()!;
    const preview = this.registry.getPreview(previewId);

    if (!preview) {
      this.isProcessing = false;
      this.processQueue();
      return;
    }

    // Mark as Rendering
    this.registry.updateStatus(previewId, 'Rendering');
    for (const cb of this.onRenderStartedCallbacks) {
      try {
        cb(preview);
      } catch (err) {
        console.error('[RenderDispatcher] onRenderStarted callback error:', err);
      }
    }

    const startTime = Date.now();
    let result: RenderResult;

    try {
      if (this.customRunner) {
        const res = await this.customRunner(preview);
        const duration = Date.now() - startTime;
        result = {
          previewId,
          success: res.success,
          durationMs: duration,
          imagePath: res.imagePath,
          imageUrl: res.imagePath
            ? `/api/previews/${encodeURIComponent(previewId)}/image?v=${Date.now()}`
            : undefined,
          error: res.error,
        };
      } else {
        result = await this.executeGradleRender(preview, startTime);
      }
    } catch (err) {
      const duration = Date.now() - startTime;
      result = {
        previewId,
        success: false,
        durationMs: duration,
        error: err instanceof Error ? err.message : String(err),
      };
    }

    // Update registry
    if (result.success && result.imagePath) {
      this.registry.updateStatus(previewId, 'Rendered', {
        durationMs: result.durationMs,
        imagePath: result.imagePath,
        imageUrl: result.imageUrl,
      });
    } else {
      this.registry.updateStatus(previewId, 'Error', {
        durationMs: result.durationMs,
        errorDetails: result.error,
      });
    }

    for (const cb of this.onRenderCompletedCallbacks) {
      try {
        cb(result);
      } catch (err) {
        console.error('[RenderDispatcher] onRenderCompleted callback error:', err);
      }
    }

    this.isProcessing = false;
    this.processQueue();
  }

  private resolveJdkHome(): string {
    const candidatePaths = [
      'C:\\Program Files\\Android\\openjdk\\jdk-21.0.8',
      'C:\\Program Files\\Android\\Android Studio\\jbr',
      process.env.JAVA_HOME ?? '',
    ];

    for (const candidate of candidatePaths) {
      if (candidate && fs.existsSync(candidate)) {
        return candidate;
      }
    }
    return 'C:\\Program Files\\Android\\openjdk\\jdk-21.0.8';
  }

  private executeGradleRender(preview: PreviewItem, startTime: number): Promise<RenderResult> {
    return new Promise((resolve) => {
      // Map module name to gradle task
      const moduleName = preview.module || 'sample-app';
      const task = `:samples:${moduleName}:exportLatestPreview`;
      const cmd = `${this.gradleCommand} ${task}`;

      const jdkHome = this.resolveJdkHome();
      const pathSep = process.platform === 'win32' ? ';' : ':';
      const jdkBin = path.join(jdkHome, 'bin');
      const updatedPath = fs.existsSync(jdkBin)
        ? `${jdkBin}${pathSep}${process.env.PATH ?? ''}`
        : process.env.PATH;

      const env = {
        ...process.env,
        JAVA_HOME: jdkHome,
        PATH: updatedPath,
      };

      exec(cmd, { cwd: this.projectRoot, env }, (error, stdout, stderr) => {
        const duration = Date.now() - startTime;
        if (error) {
          return resolve({
            previewId: preview.id,
            success: false,
            durationMs: duration,
            error: stderr || stdout || error.message,
          });
        }

        const sourceImage = path.join(this.projectRoot, '.compositor', 'latest_preview.png');
        if (!fs.existsSync(sourceImage)) {
          return resolve({
            previewId: preview.id,
            success: false,
            durationMs: duration,
            error: 'Gradle render completed successfully but output image was not found',
          });
        }

        // Copy to dedicated previews directory for multi-preview persistence
        const previewDir = path.join(this.projectRoot, '.compositor', 'previews');
        if (!fs.existsSync(previewDir)) {
          fs.mkdirSync(previewDir, { recursive: true });
        }

        const targetImage = path.join(previewDir, `${RenderDispatcher.sanitizeId(preview.id)}.png`);
        try {
          fs.copyFileSync(sourceImage, targetImage);
        } catch {
          // Fallback to source image if copy fails
        }

        const finalImagePath = fs.existsSync(targetImage) ? targetImage : sourceImage;
        const imageUrl = `/api/previews/${encodeURIComponent(preview.id)}/image?v=${Date.now()}`;

        resolve({
          previewId: preview.id,
          success: true,
          durationMs: duration,
          imagePath: finalImagePath,
          imageUrl,
        });
      });
    });
  }
}
