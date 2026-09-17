import React, { useState, useRef, useCallback, useEffect } from 'react';
import { ZoomIn, ZoomOut, Maximize2, RotateCcw } from 'lucide-react';
import { usePreviewConfig } from '../../context/PreviewConfigContext';

interface CanvasViewportProps {
  children: React.ReactNode;
}

export const CanvasViewport: React.FC<CanvasViewportProps> = ({ children }) => {
  const {
    zoom,
    setZoom,
    zoomIn,
    zoomOut,
    resetZoom,
    fitToHeight,
    isFitHeight,
    backdrop,
    deviceProfile,
    orientation,
    viewMode,
  } = usePreviewConfig();

  const [pan, setPan] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [isPanning, setIsPanning] = useState<boolean>(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  const dragStartRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });

  // Auto-fit height calculation
  const calculateFitHeight = useCallback(() => {
    if (!containerRef.current || !contentRef.current) return;
    const containerH = containerRef.current.clientHeight;
    const contentH = contentRef.current.clientHeight;

    if (containerH > 50 && contentH > 50) {
      // Allow 48px padding (24px top, 24px bottom) for a comfortable margin
      const availableH = containerH - 48;
      const targetZoom = Math.min(1.25, Math.max(0.25, availableH / contentH));
      const rounded = Math.round(targetZoom * 100) / 100;
      setZoom(rounded);
      setPan({ x: 0, y: 0 });
    }
  }, [setZoom]);

  // When in single view and isFitHeight is active, auto-scale to fit height
  useEffect(() => {
    if (isFitHeight && viewMode === 'single') {
      calculateFitHeight();
    }
  }, [isFitHeight, viewMode, deviceProfile, orientation, calculateFitHeight]);

  // Window resize observer
  useEffect(() => {
    if (!containerRef.current || typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(() => {
      if (isFitHeight && viewMode === 'single') {
        calculateFitHeight();
      }
    });
    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, [isFitHeight, viewMode, calculateFitHeight]);

  const handleMouseDown = (e: React.MouseEvent) => {
    // Left click on background or middle click triggers canvas pan
    if (e.button === 1 || (e.button === 0 && e.target === e.currentTarget)) {
      e.preventDefault();
      setIsPanning(true);
      dragStartRef.current = { x: e.clientX - pan.x, y: e.clientY - pan.y };
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isPanning) return;
    setPan({
      x: e.clientX - dragStartRef.current.x,
      y: e.clientY - dragStartRef.current.y,
    });
  };

  const handleMouseUp = () => {
    setIsPanning(false);
  };

  // Wheel zoom
  const handleWheel = (e: React.WheelEvent) => {
    if (e.ctrlKey || e.metaKey || e.altKey) {
      e.preventDefault();
      const zoomDelta = -e.deltaY * 0.0015;
      setZoom(zoom + zoomDelta);
    }
  };

  const handleResetView = () => {
    resetZoom();
    setPan({ x: 0, y: 0 });
  };

  const handleFitToHeight = () => {
    fitToHeight();
    calculateFitHeight();
  };

  // Backdrop styling class
  const getBackdropClass = () => {
    switch (backdrop) {
      case 'light':
        return 'bg-neutral-200 text-neutral-900 bg-dot-grid-light';
      case 'checkerboard':
        return 'bg-checkerboard text-neutral-100';
      case 'dark':
      default:
        return 'bg-neutral-950 text-neutral-100 bg-dot-grid-dark';
    }
  };

  return (
    <div
      ref={containerRef}
      data-testid="canvas-viewport"
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      onWheel={handleWheel}
      className={`relative flex-1 h-full overflow-hidden flex items-center justify-center select-none cursor-grab active:cursor-grabbing ${getBackdropClass()}`}
    >
      {/* Scaled & Panned Content Container */}
      <div
        ref={contentRef}
        style={{
          transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
          transformOrigin: 'center center',
          transition: isPanning ? 'none' : 'transform 0.15s ease-out',
        }}
        className="pointer-events-auto"
      >
        {children}
      </div>

      {/* Floating Canvas Zoom & Pan Control Bar */}
      <div className="absolute bottom-5 right-5 flex items-center gap-1 bg-neutral-900/90 backdrop-blur-md px-2 py-1.5 rounded-xl border border-neutral-800 shadow-xl text-neutral-300 z-30">
        <button
          onClick={zoomOut}
          title="Zoom Out"
          className="p-1.5 rounded-lg hover:bg-neutral-800 hover:text-white transition-colors"
        >
          <ZoomOut className="w-4 h-4" />
        </button>

        <button
          onClick={handleFitToHeight}
          title={isFitHeight ? 'Fit Height (Active)' : 'Click to Fit Height'}
          className={`text-xs font-mono px-2 min-w-[3.5rem] text-center rounded-md py-0.5 transition-colors ${
            isFitHeight ? 'text-purple-400 bg-purple-950/40 font-bold' : 'text-neutral-400 hover:text-white'
          }`}
        >
          {Math.round(zoom * 100)}%
        </button>

        <button
          onClick={zoomIn}
          title="Zoom In"
          className="p-1.5 rounded-lg hover:bg-neutral-800 hover:text-white transition-colors"
        >
          <ZoomIn className="w-4 h-4" />
        </button>

        <div className="w-px h-4 bg-neutral-800 mx-1" />

        <button
          onClick={handleFitToHeight}
          title="Fit to Height"
          className={`p-1.5 rounded-lg transition-colors ${
            isFitHeight
              ? 'text-purple-400 bg-purple-950/40 border border-purple-800/60'
              : 'hover:bg-neutral-800 hover:text-white'
          }`}
        >
          <Maximize2 className="w-4 h-4" />
        </button>

        <button
          onClick={handleResetView}
          title="Reset Zoom & Pan (100%)"
          className="p-1.5 rounded-lg hover:bg-neutral-800 hover:text-white transition-colors"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
