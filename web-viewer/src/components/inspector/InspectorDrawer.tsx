import React from 'react';
import { X, Box, Ruler, Tag, ShieldCheck } from 'lucide-react';
import { usePreviewConfig } from '../../context/PreviewConfigContext';

export const InspectorDrawer: React.FC = () => {
  const { selectedNode, setSelectedNode, inspectorEnabled } = usePreviewConfig();

  if (!inspectorEnabled || !selectedNode) {
    return null;
  }

  const width = Math.round(selectedNode.bounds.right - selectedNode.bounds.left);
  const height = Math.round(selectedNode.bounds.bottom - selectedNode.bounds.top);

  return (
    <aside
      data-testid="inspector-drawer"
      className="w-80 bg-neutral-900/95 backdrop-blur-md border-l border-neutral-800 flex flex-col h-full z-20 shadow-2xl select-none animate-in slide-in-from-right duration-200"
    >
      {/* Drawer Header */}
      <div className="p-3 border-b border-neutral-800 flex items-center justify-between">
        <div className="flex items-center gap-2 min-w-0">
          <Box className="w-4 h-4 text-purple-400 shrink-0" />
          <div className="min-w-0">
            <div className="text-xs font-bold text-neutral-100 truncate">{selectedNode.name}</div>
            <div className="text-[10px] font-mono text-neutral-500 truncate">
              {selectedNode.qualifiedName}
            </div>
          </div>
        </div>
        <button
          onClick={() => setSelectedNode(null)}
          title="Close Inspector Drawer"
          className="p-1 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-3 space-y-4">
        {/* Geometry & Bounds */}
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-neutral-300">
            <Ruler className="w-3.5 h-3.5 text-blue-400" />
            <span>Layout Geometry</span>
          </div>

          <div className="grid grid-cols-2 gap-2 bg-neutral-950 p-2.5 rounded-lg border border-neutral-800 text-xs">
            <div>
              <div className="text-[10px] text-neutral-500">Width</div>
              <div className="font-mono text-neutral-200">{width} dp</div>
            </div>
            <div>
              <div className="text-[10px] text-neutral-500">Height</div>
              <div className="font-mono text-neutral-200">{height} dp</div>
            </div>
            <div>
              <div className="text-[10px] text-neutral-500">X (Left)</div>
              <div className="font-mono text-neutral-200">{Math.round(selectedNode.bounds.left)} dp</div>
            </div>
            <div>
              <div className="text-[10px] text-neutral-500">Y (Top)</div>
              <div className="font-mono text-neutral-200">{Math.round(selectedNode.bounds.top)} dp</div>
            </div>
          </div>
        </div>

        {/* Padding Box Model */}
        {selectedNode.padding && (
          <div className="space-y-2">
            <div className="text-xs font-semibold text-neutral-300">Padding Metrics</div>
            <div className="bg-neutral-950 p-3 rounded-lg border border-neutral-800 flex flex-col items-center justify-center text-xs font-mono">
              <div className="text-[10px] text-neutral-500 mb-1">
                Top: {selectedNode.padding.top} dp
              </div>
              <div className="flex items-center justify-between w-full px-4 py-1.5 bg-neutral-900 rounded-md border border-neutral-800/80">
                <span className="text-[10px] text-neutral-500">Left: {selectedNode.padding.left} dp</span>
                <span className="text-purple-400 text-xs font-semibold">{selectedNode.name}</span>
                <span className="text-[10px] text-neutral-500">Right: {selectedNode.padding.right} dp</span>
              </div>
              <div className="text-[10px] text-neutral-500 mt-1">
                Bottom: {selectedNode.padding.bottom} dp
              </div>
            </div>
          </div>
        )}

        {/* Semantics & Accessibility */}
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-neutral-300">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
            <span>Accessibility & Semantics</span>
          </div>

          {selectedNode.semantics && Object.keys(selectedNode.semantics).length > 0 ? (
            <div className="bg-neutral-950 p-2.5 rounded-lg border border-neutral-800 space-y-1.5 text-xs">
              {Object.entries(selectedNode.semantics).map(([key, value]) => (
                <div key={key} className="flex items-start justify-between gap-2 border-b border-neutral-900 pb-1 last:border-0 last:pb-0">
                  <span className="text-neutral-500 font-mono text-[11px]">{key}:</span>
                  <span className="text-neutral-200 font-mono text-[11px] text-right truncate">
                    {Array.isArray(value) ? value.join(', ') : String(value)}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-xs text-neutral-500 italic bg-neutral-950 p-2.5 rounded-lg border border-neutral-800">
              No explicit semantic node properties declared.
            </div>
          )}
        </div>

        {/* Raw Bounds Coordinates */}
        <div className="space-y-1.5">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-neutral-400">
            <Tag className="w-3.5 h-3.5 text-neutral-500" />
            <span>Raw Bounds Tuple</span>
          </div>
          <div className="bg-neutral-950 px-2.5 py-1.5 rounded-md border border-neutral-800/80 font-mono text-[11px] text-purple-300">
            [{selectedNode.bounds.left}, {selectedNode.bounds.top},{' '}
            {selectedNode.bounds.right}, {selectedNode.bounds.bottom}]
          </div>
        </div>
      </div>
    </aside>
  );
};
