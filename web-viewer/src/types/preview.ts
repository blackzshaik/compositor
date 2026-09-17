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

export interface RenderErrorDiagnostic {
  previewId: string;
  title: string;
  message: string;
  filePath?: string;
  line?: number;
  column?: number;
  codeSnippet?: string;
  stackTrace?: string;
  timestamp: number;
}

export type DeviceProfileId = 'pixel-8' | 'galaxy-s24' | 'foldable' | 'tablet-10' | 'frameless';

export interface DeviceProfile {
  id: DeviceProfileId;
  name: string;
  category: 'phone' | 'foldable' | 'tablet' | 'frameless';
  widthDp: number;
  heightDp: number;
  borderRadius: string;
  bezelWidth: string;
  hasCameraCutout: boolean;
  cutoutType?: 'punchhole' | 'island' | 'tablet-camera';
}

export type Orientation = 'portrait' | 'landscape';
export type ThemeMode = 'light' | 'dark';
export type ViewMode = 'single' | 'matrix';
export type MatrixPreset = 'theme' | 'font-scale' | 'group' | 'all';
export type BackdropPattern = 'dark' | 'light' | 'checkerboard';
