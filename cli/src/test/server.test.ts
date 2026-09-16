import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import http from 'node:http';
import { WebSocket } from 'ws';
import { createPreviewServer } from '../server.js';

const TEST_HTTP_PORT = 3101;
const TEST_WS_PORT = 3102;

describe('Compositor Preview Server', () => {
  let serverInstance: ReturnType<typeof createPreviewServer>;

  beforeAll(async () => {
    serverInstance = createPreviewServer(undefined, TEST_HTTP_PORT, TEST_WS_PORT);
    await new Promise<void>((resolve) => {
      serverInstance.server.listen(TEST_HTTP_PORT, () => resolve());
    });
  });

  afterAll(async () => {
    await new Promise<void>((resolve) => {
      serverInstance.close(() => resolve());
    });
  });

  it('GET /api/status returns HTTP 200 with JSON status', async () => {
    const res = await fetch(`http://localhost:${TEST_HTTP_PORT}/api/status`);
    expect(res.status).toBe(200);
    const data = (await res.json()) as { status: string; previewExists: boolean };
    expect(data.status).toBe('ok');
    expect(data.previewExists).toBe(true);
  });

  it('GET /api/preview/latest.png returns HTTP 200 with image/png', async () => {
    const res = await fetch(`http://localhost:${TEST_HTTP_PORT}/api/preview/latest.png`);
    expect(res.status).toBe(200);
    expect(res.headers.get('content-type')).toBe('image/png');
    const buffer = await res.arrayBuffer();
    expect(buffer.byteLength).toBeGreaterThan(5000);
  });

  it('connects to WebSocket server cleanly', async () => {
    const ws = new WebSocket(`ws://localhost:${TEST_WS_PORT}`);
    const opened = await new Promise<boolean>((resolve) => {
      ws.on('open', () => resolve(true));
      ws.on('error', () => resolve(false));
    });
    expect(opened).toBe(true);
    ws.close();
  });
});
