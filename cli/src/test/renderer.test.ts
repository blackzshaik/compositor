import { describe, it, expect, beforeEach } from 'vitest';
import { RenderDispatcher } from '../renderer/render-dispatcher.js';
import { PreviewRegistry } from '../registry/preview-registry.js';
import { PreviewDefinition } from '../models/preview.js';

describe('RenderDispatcher', () => {
  let registry: PreviewRegistry;
  const sampleDef: PreviewDefinition = {
    functionName: 'GreetingPreview',
    packageName: 'com.compositor.sample',
    parameters: {},
    line: 10,
    filePath: 'Greeting.kt',
  };

  beforeEach(() => {
    registry = new PreviewRegistry();
  });

  it('manages queue and executes renders sequentially', async () => {
    const [item1] = registry.updateFilePreviews(
      'F1.kt',
      [{ ...sampleDef, functionName: 'Preview1' }],
      'sample-app'
    );
    const [item2] = registry.updateFilePreviews(
      'F2.kt',
      [{ ...sampleDef, functionName: 'Preview2' }],
      'sample-app'
    );

    const executionLog: string[] = [];
    const dispatcher = new RenderDispatcher(registry, {
      projectRoot: process.cwd(),
      customRunner: async (preview) => {
        executionLog.push(`start:${preview.id}`);
        await new Promise((resolve) => setTimeout(resolve, 50));
        executionLog.push(`done:${preview.id}`);
        return { success: true, imagePath: `/out/${preview.id}.png` };
      },
    });

    dispatcher.enqueue(item1.id);
    dispatcher.enqueue(item2.id);

    await new Promise((resolve) => setTimeout(resolve, 200));

    expect(executionLog).toEqual([
      `start:${item1.id}`,
      `done:${item1.id}`,
      `start:${item2.id}`,
      `done:${item2.id}`,
    ]);

    expect(registry.getPreview(item1.id)?.status).toBe('Rendered');
    expect(registry.getPreview(item2.id)?.status).toBe('Rendered');
  });

  it('handles render failure and records error details', async () => {
    const [item] = registry.updateFilePreviews(
      'F3.kt',
      [{ ...sampleDef, functionName: 'FailingPreview' }],
      'sample-app'
    );

    const dispatcher = new RenderDispatcher(registry, {
      projectRoot: process.cwd(),
      customRunner: async () => {
        return { success: false, error: 'Compilation error at line 42: Unresolved reference' };
      },
    });

    dispatcher.enqueue(item.id);

    await new Promise((resolve) => setTimeout(resolve, 100));

    const updated = registry.getPreview(item.id);
    expect(updated?.status).toBe('Error');
    expect(updated?.errorDetails).toContain('Compilation error at line 42');
  });

  it('triggers onRenderStarted and onRenderCompleted callbacks', async () => {
    const [item] = registry.updateFilePreviews(
      'F4.kt',
      [{ ...sampleDef, functionName: 'CallbackPreview' }],
      'sample-app'
    );

    let started = false;
    let completed = false;

    const dispatcher = new RenderDispatcher(registry, {
      projectRoot: process.cwd(),
      customRunner: async () => {
        return { success: true, imagePath: '/out/preview.png' };
      },
    });

    dispatcher.onRenderStarted((p) => {
      if (p.id === item.id) started = true;
    });

    dispatcher.onRenderCompleted((res) => {
      if (res.previewId === item.id) completed = true;
    });

    dispatcher.enqueue(item.id);

    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(started).toBe(true);
    expect(completed).toBe(true);
  });
});
