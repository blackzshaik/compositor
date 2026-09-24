import React from 'react';
import { DeviceProfileId, Orientation, ThemeMode } from '../../types/preview';
import { DEVICE_PROFILES } from './deviceProfiles';

interface DeviceFrameProps {
  imageUrl: string;
  title: string;
  isLoading?: boolean;
  profileId?: DeviceProfileId;
  orientation?: Orientation;
  themeMode?: ThemeMode;
  children?: React.ReactNode;
}

export const DeviceFrame: React.FC<DeviceFrameProps> = ({
  imageUrl,
  title,
  isLoading = false,
  profileId = 'pixel-8',
  orientation = 'portrait',
  themeMode = 'dark',
  children,
}) => {
  const profile = DEVICE_PROFILES[profileId] || DEVICE_PROFILES['pixel-8'];
  const isLandscape = orientation === 'landscape';

  // Compute inner screen dimensions in px
  const width = isLandscape ? profile.heightDp : profile.widthDp;
  const height = isLandscape ? profile.widthDp : profile.heightDp;

  const bezelPx = parseInt(profile.bezelWidth, 10) || 0;
  const shellWidth = width + bezelPx * 2;
  const shellHeight = height + bezelPx * 2;

  // Render camera cutouts
  const renderCutout = () => {
    if (!profile.hasCameraCutout || profile.id === 'frameless') return null;

    if (profile.cutoutType === 'tablet-camera') {
      return (
        <div
          aria-label="Tablet camera"
          className={`absolute bg-neutral-950 rounded-full z-20 border border-neutral-700/50 shadow-inner ${
            isLandscape
              ? 'top-1.5 left-1/2 -translate-x-1/2 w-2.5 h-2.5'
              : 'top-1/2 left-1.5 -translate-y-1/2 w-2.5 h-2.5'
          }`}
        />
      );
    }

    // Default smartphone punchhole
    return (
      <div
        aria-label="Camera cutout"
        className={`absolute bg-neutral-950 rounded-full z-20 ring-2 ring-neutral-800 shadow-inner ${
          isLandscape
            ? 'left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5'
            : 'top-2.5 left-1/2 -translate-x-1/2 w-3.5 h-3.5'
        }`}
      />
    );
  };

  // Render Home Indicator
  const renderHomeIndicator = () => {
    if (profile.id === 'frameless') return null;

    return (
      <div
        aria-label="Home indicator"
        className={`absolute bg-neutral-500/70 rounded-full z-20 pointer-events-none ${
          isLandscape
            ? 'right-1.5 top-1/2 -translate-y-1/2 w-1 h-24'
            : 'bottom-1.5 left-1/2 -translate-x-1/2 w-28 h-1'
        }`}
      />
    );
  };

  const fallbackSvg =
    'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="600" viewBox="0 0 300 600"><rect fill="%23111" width="300" height="600"/><text fill="%23888" x="50%" y="50%" text-anchor="middle" font-family="sans-serif">Rendering preview...</text></svg>';

  const displayUrl = imageUrl || fallbackSvg;
  const screenBg = themeMode === 'light' ? 'bg-white' : 'bg-neutral-950';

  if (profile.id === 'frameless') {
    return (
      <div
        data-testid="device-frame-frameless"
        className="relative flex flex-col items-center group transition-all duration-300"
      >
        <div
          style={{ width: `${width}px`, height: `${height}px` }}
          className={`relative ${screenBg} rounded-xl border border-neutral-700/60 shadow-2xl overflow-hidden flex items-center justify-center transition-all duration-300`}
        >
          {isLoading && (
            <div className="absolute inset-0 bg-neutral-950/70 backdrop-blur-xs flex items-center justify-center z-30">
              <div className="w-8 h-8 border-3 border-purple-500 border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          <img
            key={imageUrl}
            src={displayUrl}
            alt={title}
            onError={(e) => {
              (e.target as HTMLImageElement).src = fallbackSvg;
            }}
            className="w-full h-full object-contain pointer-events-auto select-none"
          />

          {children}
        </div>
      </div>
    );
  }

  return (
    <div
      data-testid={`device-frame-${profile.id}`}
      className="relative flex flex-col items-center group transition-all duration-300"
    >
      {/* Smartphone / Tablet Outer Shell */}
      <div
        style={{
          width: `${shellWidth}px`,
          height: `${shellHeight}px`,
          borderRadius: profile.borderRadius,
          borderWidth: profile.bezelWidth,
        }}
        className="relative bg-neutral-950 border-neutral-800 shadow-2xl shadow-black/80 overflow-hidden ring-1 ring-white/10 flex flex-col transition-all duration-300"
      >
        {/* Hardware Antenna / Edge Glare highlight */}
        <div className="absolute inset-0 pointer-events-none rounded-[inherit] ring-1 ring-inset ring-white/5 z-20" />

        {/* Camera Cutout */}
        {renderCutout()}

        {/* Screen Content Viewport */}
        <div
          style={{ width: `${width}px`, height: `${height}px` }}
          className={`relative ${screenBg} overflow-hidden flex items-center justify-center transition-colors duration-200`}
        >
          {isLoading && (
            <div className="absolute inset-0 bg-neutral-950/70 backdrop-blur-xs flex items-center justify-center z-30">
              <div className="w-8 h-8 border-3 border-purple-500 border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          <img
            key={imageUrl}
            src={displayUrl}
            alt={title}
            onError={(e) => {
              (e.target as HTMLImageElement).src = fallbackSvg;
            }}
            className="w-full h-full object-contain pointer-events-auto select-none transition-opacity duration-200"
          />

          {children}
        </div>

        {/* Home Indicator */}
        {renderHomeIndicator()}
      </div>
    </div>
  );
};
