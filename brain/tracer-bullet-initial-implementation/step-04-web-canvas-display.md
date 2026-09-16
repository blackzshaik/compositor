# Step 04: Web Canvas Display & Live Device Mockup

*Status*: **Ready for Execution** (Blocked by Step 03)  
*Assigned Agent*: Any AI Agent invoking "Build from brain"  
*Estimated Duration*: 4 minutes

---

## 1. Objective

Build the browser-based canvas in `web-viewer/` that connects to the local daemon over WebSocket, renders a realistic Android smartphone frame (Pixel mockup), and hot-updates the preview image automatically with zero manual page refreshes.

---

## 2. Target Files & Structure

```
web-viewer/
├── index.html
├── src/
│   ├── main.tsx
│   ├── App.tsx             # Main canvas with device frame and live preview
│   ├── index.css           # Tailwind styles & theme variables
│   └── components/
│       └── DeviceFrame.tsx # Smartphone bezel mockup with punch-hole & status bar
└── vite.config.ts
```

---

## 3. Agent Action Prompt (Step-by-Step Instructions)

### 3.1. Create `web-viewer/index.html`
```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Compositor — Live Compose Preview</title>
    <link rel="icon" type="image/svg+xml" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🎨</text></svg>" />
  </head>
  <body class="bg-neutral-950 text-neutral-100 min-h-screen antialiased selection:bg-purple-600 selection:text-white">
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

### 3.2. Create `web-viewer/src/index.css`
```css
@import "tailwindcss";

body {
  margin: 0;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}
```

### 3.3. Create `web-viewer/src/components/DeviceFrame.tsx`
```tsx
import React from 'react';

interface DeviceFrameProps {
  imageUrl: string;
  title: string;
  isLoading?: boolean;
}

export const DeviceFrame: React.FC<DeviceFrameProps> = ({ imageUrl, title, isLoading }) => {
  return (
    <div className="flex flex-col items-center">
      <div className="mb-3 text-sm font-medium text-neutral-400">
        Preview: <span className="text-purple-400 font-mono">{title}</span>
      </div>

      {/* Smartphone Outer Shell */}
      <div className="relative w-[360px] h-[740px] bg-neutral-900 border-[10px] border-neutral-800 rounded-[48px] shadow-2xl overflow-hidden ring-1 ring-white/10 flex flex-col">
        {/* Camera Punchhole */}
        <div className="absolute top-4 left-1/2 -translate-x-1/2 w-4 h-4 bg-black rounded-full z-20 ring-2 ring-neutral-800" />

        {/* Screen Content */}
        <div className="relative w-full h-full bg-black overflow-auto flex items-center justify-center">
          {isLoading && (
            <div className="absolute inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center z-10">
              <div className="w-8 h-8 border-3 border-purple-500 border-t-transparent rounded-full animate-spin" />
            </div>
          )}
          <img
            src={imageUrl}
            alt={title}
            className="w-full h-full object-contain transition-opacity duration-200"
            onError={(e) => {
              (e.target as HTMLImageElement).src =
                'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="600" viewBox="0 0 300 600"><rect fill="%23111" width="300" height="600"/><text fill="%23888" x="50%" y="50%" text-anchor="middle" font-family="sans-serif">Rendering preview...</text></svg>';
            }}
          />
        </div>

        {/* Home Indicator Bar */}
        <div className="absolute bottom-2 left-1/2 -translate-x-1/2 w-32 h-1 bg-neutral-600 rounded-full z-20" />
      </div>
    </div>
  );
};
```

### 3.4. Create `web-viewer/src/App.tsx`
```tsx
import React, { useState, useEffect } from 'react';
import { DeviceFrame } from './components/DeviceFrame';

const HTTP_BASE = 'http://localhost:3001';
const WS_URL = 'ws://localhost:3002';

export const App: React.FC = () => {
  const [imageUrl, setImageUrl] = useState<string>(`${HTTP_BASE}/api/preview/latest.png?v=${Date.now()}`);
  const [connected, setConnected] = useState<boolean>(false);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
  const [refreshCount, setRefreshCount] = useState<number>(0);

  useEffect(() => {
    let ws: WebSocket | null = null;
    let reconnectTimeout: NodeJS.Timeout;

    function connect() {
      try {
        ws = new WebSocket(WS_URL);

        ws.onopen = () => {
          setConnected(true);
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.event === 'PREVIEW_UPDATED') {
              setImageUrl(`${HTTP_BASE}${data.payload.url}`);
              setLastUpdated(new Date());
              setRefreshCount((prev) => prev + 1);
            }
          } catch (err) {
            console.error('Failed to parse WebSocket message:', err);
          }
        };

        ws.onclose = () => {
          setConnected(false);
          reconnectTimeout = setTimeout(connect, 2000);
        };

        ws.onerror = () => {
          ws?.close();
        };
      } catch {
        reconnectTimeout = setTimeout(connect, 2000);
      }
    }

    connect();

    return () => {
      ws?.close();
      clearTimeout(reconnectTimeout);
    };
  }, []);

  const manualReload = () => {
    setImageUrl(`${HTTP_BASE}/api/preview/latest.png?v=${Date.now()}`);
    setLastUpdated(new Date());
  };

  return (
    <div className="flex flex-col min-h-screen">
      {/* Navigation Header */}
      <header className="flex items-center justify-between px-6 py-4 border-b border-neutral-800 bg-neutral-900/50 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <span className="text-2xl">🎨</span>
          <span className="font-bold text-lg tracking-tight">Compositor</span>
          <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-purple-900/50 text-purple-300 border border-purple-700/50">
            Tracer Bullet
          </span>
        </div>

        <div className="flex items-center gap-4 text-sm">
          <div className="flex items-center gap-2">
            <span className={`w-2.5 h-2.5 rounded-full ${connected ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`} />
            <span className="text-neutral-400">{connected ? 'Live Sync' : 'Reconnecting...'}</span>
          </div>

          <button
            onClick={manualReload}
            className="px-3 py-1.5 text-xs font-medium rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-200 transition-colors"
          >
            Refresh
          </button>
        </div>
      </header>

      {/* Main Canvas Area */}
      <main className="flex-1 flex flex-col items-center justify-center p-8">
        <DeviceFrame
          imageUrl={imageUrl}
          title="GreetingPreview"
        />

        <div className="mt-4 text-xs text-neutral-500">
          Last updated: {lastUpdated.toLocaleTimeString()} (Updates: {refreshCount})
        </div>
      </main>
    </div>
  );
};
```

### 3.5. Create `web-viewer/src/main.tsx`
```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

---

## 4. Automated Verification Gate

Run build and typecheck from the `web-viewer/` directory:
```powershell
cd web-viewer
npm run build
npm test
```

### Acceptance Criteria:
* `npm run build` completes with exit code 0 (`dist/` generated).
* `npm test` passes.
* No TypeScript lint or typecheck errors.

---

## 5. Post-Execution Checklist
1. Commit changes: `feat: implement web viewer canvas with device frame mockup`.
2. Update [`brain/STATE.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/STATE.md): mark Step 04 complete, advance active step to Step 05.
3. Record outputs in the log below.

---

## 6. Execution Log (To be completed by executing agent)
* **Execution Date**: _[Pending Execution]_
* **Executing Agent**: _[Agent Name/Model]_
* **Build Status**: _[Pass/Fail]_
