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
