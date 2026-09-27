import React, { useEffect, useState, useMemo } from 'react';
import { usePreviewConfig } from '../../context/PreviewConfigContext';
import { usePreviewCatalog } from '../../context/PreviewCatalogContext';
import { LayoutHierarchy, LayoutNode } from '../../types/preview';

interface InspectorOverlayProps {
  containerWidth: number;
  containerHeight: number;
}

// Fallback hierarchy for GreetingPreview if backend hasn't generated hierarchy.json
const DEFAULT_HIERARCHY: LayoutHierarchy = {
  density: 2.75,
  viewWidth: 360,
  viewHeight: 780,
  root: {
    id: 'root-surface',
    name: 'Surface',
    qualifiedName: 'androidx.compose.material3.Surface',
    bounds: { left: 0, top: 0, right: 360, bottom: 780 },
    dpBounds: { left: 0, top: 0, right: 360, bottom: 780 },
    padding: { left: 16, top: 24, right: 16, bottom: 24 },
    children: [
      {
        id: 'node-column',
        name: 'Column',
        qualifiedName: 'androidx.compose.foundation.layout.Column',
        bounds: { left: 16, top: 200, right: 344, bottom: 450 },
        dpBounds: { left: 16, top: 200, right: 344, bottom: 450 },
        padding: { left: 8, top: 8, right: 8, bottom: 8 },
        children: [
          {
            id: 'node-greeting-text',
            name: 'Text',
            qualifiedName: 'androidx.compose.material3.Text',
            bounds: { left: 32, top: 220, right: 328, bottom: 260 },
            dpBounds: { left: 32, top: 220, right: 328, bottom: 260 },
            semantics: {
              text: ['Hello Android!'],
              role: 'Text',
              testTag: 'greeting_title',
            },
          },
          {
            id: 'node-action-btn',
            name: 'Button',
            qualifiedName: 'androidx.compose.material3.Button',
            bounds: { left: 80, top: 290, right: 280, bottom: 340 },
            dpBounds: { left: 80, top: 290, right: 280, bottom: 340 },
            semantics: {
              role: 'Button',
              contentDescription: 'Click me to interact',
              isClickable: true,
            },
            children: [
              {
                id: 'node-btn-text',
                name: 'Text',
                qualifiedName: 'androidx.compose.material3.Text',
                bounds: { left: 110, top: 304, right: 250, bottom: 326 },
                dpBounds: { left: 110, top: 304, right: 250, bottom: 326 },
                semantics: {
                  text: ['Click Me'],
                },
              },
            ],
          },
        ],
      },
    ],
  },
};

interface RawElementBounds {
  className: string;
  left: number;
  top: number;
  width: number;
  height: number;
  children?: RawElementBounds[];
}

function elementBoundsToNode(b: RawElementBounds, id = 'node-0'): LayoutNode {
  return {
    id,
    name: b.className.split('.').pop() || b.className,
    qualifiedName: b.className,
    bounds: { left: b.left, top: b.top, right: b.left + b.width, bottom: b.top + b.height },
    dpBounds: { left: b.left, top: b.top, right: b.left + b.width, bottom: b.top + b.height },
    children: b.children?.map((c: RawElementBounds, i: number) => elementBoundsToNode(c, `${id}-${i}`)) || [],
  };
}

