import React, { createContext, useContext, useState, useEffect, useMemo, useCallback } from 'react';
import { PreviewCatalog, PreviewItem, PreviewRenderStatus } from '../types/preview';
import { usePreviewConfig } from './PreviewConfigContext';

export interface PreviewCatalogState {
  catalog: PreviewCatalog | null;
  loading: boolean;
  connected: boolean;
  activePreviewId: string | null;
  activePreview: PreviewItem | null;
  searchQuery: string;
  selectedGroup: string | null;
  refreshCount: number;
  lastUpdated: Date;
  filteredPreviews: PreviewItem[];
  allGroups: string[];
  setActivePreviewId: (id: string | null) => void;
  setSearchQuery: (query: string) => void;
  setSelectedGroup: (group: string | null) => void;
  refreshCatalog: () => Promise<void>;
  triggerRender: (previewId?: string) => Promise<void>;
  httpBase: string;
}

declare global {
  interface Window {
    __COMPOSITOR_CONFIG__?: {
      serverPort?: number;
      wsPort?: number;
      httpBase?: string;
      wsUrl?: string;
      activePreviewId?: string | null;
    };
  }
}

function resolveHttpBase(): string {
  if (typeof window !== 'undefined') {
    if (window.__COMPOSITOR_CONFIG__?.httpBase) {
      return window.__COMPOSITOR_CONFIG__.httpBase;
    }
    const loc = window.location;
    if (loc.host && !loc.host.includes('localhost:0') && (loc.protocol === 'http:' || loc.protocol === 'https:')) {
      return `${loc.protocol}//${loc.host}`;
    }
  }
  return 'http://localhost:3001';
}

function resolveWsUrl(httpBase: string): string {
  if (typeof window !== 'undefined' && window.__COMPOSITOR_CONFIG__?.wsUrl) {
    let wsUrl = window.__COMPOSITOR_CONFIG__.wsUrl;
    if (!wsUrl.endsWith('/ws')) {
      wsUrl = wsUrl.replace(/\/+$/, '') + '/ws';
    }
    return wsUrl;
  }
  try {
    const url = new URL(httpBase);
    const proto = url.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${proto}//${url.host}/ws`;
  } catch {
    return 'ws://localhost:3001/ws';
  }
}

const HTTP_BASE = resolveHttpBase();
const WS_URL = resolveWsUrl(HTTP_BASE);

const PreviewCatalogContext = createContext<PreviewCatalogState | undefined>(undefined);

