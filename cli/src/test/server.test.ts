import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { WebSocket } from 'ws';
import { createPreviewServer } from '../server.js';
import { DaemonStatus, PreviewCatalog, PreviewItem } from '../models/preview.js';

const TEST_HTTP_PORT = 3201;
const TEST_WS_PORT = 3202;

describe('Compositor Daemon API Suite', () => {
  let serverInstance: ReturnType<typeof createPreviewServer>;
  let discoveredPreviewId = '';

  beforeAll(async () => {
    serverInstance = createPreviewServer({
      httpPort: TEST_HTTP_PORT,
      wsPort: TEST_WS_PORT,
      enableWatcher: false, // Disable fs watcher in test to keep harness clean
      customRunner: async (preview) => {
        return {
          success: true,
          imagePath: serverInstance.previewPath,
        };
      },
    });

    await new Promise<void>((resolve) => {
      serverInstance.server.listen(TEST_HTTP_PORT, () => resolve());
    });
  });

  afterAll(async () => {
    await new Promise<void>((resolve) => {
      serverInstance.close(() => resolve());
    });
  });

  it('GET /api/daemon/status returns daemon health and metrics', async () => {
    const res = await fetch(`http://localhost:${TEST_HTTP_PORT}/api/daemon/status`);
    expect(res.status).toBe(200);
    const data = (await res.json()) as DaemonStatus;

    expect(data.status).toBe('running');
    expect(data.projectRoot).toBeDefined();
    expect(data.memoryUsage).toBeDefined();
    expect(data.totalPreviews).toBeGreaterThanOrEqual(1);
  });

  it('GET /api/status returns legacy status ok', async () => {
    const res = await fetch(`http://localhost:${TEST_HTTP_PORT}/api/status`);
    expect(res.status).toBe(200);
    const data = (await res.json()) as { status: string; previewExists: boolean };
    expect(data.status).toBe('ok');
    expect(data.previewExists).toBe(true);
  });

  it('GET /api/previews returns discovered previews catalog', async () => {
    const res = await fetch(`http://localhost:${TEST_HTTP_PORT}/api/previews`);
    expect(res.status).toBe(200);
    const catalog = (await res.json()) as PreviewCatalog;

    expect(catalog.totalCount).toBeGreaterThanOrEqual(1);
    const previewIds = Object.keys(catalog.previews);
    expect(previewIds.length).toBeGreaterThanOrEqual(1);

    discoveredPreviewId = previewIds[0];
    expect(discoveredPreviewId).toContain('GreetingPreview');
  });

  it('GET /api/previews/:id returns metadata for specific preview', async () => {
    const encoded = encodeURIComponent(discoveredPreviewId);
    const res = await fetch(`http://localhost:${TEST_HTTP_PORT}/api/previews/${encoded}`);
    expect(res.status).toBe(200);

    const item = (await res.json()) as PreviewItem;
    expect(item.id).toBe(discoveredPreviewId);
    expect(item.definition.functionName).toBe('GreetingPreview');
    expect(item.definition.packageName).toBe('com.compositor.sample');
  });

  it('GET /api/previews/:id/image streams PNG bitmap', async () => {
    const encoded = encodeURIComponent(discoveredPreviewId);
    const res = await fetch(`http://localhost:${TEST_HTTP_PORT}/api/previews/${encoded}/image`);
    expect(res.status).toBe(200);
    expect(res.headers.get('content-type')).toBe('image/png');

    const buffer = await res.arrayBuffer();
    expect(buffer.byteLength).toBeGreaterThan(1000);
  });

  it('POST /api/previews/:id/render enqueues targeted render', async () => {
    const encoded = encodeURIComponent(discoveredPreviewId);
    const res = await fetch(`http://localhost:${TEST_HTTP_PORT}/api/previews/${encoded}/render`, {
      method: 'POST',
    });
    expect(res.status).toBe(202);

    const data = (await res.json()) as { success: boolean; previewId: string };
    expect(data.success).toBe(true);
    expect(data.previewId).toBe(discoveredPreviewId);
  });

  it('connects to WebSocket server and receives broadcasts', async () => {
    const ws = new WebSocket(`ws://localhost:${TEST_WS_PORT}`);
    const receivedMessages: { event: string; payload: unknown }[] = [];

    await new Promise<void>((resolve) => {
      ws.on('open', () => resolve());
    });

    ws.on('message', (data) => {
      receivedMessages.push(JSON.parse(data.toString()));
    });

    serverInstance.broadcast('PREVIEW_UPDATED', {
      previewId: discoveredPreviewId,
      url: `/api/previews/${encodeURIComponent(discoveredPreviewId)}/image`,
      timestamp: Date.now(),
    });

    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(receivedMessages).toHaveLength(1);
    expect(receivedMessages[0].event).toBe('PREVIEW_UPDATED');

    ws.close();
  });
});
