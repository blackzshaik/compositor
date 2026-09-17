import { describe, it, expect, vi } from 'vitest';
import { DaemonClient, CompositorDaemonError } from '../client/daemon-client.js';
import { DaemonStatus, LayoutHierarchy, PreviewCatalog, PreviewItem } from '../types.js';

const mockCatalog: PreviewCatalog = {
  totalCount: 2,
  byModule: { 'sample-app': ['GreetingPreview', 'ButtonPreview'] },
  byFile: { 'Greeting.kt': ['GreetingPreview'] },
  byGroup: { Default: ['GreetingPreview'] },
  previews: {
    'com.compositor.sample.GreetingPreview': {
      id: 'com.compositor.sample.GreetingPreview',
      module: 'sample-app',
      status: 'Rendered',
      definition: {
        functionName: 'GreetingPreview',
        packageName: 'com.compositor.sample',
        filePath: '/samples/sample-app/src/main/java/Greeting.kt',
        line: 42,
        parameters: { name: 'Greeting Preview', group: 'Default' },
      },
      imagePath: '/path/to/image.png',
      imageUrl: '/api/previews/com.compositor.sample.GreetingPreview/image',
    },
    'com.compositor.sample.ButtonPreview': {
      id: 'com.compositor.sample.ButtonPreview',
      module: 'sample-app',
      status: 'Pending',
      definition: {
        functionName: 'ButtonPreview',
        packageName: 'com.compositor.sample',
        filePath: '/samples/sample-app/src/main/java/Button.kt',
        line: 15,
        parameters: { name: 'Button Component' },
      },
    },
  },
};

describe('DaemonClient', () => {
  it('detects online daemon status and returns health metrics', async () => {
    const mockStatus: DaemonStatus = {
      status: 'running',
      projectRoot: '/test/root',
      watcherActive: true,
      totalPreviews: 2,
      uptimeSeconds: 120,
      memoryUsage: { rss: 10, heapTotal: 20, heapUsed: 5, external: 1 },
    };

    const customFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockStatus,
    });

    const client = new DaemonClient({
      baseUrl: 'http://localhost:3001',
      customFetch: customFetch as unknown as typeof fetch,
    });

    const isOnline = await client.isOnline();
    expect(isOnline).toBe(true);

    const status = await client.getStatus();
    expect(status.status).toBe('running');
    expect(status.totalPreviews).toBe(2);
  });

  it('handles offline daemon with actionable error instructions', async () => {
    const customFetch = vi.fn().mockRejectedValue(new Error('connect ECONNREFUSED 127.0.0.1:3001'));

    const client = new DaemonClient({
      baseUrl: 'http://localhost:3001',
      customFetch: customFetch as unknown as typeof fetch,
    });

    const isOnline = await client.isOnline();
    expect(isOnline).toBe(false);

    await expect(client.getStatus()).rejects.toThrow(CompositorDaemonError);
    await expect(client.getStatus()).rejects.toThrow(/npm run start/);
  });

  it('lists and filters previews by query string', async () => {
    const customFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockCatalog,
    });

    const client = new DaemonClient({
      customFetch: customFetch as unknown as typeof fetch,
    });

    // Unfiltered
    const all = await client.listPreviews();
    expect(all).toHaveLength(2);

    // Filter by function name
    const greeting = await client.listPreviews('greeting');
    expect(greeting).toHaveLength(1);
    expect(greeting[0].definition.functionName).toBe('GreetingPreview');

    // Filter by group name
    const defaultGroup = await client.listPreviews('default');
    expect(defaultGroup).toHaveLength(1);

    // Filter matching none
    const none = await client.listPreviews('nonexistent');
    expect(none).toHaveLength(0);
  });

  it('polls preview during render until status is Rendered', async () => {
    let pollCount = 0;
    const customFetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          status: 202,
          json: async () => ({ success: true, previewId: 'com.compositor.sample.GreetingPreview' }),
        });
      }

      pollCount++;
      const item: PreviewItem = {
        ...mockCatalog.previews['com.compositor.sample.GreetingPreview'],
        status: pollCount < 2 ? 'Rendering' : 'Rendered',
        durationMs: 150,
      };

      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => item,
      });
    });

    const client = new DaemonClient({
      customFetch: customFetch as unknown as typeof fetch,
    });

    const result = await client.renderPreview('com.compositor.sample.GreetingPreview', {
      pollIntervalMs: 10,
      timeoutMs: 1000,
    });

    expect(result.success).toBe(true);
    expect(result.status).toBe('Rendered');
    expect(result.durationMs).toBe(150);
  });

  it('returns failure details if render fails with Error status', async () => {
    const customFetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          status: 202,
          json: async () => ({ success: true, previewId: 'com.compositor.sample.ButtonPreview' }),
        });
      }

      const item: PreviewItem = {
        ...mockCatalog.previews['com.compositor.sample.ButtonPreview'],
        status: 'Error',
        errorDetails: 'Unresolved reference: MaterialTheme',
      };

      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => item,
      });
    });

    const client = new DaemonClient({
      customFetch: customFetch as unknown as typeof fetch,
    });

    const result = await client.renderPreview('com.compositor.sample.ButtonPreview', {
      pollIntervalMs: 10,
      timeoutMs: 1000,
    });

    expect(result.success).toBe(false);
    expect(result.status).toBe('Error');
    expect(result.error).toContain('Unresolved reference: MaterialTheme');
  });

  it('retrieves image bitmap and converts to base64', async () => {
    const fakePngData = Buffer.from('FAKE_PNG_BITMAP_CONTENT');

    const customFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      arrayBuffer: async () =>
        fakePngData.buffer.slice(
          fakePngData.byteOffset,
          fakePngData.byteOffset + fakePngData.byteLength
        ),
    });

    const client = new DaemonClient({
      customFetch: customFetch as unknown as typeof fetch,
    });

    const buffer = await client.getPreviewImageBuffer('com.compositor.sample.GreetingPreview');
    expect(buffer.toString()).toBe('FAKE_PNG_BITMAP_CONTENT');

    const b64 = await client.getPreviewImageBase64('com.compositor.sample.GreetingPreview');
    expect(b64).toBe(fakePngData.toString('base64'));
  });

  it('fetches layout hierarchy tree', async () => {
    const mockHierarchy: LayoutHierarchy = {
      density: 2.75,
      viewWidth: 360,
      viewHeight: 780,
      root: {
        id: 'root-node',
        name: 'GreetingPreview',
        qualifiedName: 'com.compositor.sample.GreetingPreview',
        bounds: { left: 0, top: 0, right: 360, bottom: 780 },
      },
    };

    const customFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockHierarchy,
    });

    const client = new DaemonClient({
      customFetch: customFetch as unknown as typeof fetch,
    });

    const hierarchy = await client.getLayoutHierarchy('com.compositor.sample.GreetingPreview');
    expect(hierarchy.density).toBe(2.75);
    expect(hierarchy.root.name).toBe('GreetingPreview');
  });
});
