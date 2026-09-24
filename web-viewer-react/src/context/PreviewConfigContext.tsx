import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import {
  DeviceProfileId,
  Orientation,
  ThemeMode,
  ViewMode,
  MatrixPreset,
  BackdropPattern,
  LayoutNode,
  RenderErrorDiagnostic,
} from '../types/preview';

export interface PreviewConfigState {
  deviceProfile: DeviceProfileId;
  orientation: Orientation;
  themeMode: ThemeMode;
  fontScale: number;
  viewMode: ViewMode;
  matrixPreset: MatrixPreset;
  backdrop: BackdropPattern;
  zoom: number;
  isFitHeight: boolean;
  inspectorEnabled: boolean;
  selectedNode: LayoutNode | null;
  hoveredNode: LayoutNode | null;
  sidebarCollapsed: boolean;
  errorDiagnostic: RenderErrorDiagnostic | null;
  setDeviceProfile: (profile: DeviceProfileId) => void;
  setOrientation: (orientation: Orientation) => void;
  setThemeMode: (mode: ThemeMode) => void;
  setFontScale: (scale: number) => void;
  setViewMode: (mode: ViewMode) => void;
  setMatrixPreset: (preset: MatrixPreset) => void;
  setBackdrop: (backdrop: BackdropPattern) => void;
  setZoom: (zoom: number) => void;
  zoomIn: () => void;
  zoomOut: () => void;
  resetZoom: () => void;
  fitToHeight: () => void;
  setIsFitHeight: (fit: boolean) => void;
  setInspectorEnabled: (enabled: boolean | ((prev: boolean) => boolean)) => void;
  setSelectedNode: (node: LayoutNode | null) => void;
  setHoveredNode: (node: LayoutNode | null) => void;
  setSidebarCollapsed: (collapsed: boolean | ((prev: boolean) => boolean)) => void;
  setErrorDiagnostic: (diagnostic: RenderErrorDiagnostic | null) => void;
  clearError: () => void;
}

const STORAGE_KEY_PREFIX = 'compositor:config:';

function getStoredValue<T>(key: string, defaultValue: T): T {
  try {
    const val = localStorage.getItem(STORAGE_KEY_PREFIX + key);
    return val ? (JSON.parse(val) as T) : defaultValue;
  } catch {
    return defaultValue;
  }
}

function setStoredValue<T>(key: string, value: T): void {
  try {
    localStorage.setItem(STORAGE_KEY_PREFIX + key, JSON.stringify(value));
  } catch {
    // Ignore quota or private storage errors
  }
}

const PreviewConfigContext = createContext<PreviewConfigState | undefined>(undefined);

