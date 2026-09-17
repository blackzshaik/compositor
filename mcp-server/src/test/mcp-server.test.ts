import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { PNG } from 'pngjs';
import { createCompositorMcpServer } from '../server.js';
import { DaemonClient } from '../client/daemon-client.js';
import { BaselineManager } from '../diff/visual-diff.js';
import { PreviewCatalog, LayoutHierarchy } from '../types.js';

function createSolidPng(width: number, height: number, r: number, g: number, b: number): Buffer {
  const png = new PNG({ width, height });
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const idx = (y * width + x) * 4;
      png.data[idx] = r;
      png.data[idx + 1] = g;
      png.data[idx + 2] = b;
      png.data[idx + 3] = 255;
    }
  }
  return PNG.sync.write(png);
}

describe('Compositor MCP Server End-to-End Suite', () => {
  let tempDir: string;
  let client: Client;
  let baselineManager: BaselineManager;
  let currentImageBuffer: Buffer;

  const mockCatalog: PreviewCatalog = {
    totalCount: 2,
    byModule: { 'sample-app': ['GreetingPreview', 'FailingPreview'] },
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
        imagePath: '/path/to/greeting.png',
        imageUrl: '/api/previews/com.compositor.sample.GreetingPreview/image',
      },
      'com.compositor.sample.FailingPreview': {
        id: 'com.compositor.sample.FailingPreview',
        module: 'sample-app',
        status: 'Error',
        definition: {
          functionName: 'FailingPreview',
          packageName: 'com.compositor.sample',
          filePath: '/samples/sample-app/src/main/java/Failing.kt',
          line: 12,
          parameters: { name: 'Failing Preview' },
        },
        errorDetails: 'Unresolved reference: NonExistentComposable',
      },
    },
  };

  const mockHierarchy: LayoutHierarchy = {
    density: 2.75,
    viewWidth: 360,
    viewHeight: 780,
    root: {
      id: 'root-greeting',
      name: 'GreetingPreview',
      qualifiedName: 'com.compositor.sample.GreetingPreview',
      bounds: { left: 0, top: 0, right: 360, bottom: 780 },
    },
  };

  beforeAll(async () => {
    tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'compositor-mcp-e2e-'));
    baselineManager = new BaselineManager(tempDir);
    currentImageBuffer = createSolidPng(50, 50, 30, 60, 90);

    const mockFetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
      const urlStr = typeof input === 'string' ? input : input.toString();

      if (urlStr.endsWith('/api/daemon/status')) {
        return new Response(
          JSON.stringify({
            status: 'running',
            projectRoot: '/test',
            watcherActive: true,
            totalPreviews: 2,
            uptimeSeconds: 50,
            memoryUsage: { rss: 1, heapTotal: 2, heapUsed: 1, external: 0 },
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        );
      }

      if (urlStr.endsWith('/api/previews')) {
        return new Response(JSON.stringify(mockCatalog), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        });
      }

      if (urlStr.includes('/api/previews/') && urlStr.endsWith('/image')) {
        return new Response(new Uint8Array(currentImageBuffer), {
          status: 200,
          headers: { 'Content-Type': 'image/png' },
        });
      }

      if (urlStr.includes('/api/previews/') && (urlStr.endsWith('/hierarchy') || urlStr.endsWith('/hierarchy.json'))) {
        return new Response(JSON.stringify(mockHierarchy), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        });
      }

      if (init?.method === 'POST' && urlStr.includes('/render')) {
        return new Response(JSON.stringify({ success: true }), {
          status: 202,
          headers: { 'Content-Type': 'application/json' },
        });
      }

      if (urlStr.includes('GreetingPreview')) {
        return new Response(
          JSON.stringify(mockCatalog.previews['com.compositor.sample.GreetingPreview']),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        );
      }

      if (urlStr.includes('FailingPreview')) {
        return new Response(
          JSON.stringify(mockCatalog.previews['com.compositor.sample.FailingPreview']),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        );
      }

      return new Response(JSON.stringify({ error: 'Not found' }), { status: 404 });
    };

    const daemonClient = new DaemonClient({
      baseUrl: 'http://localhost:3001',
      customFetch: mockFetch as unknown as typeof fetch,
    });

    const instance = createCompositorMcpServer({
      daemonClient,
      baselineManager,
    });

    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    await instance.server.connect(serverTransport);

    client = new Client({ name: 'test-agent', version: '1.0.0' });
    await client.connect(clientTransport);
  });

  afterAll(async () => {
    try {
      await client.close();
    } catch {
      // ignore
    }
    if (fs.existsSync(tempDir)) {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('advertises all Compositor MCP tools with valid schemas', async () => {
    const { tools } = await client.listTools();
    const toolNames = tools.map((t) => t.name);

    expect(toolNames).toContain('list_previews');
    expect(toolNames).toContain('render_preview');
    expect(toolNames).toContain('get_preview_image');
    expect(toolNames).toContain('inspect_layout_tree');
    expect(toolNames).toContain('compare_previews');
  });

  it('executes list_previews without filter and with filter', async () => {
    // Unfiltered
    const res1 = await client.callTool({
      name: 'list_previews',
      arguments: {},
    });
    expect(res1.isError).toBeFalsy();
    const parsed1 = JSON.parse((res1.content as { type: 'text'; text: string }[])[0].text);
    expect(parsed1.totalCount).toBe(2);

    // Filtered by function name
    const res2 = await client.callTool({
      name: 'list_previews',
      arguments: { filter: 'Greeting' },
    });
    const parsed2 = JSON.parse((res2.content as { type: 'text'; text: string }[])[0].text);
    expect(parsed2.totalCount).toBe(1);
    expect(parsed2.previews[0].functionName).toBe('GreetingPreview');
  });

  it('executes render_preview successfully and handles error states', async () => {
    // Successful render
    const successRes = await client.callTool({
      name: 'render_preview',
      arguments: {
        previewId: 'com.compositor.sample.GreetingPreview',
        theme: 'dark',
      },
    });
    expect(successRes.isError).toBeFalsy();
    const successData = JSON.parse((successRes.content as { type: 'text'; text: string }[])[0].text);
    expect(successData.success).toBe(true);

    // Failing render
    const failRes = await client.callTool({
      name: 'render_preview',
      arguments: {
        previewId: 'com.compositor.sample.FailingPreview',
      },
    });
    expect(failRes.isError).toBe(true);
    const failText = (failRes.content as { type: 'text'; text: string }[])[0].text;
    expect(failText).toContain('Unresolved reference: NonExistentComposable');
  });

  it('executes get_preview_image and returns native MCP image content block', async () => {
    const res = await client.callTool({
      name: 'get_preview_image',
      arguments: {
        previewId: 'com.compositor.sample.GreetingPreview',
      },
    });

    expect(res.isError).toBeFalsy();
    const contentList = (res.content ?? []) as Array<{
      type: string;
      data?: string;
      mimeType?: string;
      text?: string;
    }>;
    const imageBlock = contentList.find(
      (c: { type: string }) => c.type === 'image'
    ) as { type: 'image'; data: string; mimeType: string };

    expect(imageBlock).toBeDefined();
    expect(imageBlock.mimeType).toBe('image/png');
    expect(imageBlock.data).toBe(currentImageBuffer.toString('base64'));
  });

  it('executes inspect_layout_tree and returns structured node hierarchy', async () => {
    const res = await client.callTool({
      name: 'inspect_layout_tree',
      arguments: {
        previewId: 'com.compositor.sample.GreetingPreview',
      },
    });

    expect(res.isError).toBeFalsy();
    const textBlock = (res.content as { type: 'text'; text: string }[])[0].text;
    const hierarchy: LayoutHierarchy = JSON.parse(textBlock);

    expect(hierarchy.density).toBe(2.75);
    expect(hierarchy.viewWidth).toBe(360);
    expect(hierarchy.root.name).toBe('GreetingPreview');
  });

  it('executes compare_previews: establishes baseline, detects identical, and detects diffs', async () => {
    const previewId = 'com.compositor.sample.GreetingPreview';

    // 1. Initial run: No baseline exists -> saves current as initial baseline
    const initRes = await client.callTool({
      name: 'compare_previews',
      arguments: { previewId },
    });
    expect(initRes.isError).toBeFalsy();
    const initData = JSON.parse((initRes.content as { type: 'text'; text: string }[])[0].text);
    expect(initData.baselineEstablished).toBe(true);
    expect(initData.differencePercentage).toBe(0);

    // 2. Second run: Baseline exists, current image unchanged -> 0% diff
    const matchRes = await client.callTool({
      name: 'compare_previews',
      arguments: { previewId },
    });
    expect(matchRes.isError).toBeFalsy();
    const matchData = JSON.parse((matchRes.content as { type: 'text'; text: string }[])[0].text);
    expect(matchData.hasVisualDifferences).toBe(false);
    expect(matchData.differencePercentage).toBe(0);

    // 3. Third run: Change current image -> detects visual differences and returns magenta diff overlay
    currentImageBuffer = createSolidPng(50, 50, 255, 0, 0); // Changed to red
    const diffRes = await client.callTool({
      name: 'compare_previews',
      arguments: { previewId },
    });

    expect(diffRes.isError).toBeFalsy();
    const diffData = JSON.parse((diffRes.content as { type: 'text'; text: string }[])[0].text);
    expect(diffData.hasVisualDifferences).toBe(true);
    expect(diffData.differencePercentage).toBeGreaterThan(0);

    const diffContentList = (diffRes.content ?? []) as Array<{ type: string }>;
    const diffOverlay = diffContentList.find((c: { type: string }) => c.type === 'image');
    expect(diffOverlay).toBeDefined();
  });
});
