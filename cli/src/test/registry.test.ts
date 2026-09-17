import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { PreviewRegistry } from '../registry/preview-registry.js';
import { PreviewDefinition } from '../models/preview.js';

describe('PreviewRegistry', () => {
  let tempDir: string;
  let jsonPath: string;
  let registry: PreviewRegistry;

  beforeEach(() => {
    tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'compositor-test-'));
    jsonPath = path.join(tempDir, 'previews.json');
    registry = new PreviewRegistry(jsonPath);
  });

  afterEach(() => {
    try {
      fs.rmSync(tempDir, { recursive: true, force: true });
    } catch {
      // Ignored in cleanup
    }
  });

  it('generates deterministic preview IDs', () => {
    const def: PreviewDefinition = {
      functionName: 'GreetingPreview',
      packageName: 'com.compositor.sample',
      parameters: { name: 'Dark Theme' },
      line: 30,
      filePath: 'samples/Greeting.kt',
    };

    const id = PreviewRegistry.generateId('sample-app', def);
    expect(id).toBe('sample-app:com.compositor.sample.GreetingPreview#Dark Theme');
  });

  it('registers and updates file previews', () => {
    const defs: PreviewDefinition[] = [
      {
        functionName: 'PreviewOne',
        packageName: 'com.example',
        parameters: { name: 'P1' },
        line: 10,
        filePath: 'src/Ui.kt',
      },
      {
        functionName: 'PreviewTwo',
        packageName: 'com.example',
        parameters: { group: 'Forms' },
        line: 20,
        filePath: 'src/Ui.kt',
      },
    ];

    const items = registry.updateFilePreviews('src/Ui.kt', defs, 'ui-module');
    expect(items).toHaveLength(2);
    expect(registry.getAllPreviews()).toHaveLength(2);

    const catalog = registry.getCatalog();
    expect(catalog.totalCount).toBe(2);
    expect(catalog.byModule['ui-module']).toHaveLength(2);
    expect(catalog.byGroup['Forms']).toHaveLength(1);

    // Remove PreviewTwo from the file
    registry.updateFilePreviews('src/Ui.kt', [defs[0]], 'ui-module');
    expect(registry.getAllPreviews()).toHaveLength(1);
    expect(registry.getAllPreviews()[0].definition.functionName).toBe('PreviewOne');
  });

  it('removes all previews when a file is deleted', () => {
    const def: PreviewDefinition = {
      functionName: 'CardPreview',
      packageName: 'com.example',
      parameters: {},
      line: 15,
      filePath: 'src/Card.kt',
    };

    registry.updateFilePreviews('src/Card.kt', [def], 'app');
    expect(registry.getAllPreviews()).toHaveLength(1);

    const removed = registry.removeFile('src/Card.kt');
    expect(removed).toBe(1);
    expect(registry.getAllPreviews()).toHaveLength(0);
  });

  it('updates rendering state and status transitions', () => {
    const def: PreviewDefinition = {
      functionName: 'ButtonPreview',
      packageName: 'com.example',
      parameters: {},
      line: 5,
      filePath: 'src/Button.kt',
    };

    const [item] = registry.updateFilePreviews('src/Button.kt', [def], 'app');
    expect(item.status).toBe('Pending');

    registry.updateStatus(item.id, 'Rendering');
    expect(registry.getPreview(item.id)?.status).toBe('Rendering');

    registry.updateStatus(item.id, 'Rendered', {
      durationMs: 450,
      imagePath: '/out/preview.png',
      imageUrl: '/api/previews/app:com.example.ButtonPreview/image',
    });

    const updated = registry.getPreview(item.id);
    expect(updated?.status).toBe('Rendered');
    expect(updated?.durationMs).toBe(450);
    expect(updated?.lastRenderedAt).toBeDefined();
  });

  it('persists and restores registry state from disk', () => {
    const def: PreviewDefinition = {
      functionName: 'ModalPreview',
      packageName: 'com.example',
      parameters: {},
      line: 25,
      filePath: 'src/Modal.kt',
    };

    registry.updateFilePreviews('src/Modal.kt', [def], 'app');
    expect(fs.existsSync(jsonPath)).toBe(true);

    const reloadedRegistry = new PreviewRegistry(jsonPath);
    expect(reloadedRegistry.getAllPreviews()).toHaveLength(1);
    expect(reloadedRegistry.getAllPreviews()[0].definition.functionName).toBe('ModalPreview');
  });
});
