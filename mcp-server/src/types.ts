export interface PreviewParameters {
  name?: string;
  group?: string;
  widthDp?: number;
  heightDp?: number;
  uiMode?: string | number;
  fontScale?: number;
  showBackground?: boolean;
  backgroundColor?: string | number;
}

export interface PreviewDefinition {
  functionName: string;
  enclosingClass?: string;
  packageName: string;
  parameters: PreviewParameters;
  line: number;
  filePath: string;
}

export type PreviewRenderStatus = 'Pending' | 'Rendering' | 'Rendered' | 'Error';

export interface PreviewItem {
  id: string;
  module: string;
  definition: PreviewDefinition;
  status: PreviewRenderStatus;
  durationMs?: number;
  lastRenderedAt?: number;
  errorDetails?: string;
  imagePath?: string;
  imageUrl?: string;
}

export interface PreviewCatalog {
  previews: Record<string, PreviewItem>;
  byModule: Record<string, string[]>;
  byFile: Record<string, string[]>;
  byGroup: Record<string, string[]>;
  totalCount: number;
}

export interface BoundsRect {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export interface LayoutNode {
  id: string;
  name: string;
  qualifiedName: string;
  bounds: BoundsRect;
  dpBounds?: BoundsRect;
  children?: LayoutNode[];
  semantics?: Record<string, unknown>;
  padding?: {
    left: number;
    top: number;
    right: number;
    bottom: number;
  };
}

export interface LayoutHierarchy {
  root: LayoutNode;
  density: number;
  viewWidth: number;
  viewHeight: number;
}

export interface DaemonStatus {
  status: 'running' | 'idle' | 'stopped';
  projectRoot: string;
  watcherActive: boolean;
  totalPreviews: number;
  uptimeSeconds: number;
  memoryUsage: {
    rss: number;
    heapTotal: number;
    heapUsed: number;
    external: number;
  };
}

export interface RenderPreviewResult {
  success: boolean;
  previewId: string;
  status: PreviewRenderStatus;
  durationMs?: number;
  imagePath?: string;
  imageUrl?: string;
  error?: string;
}

export interface VisualDiffResult {
  hasVisualDifferences: boolean;
  differencePercentage: number;
  diffImageBase64?: string;
  pixelCountDifferent: number;
  totalPixels: number;
  summary: string;
}
