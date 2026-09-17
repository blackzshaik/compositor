import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { WebSocketServer, WebSocket } from 'ws';
import { DaemonStatus, PreviewItem } from './models/preview.js';
import { parseKotlinPreviews } from './parser/preview-ast-parser.js';
import { PreviewRegistry } from './registry/preview-registry.js';
import { FileWatcher, safeReadFile } from './watcher/file-watcher.js';
import { RenderDispatcher, RenderRunner } from './renderer/render-dispatcher.js';

export const HTTP_PORT = 3001;
export const WS_PORT = 3002;

export function resolveProjectRoot(): string {
  let cur = process.cwd();
  for (let i = 0; i < 5; i++) {
    if (fs.existsSync(path.join(cur, '.compositor')) || fs.existsSync(path.join(cur, 'settings.gradle.kts'))) {
      return cur;
    }
    const parent = path.dirname(cur);
    if (parent === cur) break;
    cur = parent;
  }
  return process.cwd();
}

export interface ServerOptions {
  customPreviewFile?: string;
  httpPort?: number;
  wsPort?: number;
  customRunner?: RenderRunner;
  enableWatcher?: boolean;
}

export function createPreviewServer(options: ServerOptions = {}) {
  const httpPort = options.httpPort ?? HTTP_PORT;
  const wsPort = options.wsPort ?? WS_PORT;
  const projectRoot = resolveProjectRoot();

  const previewPath = options.customPreviewFile ?? path.join(projectRoot, '.compositor', 'latest_preview.png');
  const previewsJsonPath = path.join(projectRoot, '.compositor', 'previews.json');
  const startTime = Date.now();

  // 1. Initialize Registry & Dispatcher
  const registry = new PreviewRegistry(previewsJsonPath);
  const dispatcher = new RenderDispatcher(registry, {
    projectRoot,
    customRunner: options.customRunner,
  });

  // 2. Set up WebSocket Server
  const wss = new WebSocketServer({ port: wsPort });

  function broadcast(event: string, payload: Record<string, unknown>) {
    const data = JSON.stringify({ event, payload });
    for (const client of wss.clients) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data);
      }
    }
  }

  // Hook render lifecycle events to WebSocket broadcasts
  dispatcher.onRenderStarted((preview) => {
    broadcast('PREVIEW_RENDER_STARTED', {
      previewId: preview.id,
      timestamp: Date.now(),
    });
  });

  dispatcher.onRenderCompleted((result) => {
    if (result.success) {
      broadcast('PREVIEW_UPDATED', {
        previewId: result.previewId,
        url: result.imageUrl ?? `/api/previews/${encodeURIComponent(result.previewId)}/image?v=${Date.now()}`,
        timestamp: Date.now(),
      });
    } else {
      broadcast('RENDER_ERROR', {
        previewId: result.previewId,
        error: result.error ?? 'Unknown rendering failure',
        timestamp: Date.now(),
      });
    }
  });

  // 3. Scan initial project Kotlin files and populate Registry
  function scanSourceFiles(dir: string): void {
    if (!fs.existsSync(dir)) return;
    try {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      for (const entry of entries) {
        if (['build', '.gradle', '.compositor', '.git', 'node_modules'].includes(entry.name)) {
          continue;
        }
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          scanSourceFiles(fullPath);
        } else if (entry.isFile() && entry.name.endsWith('.kt')) {
          try {
            const content = fs.readFileSync(fullPath, 'utf-8');
            const defs = parseKotlinPreviews(content, fullPath);
            if (defs.length > 0) {
              const moduleName = fullPath.includes('samples') ? 'sample-app' : 'app';
              registry.updateFilePreviews(fullPath, defs, moduleName);
            }
          } catch {
            // Graceful handling
          }
        }
      }
    } catch {
      // Graceful handling
    }
  }

  scanSourceFiles(projectRoot);

  // If latest_preview.png exists and we have previews, attach imagePath
  if (fs.existsSync(previewPath)) {
    const all = registry.getAllPreviews();
    for (const item of all) {
      if (!item.imagePath || !fs.existsSync(item.imagePath)) {
        registry.updateStatus(item.id, 'Rendered', {
          imagePath: previewPath,
          imageUrl: `/api/previews/${encodeURIComponent(item.id)}/image`,
        });
      }
    }
  }

  // 4. Set up File Watcher
  const samplesDir = path.join(projectRoot, 'samples');
  const watchTarget = fs.existsSync(samplesDir) ? samplesDir : projectRoot;
  let watcher: FileWatcher | null = null;

  if (options.enableWatcher !== false) {
    watcher = new FileWatcher(watchTarget, { debounceMs: 250 });
    watcher.onFileChange(async (event) => {
      if (event.type === 'deleted') {
        registry.removeFile(event.filePath);
        return;
      }

      try {
        const content = await safeReadFile(event.filePath);
        const defs = parseKotlinPreviews(content, event.filePath);
        const moduleName = event.filePath.includes('samples') ? 'sample-app' : 'app';
        const items = registry.updateFilePreviews(event.filePath, defs, moduleName);

        for (const item of items) {
          broadcast('PREVIEW_REGISTERED', { preview: item });
          dispatcher.enqueue(item.id);
        }
      } catch (err) {
        console.error(`[Compositor Daemon] Error processing changed file ${event.filePath}:`, err);
      }
    });
    watcher.start();
  }

  // 5. Create HTTP Server & Route Handlers
  const server = http.createServer((req, res) => {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Cache-Control');

    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    const url = new URL(req.url ?? '/', `http://localhost:${httpPort}`);
    const pathname = url.pathname;

    // GET /api/daemon/status
    if (req.method === 'GET' && pathname === '/api/daemon/status') {
      const daemonStatus: DaemonStatus = {
        status: 'running',
        projectRoot,
        watcherActive: watcher?.getStatus().isRunning ?? false,
        totalPreviews: registry.getAllPreviews().length,
        uptimeSeconds: Math.floor((Date.now() - startTime) / 1000),
        memoryUsage: process.memoryUsage(),
      };
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(daemonStatus, null, 2));
      return;
    }

    // GET /api/status (Legacy backwards-compatibility)
    if (req.method === 'GET' && pathname === '/api/status') {
      const exists = fs.existsSync(previewPath);
      const mtime = exists ? fs.statSync(previewPath).mtimeMs : 0;
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'ok', previewExists: exists, lastModified: mtime }));
      return;
    }

    // GET /api/previews
    if (req.method === 'GET' && pathname === '/api/previews') {
      const catalog = registry.getCatalog();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(catalog, null, 2));
      return;
    }

    // Single Preview Routes: /api/previews/:id/...
    const previewMatch = pathname.match(/^\/api\/previews\/([^/]+)(?:\/(.*))?$/);
    if (previewMatch) {
      const rawId = previewMatch[1];
      const subAction = previewMatch[2];
      const previewId = decodeURIComponent(rawId);

      const preview = registry.getPreview(previewId);
      if (!preview) {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: `Preview with ID "${previewId}" not found` }));
        return;
      }

      // GET /api/previews/:id/image
      if (req.method === 'GET' && subAction === 'image') {
        const imagePath = preview.imagePath ?? previewPath;
        if (!fs.existsSync(imagePath)) {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Image not rendered yet for this preview' }));
          return;
        }

        res.writeHead(200, {
          'Content-Type': 'image/png',
          'Cache-Control': 'no-cache, no-store, must-revalidate',
        });
        fs.createReadStream(imagePath).pipe(res);
        return;
      }

      // POST /api/previews/:id/render
      if (req.method === 'POST' && subAction === 'render') {
        dispatcher.enqueue(previewId);
        res.writeHead(202, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          previewId,
          status: 'Rendering',
          message: 'Targeted preview render enqueued',
        }));
        return;
      }

      // GET /api/previews/:id/hierarchy or hierarchy.json
      if (req.method === 'GET' && (subAction === 'hierarchy' || subAction === 'hierarchy.json')) {
        const sanitized = RenderDispatcher.sanitizeId(preview.id);
        const hierarchyFile = path.join(projectRoot, '.compositor', 'previews', `${sanitized}.hierarchy.json`);
        if (fs.existsSync(hierarchyFile)) {
          try {
            const data = fs.readFileSync(hierarchyFile, 'utf-8');
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(data);
            return;
          } catch {
            // Fall through to synthesize
          }
        }

        // Generate synthetic hierarchy from preview definition
        const fnName = preview.definition.functionName;
        const pkg = preview.definition.packageName;
        const width = preview.definition.parameters.widthDp ?? 360;
        const height = preview.definition.parameters.heightDp ?? 780;
        const synthHierarchy = {
          density: 2.75,
          viewWidth: width,
          viewHeight: height,
          root: {
            id: `root-${sanitized}`,
            name: fnName,
            qualifiedName: `${pkg}.${fnName}`,
            bounds: { left: 0, top: 0, right: width, bottom: height },
            dpBounds: { left: 0, top: 0, right: width, bottom: height },
            padding: { left: 16, top: 16, right: 16, bottom: 16 },
            children: [
              {
                id: `node-${sanitized}-content`,
                name: 'ComposableContent',
                qualifiedName: `${pkg}.${fnName}`,
                bounds: { left: 16, top: 16, right: Math.max(16, width - 16), bottom: Math.max(16, height - 16) },
                dpBounds: { left: 16, top: 16, right: Math.max(16, width - 16), bottom: Math.max(16, height - 16) },
                semantics: {
                  role: 'Component',
                  functionName: fnName,
                  packageName: pkg,
                },
              },
            ],
          },
        };
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(synthHierarchy, null, 2));
        return;
      }

      // GET /api/previews/:id
      if (req.method === 'GET' && !subAction) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(preview, null, 2));
        return;
      }
    }

    // GET /api/preview/latest.png (Legacy)
    if (req.method === 'GET' && pathname.startsWith('/api/preview/latest.png')) {
      if (!fs.existsSync(previewPath)) {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'No preview rendered yet' }));
        return;
      }
      res.writeHead(200, {
        'Content-Type': 'image/png',
        'Cache-Control': 'no-cache, no-store, must-revalidate',
      });
      fs.createReadStream(previewPath).pipe(res);
      return;
    }

    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not found' }));
  });

  function close(callback?: () => void) {
    if (watcher) {
      watcher.stop();
    }
    wss.close(() => {
      server.close(callback);
    });
  }

  return {
    server,
    wss,
    registry,
    dispatcher,
    watcher,
    broadcast,
    close,
    previewPath,
  };
}

// Auto-run if executed directly
if (process.argv[1] && import.meta.url.endsWith(path.basename(process.argv[1]))) {
  const { server } = createPreviewServer();
  server.listen(HTTP_PORT, () => {
    console.info(`[Compositor Daemon] HTTP listening on http://localhost:${HTTP_PORT}`);
    console.info(`[Compositor Daemon] WebSocket listening on ws://localhost:${WS_PORT}`);
  });
}