export const PreviewConfigProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [deviceProfile, setDeviceProfileState] = useState<DeviceProfileId>(() =>
    getStoredValue<DeviceProfileId>('deviceProfile', 'pixel-8')
  );
  const [orientation, setOrientationState] = useState<Orientation>(() =>
    getStoredValue<Orientation>('orientation', 'portrait')
  );
  const [themeMode, setThemeModeState] = useState<ThemeMode>(() =>
    getStoredValue<ThemeMode>('themeMode', 'dark')
  );
  const [fontScale, setFontScaleState] = useState<number>(() =>
    getStoredValue<number>('fontScale', 1.0)
  );
  const [viewMode, setViewModeState] = useState<ViewMode>('single');
  const [matrixPreset, setMatrixPresetState] = useState<MatrixPreset>('theme');
  const [backdrop, setBackdropState] = useState<BackdropPattern>(() =>
    getStoredValue<BackdropPattern>('backdrop', 'dark')
  );
  const [zoom, setZoomState] = useState<number>(1.0);
  const [isFitHeight, setIsFitHeight] = useState<boolean>(true);
  const [inspectorEnabled, setInspectorEnabledState] = useState<boolean>(false);
  const [selectedNode, setSelectedNode] = useState<LayoutNode | null>(null);
  const [hoveredNode, setHoveredNode] = useState<LayoutNode | null>(null);
  const [sidebarCollapsed, setSidebarCollapsedState] = useState<boolean>(() =>
    getStoredValue<boolean>('sidebarCollapsed', false)
  );
  const [errorDiagnostic, setErrorDiagnostic] = useState<RenderErrorDiagnostic | null>(null);

  const setDeviceProfile = (profile: DeviceProfileId) => {
    setDeviceProfileState(profile);
    setStoredValue('deviceProfile', profile);
  };

  const setOrientation = (o: Orientation) => {
    setOrientationState(o);
    setStoredValue('orientation', o);
  };

  const setThemeMode = (mode: ThemeMode) => {
    setThemeModeState(mode);
    setStoredValue('themeMode', mode);
  };

  const setFontScale = (scale: number) => {
    setFontScaleState(scale);
    setStoredValue('fontScale', scale);
  };

  const setViewMode = (mode: ViewMode) => {
    setViewModeState(mode);
  };

  const setMatrixPreset = (preset: MatrixPreset) => {
    setMatrixPresetState(preset);
  };

  const setBackdrop = (b: BackdropPattern) => {
    setBackdropState(b);
    setStoredValue('backdrop', b);
  };

  const setZoom = useCallback((z: number) => {
    setIsFitHeight(false);
    setZoomState(Math.max(0.25, Math.min(2.5, z)));
  }, []);

  const zoomIn = useCallback(() => {
    setIsFitHeight(false);
    setZoomState((prev) => Math.min(2.5, Math.round((prev + 0.1) * 10) / 10));
  }, []);

  const zoomOut = useCallback(() => {
    setIsFitHeight(false);
    setZoomState((prev) => Math.max(0.25, Math.round((prev - 0.1) * 10) / 10));
  }, []);

  const resetZoom = useCallback(() => {
    setIsFitHeight(false);
    setZoomState(1.0);
  }, []);

  const fitToHeight = useCallback(() => {
    setIsFitHeight(true);
  }, []);

  const setInspectorEnabled = (enabled: boolean | ((prev: boolean) => boolean)) => {
    setInspectorEnabledState(enabled);
    if (!enabled) {
      setSelectedNode(null);
      setHoveredNode(null);
    }
  };

  const setSidebarCollapsed = (collapsed: boolean | ((prev: boolean) => boolean)) => {
    setSidebarCollapsedState((prev) => {
      const next = typeof collapsed === 'function' ? collapsed(prev) : collapsed;
      setStoredValue('sidebarCollapsed', next);
      return next;
    });
  };

  const clearError = useCallback(() => {
    setErrorDiagnostic(null);
  }, []);

  // Keyboard shortcut: Alt + I toggles inspector mode
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.altKey && (e.key === 'i' || e.key === 'I')) {
        e.preventDefault();
        setInspectorEnabled((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <PreviewConfigContext.Provider
      value={{
        deviceProfile,
        orientation,
        themeMode,
        fontScale,
        viewMode,
        matrixPreset,
        backdrop,
        zoom,
        isFitHeight,
        inspectorEnabled,
        selectedNode,
        hoveredNode,
        sidebarCollapsed,
        errorDiagnostic,
        setDeviceProfile,
        setOrientation,
        setThemeMode,
        setFontScale,
        setViewMode,
        setMatrixPreset,
        setBackdrop,
        setZoom,
        zoomIn,
        zoomOut,
        resetZoom,
        fitToHeight,
        setIsFitHeight,
        setInspectorEnabled,
        setSelectedNode,
        setHoveredNode,
        setSidebarCollapsed,
        setErrorDiagnostic,
        clearError,
      }}
    >
      {children}
    </PreviewConfigContext.Provider>
  );
};

export const usePreviewConfig = (): PreviewConfigState => {
  const context = useContext(PreviewConfigContext);
  if (!context) {
    throw new Error('usePreviewConfig must be used within a PreviewConfigProvider');
  }
  return context;
};
