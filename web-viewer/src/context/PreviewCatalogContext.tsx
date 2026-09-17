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

const HTTP_BASE = 'http://localhost:3001';
const WS_URL = 'ws://localhost:3002';

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

  // Fetch catalog from REST API
  const refreshCatalog = useCallback(async () => {
    try {
      setLoading(true);
      const res = await fetch(`${HTTP_BASE}/api/previews`);
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
      }
      const data: PreviewCatalog = await res.json();
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
      console.error('[Compositor] Failed to fetch previews catalog:', err);
    } finally {
      setLoading(false);
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
    let reconnectTimeout: ReturnType<typeof setTimeout>;

    function connect() {
      try {
        ws = new WebSocket(WS_URL);

        ws.onopen = () => {
          setConnected(true);
        };

        ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data);
            const { event: eventName, payload } = message;

            if (eventName === 'PREVIEW_REGISTERED' && payload.preview) {
              const item: PreviewItem = payload.preview;
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
              setCatalog((prev) => {
                if (!prev || !prev.previews[payload.previewId]) return prev;
                return {
                  ...prev,
                  previews: {
                    ...prev.previews,
                    [payload.previewId]: {
                      ...prev.previews[payload.previewId],
                      status: 'Rendering',
                    },
                  },
                };
              });
            } else if (eventName === 'PREVIEW_UPDATED' && payload.previewId) {
              setCatalog((prev) => {
                if (!prev || !prev.previews[payload.previewId]) return prev;
                return {
                  ...prev,
                  previews: {
                    ...prev.previews,
                    [payload.previewId]: {
                      ...prev.previews[payload.previewId],
                      status: 'Rendered',
                      imageUrl: payload.url,
                      lastRenderedAt: payload.timestamp ?? Date.now(),
                    },
                  },
                };
              });
              setLastUpdated(new Date());
              setRefreshCount((c) => c + 1);

              // Clear diagnostic error if this was the errored preview
              clearError();
            } else if (eventName === 'RENDER_ERROR' && payload.previewId) {
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

              setErrorDiagnostic({
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
            console.error('[Compositor] Error handling WebSocket message:', err);
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
  }, [clearError, setErrorDiagnostic]);

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
