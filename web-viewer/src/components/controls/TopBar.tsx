import React from 'react';
import {
  Sun,
  Moon,
  Smartphone,
  RotateCw,
  Type,
  LayoutGrid,
  Square,
  Crosshair,
  Palette,
  RefreshCw,
  ZoomIn,
  ZoomOut,
  Maximize2,
} from 'lucide-react';
import { usePreviewConfig } from '../../context/PreviewConfigContext';
import { usePreviewCatalog } from '../../context/PreviewCatalogContext';
import { DeviceProfileId, BackdropPattern, MatrixPreset } from '../../types/preview';

export const TopBar: React.FC = () => {
  const {
    deviceProfile,
    setDeviceProfile,
    orientation,
    setOrientation,
    themeMode,
    setThemeMode,
    fontScale,
    setFontScale,
    viewMode,
    setViewMode,
    matrixPreset,
    setMatrixPreset,
    backdrop,
    setBackdrop,
    inspectorEnabled,
    setInspectorEnabled,
    zoom,
    zoomIn,
    zoomOut,
    fitToHeight,
    isFitHeight,
  } = usePreviewConfig();

  const { connected, refreshCatalog, loading, activePreview, triggerRender } = usePreviewCatalog();

  const fontScales = [0.85, 1.0, 1.15, 1.3, 1.5];

  const handleDeviceChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setDeviceProfile(e.target.value as DeviceProfileId);
  };

  const handleBackdropChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setBackdrop(e.target.value as BackdropPattern);
  };

  return (
    <header className="h-14 bg-neutral-900 border-b border-neutral-800 px-4 flex items-center justify-between text-neutral-200 z-30 select-none">
      {/* Brand & Connection Status */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <span className="text-xl">🎨</span>
          <span className="font-bold text-base tracking-tight bg-gradient-to-r from-purple-400 to-indigo-300 bg-clip-text text-transparent">
            Compositor
          </span>
          <span className="px-1.5 py-0.5 text-[10px] font-mono font-medium rounded-md bg-purple-900/50 text-purple-300 border border-purple-700/50">
            Phase 2
          </span>
        </div>

        <div className="w-px h-5 bg-neutral-800" />

        {/* Live sync connection indicator */}
        <div className="flex items-center gap-1.5 text-xs">
          <span
            className={`w-2 h-2 rounded-full ${
              connected ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'
            }`}
          />
          <span className="text-neutral-400 text-xs hidden sm:inline">
            {connected ? 'Live Sync' : 'Reconnecting...'}
          </span>
        </div>

        {/* Active preview name pill */}
        {activePreview && (
          <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-neutral-800/80 border border-neutral-700/40 text-xs text-neutral-300">
            <span className="text-neutral-500 font-mono">Preview:</span>
            <span className="font-medium text-purple-300 font-mono truncate max-w-[140px]">
              {activePreview.definition.functionName}
            </span>
          </div>
        )}
      </div>

      {/* Center Controls: View Mode & Inspector Controls */}
      <div className="flex items-center gap-2">
        {/* Single Device vs Matrix Grid Toggle */}
        <div className="flex items-center bg-neutral-950 p-0.5 rounded-lg border border-neutral-800">
          <button
            onClick={() => setViewMode('single')}
            className={`flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-md font-medium transition-colors ${
              viewMode === 'single'
                ? 'bg-purple-600 text-white shadow-xs'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            <Square className="w-3.5 h-3.5" />
            <span className="hidden md:inline">Single</span>
          </button>
          <button
            onClick={() => setViewMode('matrix')}
            className={`flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-md font-medium transition-colors ${
              viewMode === 'matrix'
                ? 'bg-purple-600 text-white shadow-xs'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            <LayoutGrid className="w-3.5 h-3.5" />
            <span className="hidden md:inline">Matrix</span>
          </button>
        </div>

        {viewMode === 'matrix' && (
          /* Matrix Preset Selector */
          <div className="flex items-center bg-neutral-950 px-2 py-1 rounded-lg border border-neutral-800 text-xs">
            <span className="text-neutral-400 mr-2 text-[11px]">Matrix:</span>
            <select
              value={matrixPreset}
              onChange={(e) => setMatrixPreset(e.target.value as MatrixPreset)}
              className="bg-transparent text-purple-300 font-medium focus:outline-hidden text-xs cursor-pointer"
            >
              <option value="theme" className="bg-neutral-900 text-neutral-200">
                Light vs Dark
              </option>
              <option value="font-scale" className="bg-neutral-900 text-neutral-200">
                Font Scales (0.85x - 1.5x)
              </option>
              <option value="group" className="bg-neutral-900 text-neutral-200">
                Group Previews
              </option>
              <option value="all" className="bg-neutral-900 text-neutral-200">
                All Previews
              </option>
            </select>
          </div>
        )}

        <div className="w-px h-5 bg-neutral-800 hidden md:block" />

        {/* Device Profile Switcher (Single Mode only) */}
        {viewMode === 'single' && (
          <div className="hidden md:flex items-center gap-1 bg-neutral-950 px-2 py-1 rounded-lg border border-neutral-800 text-xs">
            <Smartphone className="w-3.5 h-3.5 text-neutral-400" />
            <select
              value={deviceProfile}
              onChange={handleDeviceChange}
              className="bg-transparent text-neutral-200 focus:outline-hidden text-xs cursor-pointer font-medium"
            >
              <option value="pixel-8" className="bg-neutral-900 text-neutral-200">
                Pixel 8
              </option>
              <option value="galaxy-s24" className="bg-neutral-900 text-neutral-200">
                Galaxy S24 Ultra
              </option>
              <option value="foldable" className="bg-neutral-900 text-neutral-200">
                Foldable
              </option>
              <option value="tablet-10" className="bg-neutral-900 text-neutral-200">
                Tablet (10")
              </option>
              <option value="frameless" className="bg-neutral-900 text-neutral-200">
                Frameless
              </option>
            </select>
          </div>
        )}

        {/* Orientation Switcher */}
        {viewMode === 'single' && (
          <button
            onClick={() => setOrientation(orientation === 'portrait' ? 'landscape' : 'portrait')}
            title={`Orientation: ${orientation} (Click to flip)`}
            className={`p-1.5 rounded-lg border border-neutral-800 bg-neutral-950 hover:border-neutral-700 transition-colors ${
              orientation === 'landscape' ? 'text-purple-400' : 'text-neutral-300'
            }`}
          >
            <RotateCw className="w-4 h-4" />
          </button>
        )}

        {/* Theme Switcher: Light / Dark */}
        <button
          onClick={() => setThemeMode(themeMode === 'light' ? 'dark' : 'light')}
          title={`Theme: ${themeMode} mode (Click to toggle)`}
          className="p-1.5 rounded-lg border border-neutral-800 bg-neutral-950 hover:border-neutral-700 text-neutral-300 transition-colors"
        >
          {themeMode === 'light' ? (
            <Sun className="w-4 h-4 text-amber-400" />
          ) : (
            <Moon className="w-4 h-4 text-purple-400" />
          )}
        </button>

        {/* Font Scale Stepper */}
        {viewMode === 'single' && (
          <div className="hidden lg:flex items-center gap-1 bg-neutral-950 px-2 py-1 rounded-lg border border-neutral-800 text-xs">
            <Type className="w-3.5 h-3.5 text-neutral-400" />
            <select
              value={fontScale}
              onChange={(e) => setFontScale(parseFloat(e.target.value))}
              className="bg-transparent text-neutral-200 focus:outline-hidden text-xs cursor-pointer font-mono"
            >
              {fontScales.map((scale) => (
                <option key={scale} value={scale} className="bg-neutral-900 text-neutral-200">
                  {scale.toFixed(2)}x
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Canvas Backdrop Selector */}
        <div className="hidden xl:flex items-center gap-1 bg-neutral-950 px-2 py-1 rounded-lg border border-neutral-800 text-xs">
          <Palette className="w-3.5 h-3.5 text-neutral-400" />
          <select
            value={backdrop}
            onChange={handleBackdropChange}
            className="bg-transparent text-neutral-300 focus:outline-hidden text-xs cursor-pointer"
          >
            <option value="dark" className="bg-neutral-900 text-neutral-200">
              Dark Canvas
            </option>
            <option value="light" className="bg-neutral-900 text-neutral-200">
              Light Canvas
            </option>
            <option value="checkerboard" className="bg-neutral-900 text-neutral-200">
              Checkerboard
            </option>
          </select>
        </div>

        {/* Element Inspector Overlay Toggle (Alt + I) */}
        <button
          onClick={() => setInspectorEnabled((prev) => !prev)}
          title="Toggle Element Inspector Overlay (Alt+I)"
          className={`flex items-center gap-1 px-2.5 py-1 text-xs rounded-lg border transition-all ${
            inspectorEnabled
              ? 'bg-indigo-600/30 border-indigo-500 text-indigo-300 shadow-xs'
              : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:text-neutral-200'
          }`}
        >
          <Crosshair className="w-3.5 h-3.5" />
          <span className="hidden sm:inline font-medium">Inspect</span>
          <span className="text-[10px] opacity-60 font-mono hidden xl:inline">Alt+I</span>
        </button>

        {/* Zoom Controls (Zoom Out, %, Zoom In, Fit Height) */}
        <div className="flex items-center bg-neutral-950 px-1 py-0.5 rounded-lg border border-neutral-800 text-xs">
          <button
            onClick={zoomOut}
            title="Zoom Out"
            className="p-1 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
          >
            <ZoomOut className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={fitToHeight}
            title={isFitHeight ? 'Fit to Height (Active)' : 'Click to Fit Height'}
            className={`px-1.5 py-0.5 text-xs font-mono rounded-xs transition-colors cursor-pointer ${
              isFitHeight
                ? 'text-purple-300 font-bold bg-purple-950/40'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            {Math.round(zoom * 100)}%
          </button>
          <button
            onClick={zoomIn}
            title="Zoom In"
            className="p-1 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
          >
            <ZoomIn className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={fitToHeight}
            title="Fit to Height"
            className={`p-1 rounded-md transition-colors ${
              isFitHeight
                ? 'text-purple-400 bg-purple-950/60 border border-purple-800/50'
                : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
            }`}
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Right Controls: Refresh / Re-render */}
      <div className="flex items-center gap-2">
        <button
          onClick={() => refreshCatalog()}
          title="Refresh preview catalog"
          className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium rounded-lg bg-neutral-900 hover:bg-neutral-800 text-neutral-300 border border-neutral-800 transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-purple-400' : ''}`} />
          <span className="hidden sm:inline">Refresh</span>
        </button>

        <button
          onClick={() => triggerRender()}
          disabled={!activePreview}
          title="Re-render active preview"
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-200 border border-neutral-700/50 transition-colors disabled:opacity-40"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span className="hidden sm:inline">Re-render</span>
        </button>
      </div>
    </header>
  );
};
