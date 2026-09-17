import {
  DaemonStatus,
  LayoutHierarchy,
  PreviewCatalog,
  PreviewItem,
  RenderPreviewResult,
} from '../types.js';

export interface DaemonClientOptions {
  baseUrl?: string;
  timeoutMs?: number;
  customFetch?: typeof fetch;
}

export class CompositorDaemonError extends Error {
  constructor(
    message: string,
    public readonly statusCode?: number,
    public readonly isConnectionRefused: boolean = false
  ) {
    super(message);
    this.name = 'CompositorDaemonError';
  }
}

export class DaemonClient {
  private baseUrl: string;
  private timeoutMs: number;
  private fetchFn: typeof fetch;

  constructor(options: DaemonClientOptions = {}) {
    this.baseUrl = (
      options.baseUrl ??
      process.env.COMPOSITOR_DAEMON_URL ??
      'http://127.0.0.1:3001'
    ).replace(/\/$/, '');
    this.timeoutMs = options.timeoutMs ?? 15000;
    this.fetchFn = options.customFetch ?? globalThis.fetch;
  }

  public getBaseUrl(): string {
    return this.baseUrl;
  }

  private async request<T>(
    endpoint: string,
    init: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);

    try {
      const response = await this.fetchFn(url, {
        ...init,
        signal: controller.signal,
      });

      if (!response.ok) {
        let errorBody = '';
        try {
          errorBody = await response.text();
        } catch {
          // ignore parsing error
        }
        throw new CompositorDaemonError(
          `Compositor daemon returned HTTP ${response.status} for ${endpoint}: ${errorBody || response.statusText}`,
          response.status
        );
      }

      return (await response.json()) as T;
    } catch (err: unknown) {
      if (err instanceof CompositorDaemonError) {
        throw err;
      }

      const isConnRefused =
        (err instanceof Error &&
          (err.message.includes('ECONNREFUSED') ||
            err.message.includes('fetch failed') ||
            err.name === 'TypeError' ||
            err.name === 'AbortError')) ||
        false;

      if (isConnRefused) {
        throw new CompositorDaemonError(
          `Unable to connect to Compositor daemon at ${this.baseUrl}. ` +
            `Ensure the daemon is running (e.g. 'npm run start' in cli/) and port 3001 is accessible. ` +
            `Details: ${err instanceof Error ? err.message : String(err)}`,
          undefined,
          true
        );
      }

      throw new CompositorDaemonError(
        `Error requesting ${endpoint} from Compositor daemon: ${err instanceof Error ? err.message : String(err)}`
      );
    } finally {
      clearTimeout(timer);
    }
  }

  public async isOnline(): Promise<boolean> {
    try {
      await this.getStatus();
      return true;
    } catch {
      return false;
    }
  }

  public async getStatus(): Promise<DaemonStatus> {
    return this.request<DaemonStatus>('/api/daemon/status');
  }

  public async listPreviews(filter?: string): Promise<PreviewItem[]> {
    const catalog = await this.request<PreviewCatalog>('/api/previews');
    const items = Object.values(catalog.previews ?? {});

    if (!filter || filter.trim() === '') {
      return items;
    }

    const query = filter.toLowerCase().trim();
    return items.filter((item) => {
      const idMatch = item.id.toLowerCase().includes(query);
      const fnMatch = item.definition.functionName.toLowerCase().includes(query);
      const pkgMatch = item.definition.packageName.toLowerCase().includes(query);
      const moduleMatch = item.module.toLowerCase().includes(query);
      const fileMatch = item.definition.filePath.toLowerCase().includes(query);
      const groupMatch = item.definition.parameters.group?.toLowerCase().includes(query) ?? false;
      const nameMatch = item.definition.parameters.name?.toLowerCase().includes(query) ?? false;

      return (
        idMatch ||
        fnMatch ||
        pkgMatch ||
        moduleMatch ||
        fileMatch ||
        groupMatch ||
        nameMatch
      );
    });
  }

  public async getPreview(previewId: string): Promise<PreviewItem> {
    const encoded = encodeURIComponent(previewId);
    return this.request<PreviewItem>(`/api/previews/${encoded}`);
  }

  public async renderPreview(
    previewId: string,
    options?: { theme?: string; fontScale?: number; pollIntervalMs?: number; timeoutMs?: number }
  ): Promise<RenderPreviewResult> {
    const encoded = encodeURIComponent(previewId);
    const postEndpoint = `/api/previews/${encoded}/render`;

    await this.request<{ success: boolean; previewId: string }>(postEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(options ?? {}),
    });

    // Poll until complete or timeout
    const pollInterval = options?.pollIntervalMs ?? 150;
    const timeout = options?.timeoutMs ?? this.timeoutMs;
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
      await new Promise((resolve) => setTimeout(resolve, pollInterval));
      const current = await this.getPreview(previewId);

      if (current.status === 'Rendered') {
        return {
          success: true,
          previewId: current.id,
          status: current.status,
          durationMs: current.durationMs ?? Date.now() - startTime,
          imagePath: current.imagePath,
          imageUrl: current.imageUrl,
        };
      }

      if (current.status === 'Error') {
        return {
          success: false,
          previewId: current.id,
          status: current.status,
          durationMs: current.durationMs ?? Date.now() - startTime,
          error: current.errorDetails ?? 'Preview render failed with an unspecified error',
        };
      }
    }

    return {
      success: false,
      previewId,
      status: 'Rendering',
      durationMs: Date.now() - startTime,
      error: `Render request for preview "${previewId}" timed out after ${timeout}ms.`,
    };
  }

  public async getPreviewImageBuffer(previewId: string): Promise<Buffer> {
    const encoded = encodeURIComponent(previewId);
    const url = `${this.baseUrl}/api/previews/${encoded}/image`;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);

    try {
      const response = await this.fetchFn(url, { signal: controller.signal });
      if (!response.ok) {
        throw new CompositorDaemonError(
          `Failed to fetch preview image (HTTP ${response.status}): preview "${previewId}" may not be rendered yet.`,
          response.status
        );
      }
      const arrayBuffer = await response.arrayBuffer();
      return Buffer.from(arrayBuffer);
    } catch (err: unknown) {
      if (err instanceof CompositorDaemonError) {
        throw err;
      }
      throw new CompositorDaemonError(
        `Failed to retrieve image for preview "${previewId}": ${err instanceof Error ? err.message : String(err)}`
      );
    } finally {
      clearTimeout(timer);
    }
  }

  public async getPreviewImageBase64(previewId: string): Promise<string> {
    const buffer = await this.getPreviewImageBuffer(previewId);
    return buffer.toString('base64');
  }

  public async getLayoutHierarchy(previewId: string): Promise<LayoutHierarchy> {
    const encoded = encodeURIComponent(previewId);
    return this.request<LayoutHierarchy>(`/api/previews/${encoded}/hierarchy`);
  }
}
