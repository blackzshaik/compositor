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

export interface FileChangeEvent {
  type: 'added' | 'modified' | 'deleted';
  filePath: string;
  timestamp: number;
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