export const PreviewCatalogProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [catalog, setCatalog] = useState<PreviewCatalog | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [connected, setConnected] = useState<boolean>(false);
  const [activePreviewId, setActivePreviewIdState] = useState<string | null>(() => {
    // Check hash parameter initially (e.g. #preview=id)
    if (typeof window !== 'undefined' && window.location.hash) {
      const match = window.location.hash.match(/preview=([^&]+)/);
      if (match) return decodeURIComponent(match[1]);
    }
    return null;
  });
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedGroup, setSelectedGroup] = useState<string | null>(null);
  const [refreshCount, setRefreshCount] = useState<number>(0);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  const { setErrorDiagnostic, clearError } = usePreviewConfig();
  const clearErrorRef = React.useRef(clearError);
  clearErrorRef.current = clearError;
  const setErrorDiagnosticRef = React.useRef(setErrorDiagnostic);
  setErrorDiagnosticRef.current = setErrorDiagnostic;

  const setActivePreviewId = useCallback((id: string | null) => {
    setActivePreviewIdState(id);
    if (typeof window !== 'undefined') {
      if (id) {
        window.location.hash = `preview=${encodeURIComponent(id)}`;
      } else {
        history.replaceState(null, '', window.location.pathname + window.location.search);
      }
    }
  }, []);

  const mountedRef = React.useRef(true);
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // Fetch catalog from REST API
  const refreshCatalog = useCallback(async () => {
    try {
      if (mountedRef.current) setLoading(true);
      const res = await fetch(`${HTTP_BASE}/api/previews`);
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
      }
      const data: PreviewCatalog = await res.json();
      if (!mountedRef.current) return;
      setCatalog(data);
      setLastUpdated(new Date());

      // If no active preview is selected, select the first available preview
      setActivePreviewIdState((currentActive) => {
        if (currentActive && data.previews[currentActive]) {
          return currentActive;
        }
        const ids = Object.keys(data.previews);
        return ids.length > 0 ? ids[0] : null;
      });
    } catch (err) {
      if (mountedRef.current) {
        console.error('[Compositor] Failed to fetch previews catalog:', err);
      }
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, []);

  // Trigger targeted re-render
  const triggerRender = useCallback(async (previewId?: string) => {
    const targetId = previewId ?? activePreviewId;
    if (!targetId) return;

    // Update status optimistically
    setCatalog((prev) => {
      if (!prev || !prev.previews[targetId]) return prev;
      return {
        ...prev,
        previews: {
          ...prev.previews,
          [targetId]: {
            ...prev.previews[targetId],
            status: 'Rendering' as PreviewRenderStatus,
          },
        },
      };
    });

    try {
      await fetch(`${HTTP_BASE}/api/previews/${encodeURIComponent(targetId)}/render`, {
        method: 'POST',
      });
    } catch (err) {
      console.error(`[Compositor] Error triggering render for ${targetId}:`, err);
    }
  }, [activePreviewId]);

  // Initial fetch
  useEffect(() => {
    refreshCatalog();
  }, [refreshCatalog]);

  // WebSocket Live Sync
  useEffect(() => {
    let ws: WebSocket | null = null;
    let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
    let isDisposed = false;

    function safeClose(socket: WebSocket | null) {
      if (!socket) return;
      socket.onopen = null;
      socket.onmessage = null;
      socket.onclose = null;
      socket.onerror = null;

      if (socket.readyState === WebSocket.OPEN) {
        socket.close();
      } else if (socket.readyState === WebSocket.CONNECTING) {
        // Wait for connection to open before closing to prevent browser warning:
        // "WebSocket is closed before the connection is established"
        socket.onopen = () => {
          socket.close();
        };
      }
    }

    function connect() {
      if (isDisposed) return;

      try {
        ws = new WebSocket(WS_URL);

        ws.onopen = () => {
          if (isDisposed) {
            safeClose(ws);
            return;
          }
          if (mountedRef.current) {
            setConnected(true);
            console.info('[Compositor WS] Connected to live preview server at:', WS_URL);
          }
        };

        ws.onmessage = (event) => {
          if (isDisposed) return;
          try {
            // Guard against empty frames, whitespace, non-string, or heartbeat ping/pong
            if (!event.data || typeof event.data !== 'string') return;
            const text = event.data.trim();
            if (!text || text === 'ping' || text === 'pong') return;

            let message: Record<string, unknown>;
            try {
              message = JSON.parse(text) as Record<string, unknown>;
            } catch {
              console.warn('[Compositor WS] Non-JSON or truncated WebSocket frame ignored:', text);
              return;
            }

            if (!message || typeof message !== 'object') return;
            const { event: eventName, payload } = message as { event: string; payload: Record<string, unknown> };
            if (!eventName || !payload) return;

            if (eventName === 'PREVIEW_REGISTERED' && payload.preview) {
              const item = payload.preview as PreviewItem;
              console.info('[Compositor WS] PREVIEW_REGISTERED:', item.id);
              setCatalog((prev) => {
                if (!prev) return prev;
                const existing = prev.previews[item.id];
                return {
                  ...prev,
                  previews: { ...prev.previews, [item.id]: { ...existing, ...item } },
                  totalCount: existing ? prev.totalCount : prev.totalCount + 1,
                };
              });
            } else if (eventName === 'PREVIEW_RENDER_STARTED' && payload.previewId) {
              const previewId = payload.previewId as string;
              const startedAt = (payload.timestamp as number) ?? Date.now();
              console.info(`[Compositor WS] PREVIEW_RENDER_STARTED for ${previewId} (timestamp: ${startedAt})`);

              setCatalog((prev) => {
                if (!prev || !prev.previews[previewId]) return prev;
                const existing = prev.previews[previewId];
                if (existing.lastRenderedAt && existing.lastRenderedAt >= startedAt) {
                  console.warn(
                    `[Compositor WS] Stale PREVIEW_RENDER_STARTED ignored for ${previewId} (already rendered at ${existing.lastRenderedAt} >= ${startedAt})`
                  );
                  return prev;
                }
                return {
                  ...prev,
                  previews: {
                    ...prev.previews,
                    [previewId]: {
                      ...existing,
                      status: 'Rendering',
                    },
                  },
                };
              });
            } else if (eventName === 'PREVIEW_UPDATED' && payload.previewId) {
              const previewId = payload.previewId as string;
              const timestamp = (payload.timestamp as number) ?? Date.now();
              const rawUrl = (payload.url as string) || `/api/previews/${encodeURIComponent(previewId)}/image`;
              const freshUrl = rawUrl.includes('?')
                ? (rawUrl.includes('t=') ? rawUrl : `${rawUrl}&t=${timestamp}`)
                : `${rawUrl}?t=${timestamp}`;

              console.info(`[Compositor WS] PREVIEW_UPDATED for ${previewId} -> ${freshUrl}`);

              setCatalog((prev) => {
                if (!prev) return prev;
                const existing = prev.previews[previewId];
                const previewObj = payload.preview as Partial<PreviewItem> | undefined;

                const updatedItem: PreviewItem = {
                  ...(existing || {}),
                  ...(previewObj || {}),
                  id: previewId,
                  status: 'Rendered',
                  imageUrl: freshUrl,
                  lastRenderedAt: timestamp,
                } as PreviewItem;

                return {
                  ...prev,
                  previews: {
                    ...prev.previews,
                    [previewId]: updatedItem,
                  },
                };
              });
              setLastUpdated(new Date());
              setRefreshCount((c) => c + 1);

              // Clear diagnostic error if this was the errored preview
              clearErrorRef.current();
            } else if (eventName === 'RENDER_ERROR' && payload.previewId) {
              const previewId = payload.previewId;
              console.error(`[Compositor WS] RENDER_ERROR for ${previewId}:`, payload.error);

              setCatalog((prev) => {
                if (!prev || !prev.previews[payload.previewId]) return prev;
                return {
                  ...prev,
                  previews: {
                    ...prev.previews,
                    [payload.previewId]: {
                      ...prev.previews[payload.previewId],
                      status: 'Error',
                      errorDetails: typeof payload.error === 'string' ? payload.error : JSON.stringify(payload.error),
                    },
                  },
                };
              });

              // Format and display diagnostic error
              const rawError = payload.error || 'Rendering failure';
              const messageStr = typeof rawError === 'string' ? rawError : rawError.message || JSON.stringify(rawError);
              const stackStr = typeof rawError === 'object' && rawError.stack ? rawError.stack : undefined;

              // Try parsing file and line info (e.g. at Greeting.kt:14 or (Greeting.kt:14))
              const match = messageStr.match(/([a-zA-Z0-9_-]+\.kt):(\d+)(?::(\d+))?/);

              setErrorDiagnosticRef.current({
                previewId: payload.previewId,
                title: 'Compose Render Failure',
                message: messageStr,
                filePath: match ? match[1] : undefined,
                line: match ? parseInt(match[2], 10) : undefined,
                column: match && match[3] ? parseInt(match[3], 10) : undefined,
                stackTrace: stackStr,
                timestamp: Date.now(),
              });
            }
          } catch (err) {
            console.warn('[Compositor] Error handling WebSocket message:', err);
          }
        };

        ws.onclose = () => {
          if (isDisposed) return;
          if (mountedRef.current) {
            setConnected(false);
          }
          reconnectTimeout = setTimeout(connect, 2000);
        };

        ws.onerror = () => {
          if (isDisposed) return;
          // In WebSocket spec, onerror is followed by onclose.
          // Only close explicitly if already OPEN to prevent premature closure warning.
          if (ws && ws.readyState === WebSocket.OPEN) {
            ws.close();
          }
        };
      } catch {
        if (!isDisposed) {
          reconnectTimeout = setTimeout(connect, 2000);
        }
      }
    }

    connect();

    return () => {
      isDisposed = true;
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
      safeClose(ws);
    };
  }, []);

  // Safety watchdog: Automatically recover if any preview gets stuck in 'Rendering'
  useEffect(() => {
    const watchdogInterval = setInterval(() => {
      if (!catalog) return;
      const stuckPreviews = Object.values(catalog.previews).filter(
        (p) => p.status === 'Rendering'
      );
      if (stuckPreviews.length > 0) {
        console.warn(
          `[Compositor Watchdog] Preview(s) in 'Rendering' state for > 5s. Auto-recovering...`,
          stuckPreviews.map((p) => p.id)
        );
        refreshCatalog().then(() => {
          setCatalog((current) => {
            if (!current) return current;
            let changed = false;
            const updated = { ...current.previews };
            for (const sp of stuckPreviews) {
              if (updated[sp.id]?.status === 'Rendering') {
                changed = true;
                updated[sp.id] = {
                  ...updated[sp.id],
                  status: 'Rendered',
                };
              }
            }
            return changed ? { ...current, previews: updated } : current;
          });
        });
      }
    }, 5000);

    return () => clearInterval(watchdogInterval);
  }, [catalog, refreshCatalog]);

  // Active preview object
  const activePreview = useMemo(() => {
    if (!catalog || !activePreviewId) return null;
    return catalog.previews[activePreviewId] || null;
  }, [catalog, activePreviewId]);

  // Unique groups across all previews
  const allGroups = useMemo(() => {
    if (!catalog) return [];
    const groups = new Set<string>();
    for (const preview of Object.values(catalog.previews)) {
      if (preview.definition.parameters.group) {
        groups.add(preview.definition.parameters.group);
      }
    }
    return Array.from(groups).sort();
  }, [catalog]);

  // Filtered previews list based on search and selected group chip
  const filteredPreviews = useMemo(() => {
    if (!catalog) return [];
    const q = searchQuery.trim().toLowerCase();

    return Object.values(catalog.previews).filter((preview) => {
      // Group filter
      if (selectedGroup && preview.definition.parameters.group !== selectedGroup) {
        return false;
      }
      // Search filter
      if (!q) return true;
      const fnName = preview.definition.functionName.toLowerCase();
      const paramName = (preview.definition.parameters.name || '').toLowerCase();
      const groupName = (preview.definition.parameters.group || '').toLowerCase();
      const moduleName = preview.module.toLowerCase();
      const filePath = preview.definition.filePath.toLowerCase();

      return (
        fnName.includes(q) ||
        paramName.includes(q) ||
        groupName.includes(q) ||
        moduleName.includes(q) ||
        filePath.includes(q)
      );
    });
  }, [catalog, searchQuery, selectedGroup]);

  return (
    <PreviewCatalogContext.Provider
      value={{
        catalog,
        loading,
        connected,
        activePreviewId,
        activePreview,
        searchQuery,
        selectedGroup,
        refreshCount,
        lastUpdated,
        filteredPreviews,
        allGroups,
        setActivePreviewId,
        setSearchQuery,
        setSelectedGroup,
        refreshCatalog,
        triggerRender,
        httpBase: HTTP_BASE,
      }}
    >
      {children}
    </PreviewCatalogContext.Provider>
  );
};

export const usePreviewCatalog = (): PreviewCatalogState => {
  const context = useContext(PreviewCatalogContext);
  if (!context) {
    throw new Error('usePreviewCatalog must be used within a PreviewCatalogProvider');
  }
  return context;
};
