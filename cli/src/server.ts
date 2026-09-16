import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { WebSocketServer, WebSocket } from 'ws';

export const HTTP_PORT = 3001;
export const WS_PORT = 3002;

function resolveProjectRoot(): string {
  if (fs.existsSync(path.join(process.cwd(), '.compositor'))) {
    return process.cwd();
  }
  return path.resolve(process.cwd(), '..');
}

export function createPreviewServer(customPreviewFile?: string, httpPort = HTTP_PORT, wsPort = WS_PORT) {
  const projectRoot = resolveProjectRoot();
  const previewPath = customPreviewFile ?? path.join(projectRoot, '.compositor', 'latest_preview.png');

  const server = http.createServer((req, res) => {
    // Enable CORS for local dev web viewer
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');

    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    if (req.url === '/api/status') {
      const exists = fs.existsSync(previewPath);
      const mtime = exists ? fs.statSync(previewPath).mtimeMs : 0;
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'ok', previewExists: exists, lastModified: mtime }));
      return;
    }

    if (req.url?.startsWith('/api/preview/latest.png')) {
      if (!fs.existsSync(previewPath)) {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'No preview rendered yet' }));
        return;
      }
      const stream = fs.createReadStream(previewPath);
      res.writeHead(200, {
        'Content-Type': 'image/png',
        'Cache-Control': 'no-cache, no-store, must-revalidate',
      });
      stream.pipe(res);
      return;
    }

    res.writeHead(404);
    res.end('Not found');
  });

  const wss = new WebSocketServer({ port: wsPort });

  function broadcast(event: string, payload: Record<string, unknown>) {
    const data = JSON.stringify({ event, payload });
    for (const client of wss.clients) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data);
      }
    }
  }

  // Watch for image updates
  const compositorDir = path.dirname(previewPath);
  if (!fs.existsSync(compositorDir)) {
    fs.mkdirSync(compositorDir, { recursive: true });
  }

  let debounceTimer: NodeJS.Timeout | null = null;
  const watcher = fs.watch(compositorDir, (_eventType, filename) => {
    if (filename === path.basename(previewPath)) {
      if (debounceTimer) clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        broadcast('PREVIEW_UPDATED', {
          url: `/api/preview/latest.png?v=${Date.now()}`,
          timestamp: Date.now(),
        });
      }, 100);
    }
  });

  function close(callback?: () => void) {
    if (debounceTimer) clearTimeout(debounceTimer);
    watcher.close();
    wss.close(() => {
      server.close(callback);
    });
  }

  return { server, wss, broadcast, close, previewPath };
}

// Auto-run if executed directly
if (process.argv[1] && import.meta.url.endsWith(path.basename(process.argv[1]))) {
  const { server } = createPreviewServer();
  server.listen(HTTP_PORT, () => {
    console.info(`[Compositor Daemon] HTTP listening on http://localhost:${HTTP_PORT}`);
    console.info(`[Compositor Daemon] WebSocket listening on ws://localhost:${WS_PORT}`);
  });
}
