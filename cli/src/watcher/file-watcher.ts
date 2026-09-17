import fs from 'node:fs';
import path from 'node:path';
import { FileChangeEvent } from '../models/preview.js';

export interface FileWatcherOptions {
  debounceMs?: number;
  ignoredDirs?: string[];
  extensions?: string[];
}

export class FileWatcher {
  private watchPaths: string[];
  private options: Required<FileWatcherOptions>;
  private watchers: fs.FSWatcher[] = [];
  private debounceTimers = new Map<string, NodeJS.Timeout>();
  private knownFiles = new Set<string>();
  private paused = false;
  private isRunning = false;

  private onEventCallback?: (event: FileChangeEvent) => void | Promise<void>;

  constructor(watchPaths: string | string[], options: FileWatcherOptions = {}) {
    this.watchPaths = Array.isArray(watchPaths) ? watchPaths : [watchPaths];
    this.options = {
      debounceMs: options.debounceMs ?? 250,
      ignoredDirs: options.ignoredDirs ?? [
        'build',
        '.gradle',
        '.compositor',
        '.git',
        'node_modules',
        '.idea',
        'dist',
      ],
      extensions: options.extensions ?? ['.kt'],
    };
  }

  public onFileChange(callback: (event: FileChangeEvent) => void | Promise<void>): this {
    this.onEventCallback = callback;
    return this;
  }

  public start(): void {
    if (this.isRunning) return;
    this.isRunning = true;

    for (const targetPath of this.watchPaths) {
      if (!fs.existsSync(targetPath)) continue;

      // Scan initial files to know existing files
      this.scanDirectory(targetPath);

      try {
        const watcher = fs.watch(targetPath, { recursive: true }, (_eventType, filename) => {
          if (!filename || this.paused) return;

          const resolvedPath = path.isAbsolute(filename) ? filename : path.join(targetPath, filename);
          this.handleRawFsEvent(resolvedPath);
        });

        this.watchers.push(watcher);
      } catch (err) {
        console.warn(`[FileWatcher] Failed to attach watcher to ${targetPath}:`, err);
      }
    }
  }

  public pause(): void {
    this.paused = true;
  }

  public resume(): void {
    this.paused = false;
  }

  public stop(): void {
    this.isRunning = false;
    for (const timer of this.debounceTimers.values()) {
      clearTimeout(timer);
    }
    this.debounceTimers.clear();

    for (const watcher of this.watchers) {
      try {
        watcher.close();
      } catch {
        // Ignored
      }
    }
    this.watchers = [];
  }

  public getStatus(): { isRunning: boolean; isPaused: boolean; knownFilesCount: number } {
    return {
      isRunning: this.isRunning,
      isPaused: this.paused,
      knownFilesCount: this.knownFiles.size,
    };
  }

  private handleRawFsEvent(filePath: string): void {
    const ext = path.extname(filePath);
    if (!this.options.extensions.includes(ext)) {
      return;
    }

    // Check if path contains any ignored directory segments
    const normalized = path.normalize(filePath);
    const parts = normalized.split(path.sep);
    for (const ignored of this.options.ignoredDirs) {
      if (parts.includes(ignored)) {
        return;
      }
    }

    // Debounce by filePath
    const existingTimer = this.debounceTimers.get(normalized);
    if (existingTimer) {
      clearTimeout(existingTimer);
    }

    const timer = setTimeout(() => {
      this.debounceTimers.delete(normalized);
      this.processFileChange(normalized);
    }, this.options.debounceMs);

    this.debounceTimers.set(normalized, timer);
  }

  private processFileChange(filePath: string): void {
    const exists = fs.existsSync(filePath);
    const wasKnown = this.knownFiles.has(filePath);

    let eventType: 'added' | 'modified' | 'deleted';
    if (exists && !wasKnown) {
      this.knownFiles.add(filePath);
      eventType = 'added';
    } else if (exists && wasKnown) {
      eventType = 'modified';
    } else if (!exists && wasKnown) {
      this.knownFiles.delete(filePath);
      eventType = 'deleted';
    } else {
      // Non-existent and wasn't known; ignore
      return;
    }

    if (this.onEventCallback) {
      const event: FileChangeEvent = {
        type: eventType,
        filePath,
        timestamp: Date.now(),
      };
      try {
        this.onEventCallback(event);
      } catch (err) {
        console.error('[FileWatcher] Error in onFileChange callback:', err);
      }
    }
  }

  private scanDirectory(dirPath: string): void {
    try {
      const stats = fs.statSync(dirPath);
      if (stats.isFile()) {
        const ext = path.extname(dirPath);
        if (this.options.extensions.includes(ext)) {
          this.knownFiles.add(path.normalize(dirPath));
        }
        return;
      }

      const entries = fs.readdirSync(dirPath, { withFileTypes: true });
      for (const entry of entries) {
        if (this.options.ignoredDirs.includes(entry.name)) continue;

        const fullPath = path.join(dirPath, entry.name);
        if (entry.isDirectory()) {
          this.scanDirectory(fullPath);
        } else if (entry.isFile()) {
          const ext = path.extname(entry.name);
          if (this.options.extensions.includes(ext)) {
            this.knownFiles.add(path.normalize(fullPath));
          }
        }
      }
    } catch {
      // Handled gracefully
    }
  }
}

/**
 * Helper to safely read file content on Windows, retrying on transient locks.
 */
export async function safeReadFile(filePath: string, retries = 3, delayMs = 50): Promise<string> {
  let attempt = 0;
  while (attempt < retries) {
    try {
      return fs.readFileSync(filePath, 'utf-8');
    } catch (err: unknown) {
      const nodeErr = err as NodeJS.ErrnoException;
      if (nodeErr.code === 'EBUSY' || nodeErr.code === 'EPERM' || nodeErr.code === 'EACCES') {
        attempt++;
        if (attempt >= retries) throw err;
        await new Promise((resolve) => setTimeout(resolve, delayMs));
      } else {
        throw err;
      }
    }
  }
  throw new Error(`Failed to read file after ${retries} retries: ${filePath}`);
}
