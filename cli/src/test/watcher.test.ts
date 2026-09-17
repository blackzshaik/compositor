import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { FileWatcher, safeReadFile } from '../watcher/file-watcher.js';
import { FileChangeEvent } from '../models/preview.js';

describe('FileWatcher', () => {
  let tempDir: string;
  let watcher: FileWatcher;

  beforeEach(() => {
    tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'compositor-watcher-'));
  });

  afterEach(() => {
    if (watcher) {
      watcher.stop();
    }
    try {
      fs.rmSync(tempDir, { recursive: true, force: true });
    } catch {
      // Cleanup
    }
  });

  it('detects file modifications and debounces events', async () => {
    const testFile = path.join(tempDir, 'Sample.kt');
    fs.writeFileSync(testFile, '// initial', 'utf-8');

    const events: FileChangeEvent[] = [];
    watcher = new FileWatcher(tempDir, { debounceMs: 100 });
    watcher.onFileChange((event) => {
      events.push(event);
    });
    watcher.start();

    // Trigger rapid writes
    fs.writeFileSync(testFile, '// modification 1', 'utf-8');
    fs.writeFileSync(testFile, '// modification 2', 'utf-8');

    await new Promise((resolve) => setTimeout(resolve, 350));

    expect(events.length).toBeGreaterThanOrEqual(1);
    expect(events[events.length - 1].type).toBe('modified');
    expect(path.normalize(events[events.length - 1].filePath)).toBe(path.normalize(testFile));
  });

  it('ignores non-Kotlin files and ignored directories', async () => {
    const events: FileChangeEvent[] = [];
    watcher = new FileWatcher(tempDir, { debounceMs: 50 });
    watcher.onFileChange((event) => {
      events.push(event);
    });
    watcher.start();

    // Create a txt file (should be ignored)
    fs.writeFileSync(path.join(tempDir, 'notes.txt'), 'ignore me', 'utf-8');

    // Create a build directory with a .kt file inside (should be ignored)
    const buildDir = path.join(tempDir, 'build');
    fs.mkdirSync(buildDir);
    fs.writeFileSync(path.join(buildDir, 'Generated.kt'), '// generated', 'utf-8');

    await new Promise((resolve) => setTimeout(resolve, 200));
    expect(events).toHaveLength(0);
  });

  it('safeReadFile successfully reads text content', async () => {
    const testFile = path.join(tempDir, 'ReadTest.kt');
    fs.writeFileSync(testFile, 'package com.test\nfun test() {}', 'utf-8');

    const content = await safeReadFile(testFile);
    expect(content).toContain('fun test() {}');
  });
});
