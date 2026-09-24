import React from 'react';
import { PreviewConfigProvider, usePreviewConfig } from './context/PreviewConfigContext';
import { PreviewCatalogProvider, usePreviewCatalog } from './context/PreviewCatalogContext';
import { TopBar } from './components/controls/TopBar';
import { Sidebar } from './components/sidebar/Sidebar';
import { DeviceFrame } from './components/device/DeviceFrame';
import { CanvasViewport } from './components/device/CanvasViewport';
import { MatrixGrid } from './components/matrix/MatrixGrid';
import { InspectorOverlay } from './components/inspector/InspectorOverlay';
import { InspectorDrawer } from './components/inspector/InspectorDrawer';
import { ErrorBoundary } from './components/diagnostics/ErrorBoundary';
import { ErrorOverlay } from './components/diagnostics/ErrorOverlay';
import { DEVICE_PROFILES } from './components/device/deviceProfiles';
import { resolvePreviewUrl } from './utils/url';

const AppContent: React.FC = () => {
  const {
    deviceProfile,
    orientation,
    viewMode,
    themeMode,
  } = usePreviewConfig();

  const { activePreview, httpBase } = usePreviewCatalog();

  const profile = DEVICE_PROFILES[deviceProfile] || DEVICE_PROFILES['pixel-8'];
  const isLandscape = orientation === 'landscape';
  const width = isLandscape ? profile.heightDp : profile.widthDp;
  const height = isLandscape ? profile.widthDp : profile.heightDp;

  const imageUrl = resolvePreviewUrl(
    activePreview ? activePreview.imageUrl : null,
    httpBase,
    activePreview ? activePreview.lastRenderedAt : null
  );

  const title = activePreview
    ? activePreview.definition.parameters.name || activePreview.definition.functionName
    : 'GreetingPreview';

  const isLoading = activePreview?.status === 'Rendering';

  return (
    <div className="flex flex-col h-screen w-screen bg-neutral-950 text-neutral-100 overflow-hidden font-sans">
      {/* Top Controls Bar */}
      <TopBar />

      {/* Main Studio Viewport */}
      <div className="flex-1 flex overflow-hidden relative">
        {/* Left Navigation Sidebar */}
        <Sidebar />

        {/* Center Interactive Canvas */}
        <CanvasViewport>
          {viewMode === 'single' ? (
            <div className="flex flex-col items-center">
              <DeviceFrame
                imageUrl={imageUrl}
                title={title}
                isLoading={isLoading}
                profileId={deviceProfile}
                orientation={orientation}
                themeMode={themeMode}
              >
                {/* Element Bounds Inspector Overlay */}
                <InspectorOverlay containerWidth={width} containerHeight={height} />

                {/* Inline Error Diagnostics Overlay */}
                <ErrorOverlay />
              </DeviceFrame>
            </div>
          ) : (
            <MatrixGrid />
          )}
        </CanvasViewport>

        {/* Right Element Inspector Drawer */}
        <InspectorDrawer />
      </div>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <ErrorBoundary fallbackTitle="Compositor Studio Error">
      <PreviewConfigProvider>
        <PreviewCatalogProvider>
          <AppContent />
        </PreviewCatalogProvider>
      </PreviewConfigProvider>
    </ErrorBoundary>
  );
};

export default App;