export const InspectorOverlay: React.FC<InspectorOverlayProps> = ({
  containerWidth,
  containerHeight,
}) => {
  const {
    inspectorEnabled,
    selectedNode,
    setSelectedNode,
    hoveredNode,
    setHoveredNode,
  } = usePreviewConfig();

  const { activePreview, httpBase } = usePreviewCatalog();
  const [hierarchy, setHierarchy] = useState<LayoutHierarchy>(DEFAULT_HIERARCHY);

  // Attempt to load rootBounds from preview catalog or fetch hierarchy.json
  useEffect(() => {
    if (!activePreview) return;
    let isMounted = true;

    if (activePreview.rootBounds) {
      setHierarchy({
        density: 2.75,
        viewWidth: activePreview.rootBounds.width || containerWidth,
        viewHeight: activePreview.rootBounds.height || containerHeight,
        root: elementBoundsToNode(activePreview.rootBounds),
      });
      return;
    }

    async function fetchHierarchy() {
      try {
        const res = await fetch(
          `${httpBase}/api/previews/${encodeURIComponent(activePreview!.id)}/hierarchy.json`
        );
        if (res.ok) {
          const data = await res.json();
          if (isMounted && data.root) {
            setHierarchy(data);
          }
        } else {
          // Fallback hierarchy with active composable name
          if (isMounted) {
            setHierarchy({
              ...DEFAULT_HIERARCHY,
              root: {
                ...DEFAULT_HIERARCHY.root,
                name: activePreview!.definition.functionName,
                qualifiedName: `${activePreview!.definition.packageName}.${activePreview!.definition.functionName}`,
              },
            });
          }
        }
      } catch {
        if (isMounted) {
          setHierarchy({
            ...DEFAULT_HIERARCHY,
            root: {
              ...DEFAULT_HIERARCHY.root,
              name: activePreview!.definition.functionName,
            },
          });
        }
      }
    }

    fetchHierarchy();

    return () => {
      isMounted = false;
    };
  }, [activePreview, httpBase]);

  // Flatten nodes for rendering overlay boxes
  const allNodes = useMemo(() => {
    const list: LayoutNode[] = [];
    function collect(node: LayoutNode) {
      list.push(node);
      if (node.children) {
        for (const child of node.children) {
          collect(child);
        }
      }
    }
    if (hierarchy?.root) {
      collect(hierarchy.root);
    }
    return list;
  }, [hierarchy]);

  if (!inspectorEnabled) {
    return null;
  }

  // Calculate coordinate scaling factor between natural preview dimensions and container dimensions
  const scaleX = containerWidth / (hierarchy.viewWidth || 360);
  const scaleY = containerHeight / (hierarchy.viewHeight || 780);

  return (
    <div
      data-testid="inspector-overlay"
      className="absolute inset-0 z-30 pointer-events-auto"
      onClick={() => setSelectedNode(null)}
    >
      {allNodes.map((node) => {
        const left = node.bounds.left * scaleX;
        const top = node.bounds.top * scaleY;
        const width = (node.bounds.right - node.bounds.left) * scaleX;
        const height = (node.bounds.bottom - node.bounds.top) * scaleY;

        const isHovered = hoveredNode?.id === node.id;
        const isSelected = selectedNode?.id === node.id;

        return (
          <div
            key={node.id}
            style={{
              left: `${left}px`,
              top: `${top}px`,
              width: `${width}px`,
              height: `${height}px`,
            }}
            onClick={(e) => {
              e.stopPropagation();
              setSelectedNode(node);
            }}
            onMouseEnter={(e) => {
              e.stopPropagation();
              setHoveredNode(node);
            }}
            onMouseLeave={() => {
              if (hoveredNode?.id === node.id) {
                setHoveredNode(null);
              }
            }}
            className={`absolute transition-colors cursor-crosshair ${
              isSelected
                ? 'border-2 border-purple-400 bg-purple-500/20 z-40'
                : isHovered
                ? 'border border-cyan-400 bg-cyan-400/15 z-30'
                : 'border border-transparent hover:border-cyan-400/50 hover:bg-cyan-500/10'
            }`}
          >
            {/* Element Tag Label on Hover or Selection */}
            {(isHovered || isSelected) && (
              <div
                className={`absolute -top-5 left-0 px-1.5 py-0.5 text-[10px] font-mono font-semibold rounded-xs shadow-md whitespace-nowrap z-50 ${
                  isSelected ? 'bg-purple-600 text-white' : 'bg-cyan-600 text-white'
                }`}
              >
                <span>{node.name}</span>
                <span className="opacity-80 ml-1">
                  ({Math.round(node.bounds.right - node.bounds.left)}×
                  {Math.round(node.bounds.bottom - node.bounds.top)})
                </span>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};
