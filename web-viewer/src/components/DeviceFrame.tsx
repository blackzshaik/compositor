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
