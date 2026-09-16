# Step 03: Local Preview Server (HTTP & WebSocket)

*Status*: **Complete**  
*Assigned Agent*: Any AI Agent invoking "Build from brain"  
*Estimated Duration*: 4 minutes

---

## 1. Objective

Build a lightweight local HTTP and WebSocket preview server inside `cli/` that serves the rendered `.compositor/latest_preview.png` image and immediately pushes hot-reload notifications to connected clients whenever the preview image is regenerated.

---

## 2. Target Files & Structure

```
cli/
├── src/
│   ├── server.ts         # HTTP & WebSocket daemon implementation
│   └── test/
│       └── server.test.ts # Automated smoke test verifying endpoints
├── package.json
└── tsconfig.json
```

---

## 3. Agent Action Prompt (Step-by-Step Instructions)

### 3.1. Create `cli/package.json`
```json
{
  "name": "@compositor/cli",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "start": "tsx src/server.ts",
    "test": "vitest run"
  },
  "dependencies": {
    "ws": "^8.18.0"
  },
  "devDependencies": {
    "@types/node": "^24.0.0",
    "@types/ws": "^8.5.14",
    "tsx": "^4.19.2",
    "typescript": "^5.7.3",
    "vitest": "^3.0.5"
  }
}
```

### 3.2. Create `cli/tsconfig.json`
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "strict": true,
    "noImplicitAny": true,
    "skipLibCheck": true,
    "outDir": "./dist"
  },
  "include": ["src/**/*"]
}
```

### 3.3. Create `cli/src/server.ts`
Implement the server using Node's standard `http` module and `ws`:
```typescript
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { WebSocketServer, WebSocket } from 'ws';

export const HTTP_PORT = 3001;
export const WS_PORT = 3002;

const PROJECT_ROOT = path.resolve(process.cwd(), '..');
const PREVIEW_FILE = path.join(PROJECT_ROOT, '.compositor', 'latest_preview.png');

export function createPreviewServer() {
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
      const exists = fs.existsSync(PREVIEW_FILE);
      const mtime = exists ? fs.statSync(PREVIEW_FILE).mtimeMs : 0;
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'ok', previewExists: exists, lastModified: mtime }));
      return;
    }

    if (req.url?.startsWith('/api/preview/latest.png')) {
      if (!fs.existsSync(PREVIEW_FILE)) {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'No preview rendered yet' }));
        return;
      }
      const stream = fs.createReadStream(PREVIEW_FILE);
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

  const wss = new WebSocketServer({ port: WS_PORT });

  function broadcast(event: string, payload: Record<string, unknown>) {
    const data = JSON.stringify({ event, payload });
    for (const client of wss.clients) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data);
      }
    }
  }

  // Watch for image updates
  const compositorDir = path.dirname(PREVIEW_FILE);
  if (!fs.existsSync(compositorDir)) {
    fs.mkdirSync(compositorDir, { recursive: true });
  }

  let debounceTimer: NodeJS.Timeout | null = null;
  fs.watch(compositorDir, (eventType, filename) => {
    if (filename === 'latest_preview.png') {
      if (debounceTimer) clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        broadcast('PREVIEW_UPDATED', {
          url: `/api/preview/latest.png?v=${Date.now()}`,
          timestamp: Date.now(),
        });
      }, 100);
    }
  });

  return { server, wss, broadcast };
}

// Auto-run if executed directly
if (import.meta.url === `file://${process.argv[1].replace(/\\/g, '/')}`) {
  const { server } = createPreviewServer();
  server.listen(HTTP_PORT, () => {
    console.info(`[Compositor Daemon] HTTP listening on http://localhost:${HTTP_PORT}`);
    console.info(`[Compositor Daemon] WebSocket listening on ws://localhost:${WS_PORT}`);
  });
}
```

---

## 4. Automated Verification Gate

Create and run an automated integration test `cli/src/test/server.test.ts`:
```powershell
cd cli
npm install
npm test
```

### Acceptance Criteria:
* `GET /api/status` returns HTTP 200 with `{ status: "ok" }`.
* `GET /api/preview/latest.png` streams the image with `Content-Type: image/png`.
* WebSocket connection establishes cleanly at `ws://localhost:3002`.

---

## 5. Post-Execution Checklist
1. Commit changes: `feat: implement local HTTP and WebSocket preview server`.
2. Update [`brain/STATE.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/STATE.md): mark Step 03 complete, advance active step to Step 04.
3. Record outputs in the log below.

---

## 6. Execution Log (To be completed by executing agent)
* **Execution Date**: 2026-09-16
* **Executing Agent**: Gemini 3.8 Flash (High) / Antigravity
* **HTTP Port**: 3001
* **WebSocket Port**: 3002
* **Verification Status**: Pass (3/3 integration tests passed)
