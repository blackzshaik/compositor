import fs from 'node:fs';
import path from 'node:path';
import {
  PreviewCatalog,
  PreviewDefinition,
  PreviewItem,
  PreviewRenderStatus,
} from '../models/preview.js';

export class PreviewRegistry {
  private items = new Map<string, PreviewItem>();
  private filePath: string | null = null;

  constructor(filePath?: string) {
    if (filePath) {
      this.filePath = filePath;
      this.loadFromFile(filePath);
    }
  }

  /**
   * Generates a deterministic preview ID.
   */
  public static generateId(moduleName: string, def: PreviewDefinition): string {
    const pkgPrefix = def.packageName ? `${def.packageName}.` : '';
    const classPrefix = def.enclosingClass ? `${def.enclosingClass}.` : '';
    const target = `${pkgPrefix}${classPrefix}${def.functionName}`;
    const tag = def.parameters.name ? `#${def.parameters.name}` : '';
    return `${moduleName}:${target}${tag}`;
  }

  /**
   * Updates or registers previews for a specific source file.
   */
  public updateFilePreviews(
    fileSourcePath: string,
    definitions: PreviewDefinition[],
    moduleName = 'app'
  ): PreviewItem[] {
    const normalizedPath = path.normalize(fileSourcePath);
    const existingIdsForFile: string[] = [];

    for (const [id, item] of this.items.entries()) {
      if (path.normalize(item.definition.filePath) === normalizedPath) {
        existingIdsForFile.push(id);
      }
    }

    const currentIds = new Set<string>();
    const updatedItems: PreviewItem[] = [];

    for (const def of definitions) {
      const id = PreviewRegistry.generateId(moduleName, def);
      currentIds.add(id);

      const existing = this.items.get(id);
      const item: PreviewItem = {
        id,
        module: moduleName,
        definition: def,
        status: existing ? existing.status : 'Pending',
        durationMs: existing?.durationMs,
        lastRenderedAt: existing?.lastRenderedAt,
        errorDetails: existing?.errorDetails,
        imagePath: existing?.imagePath,
        imageUrl: existing?.imageUrl,
      };

      this.items.set(id, item);
      updatedItems.push(item);
    }

    // Remove obsolete previews that were deleted from this file
    for (const oldId of existingIdsForFile) {
      if (!currentIds.has(oldId)) {
        this.items.delete(oldId);
      }
    }

    this.persist();
    return updatedItems;
  }

  /**
   * Removes all previews associated with a removed source file.
   */
  public removeFile(fileSourcePath: string): number {
    const normalizedPath = path.normalize(fileSourcePath);
    let removedCount = 0;

    for (const [id, item] of this.items.entries()) {
      if (path.normalize(item.definition.filePath) === normalizedPath) {
        this.items.delete(id);
        removedCount++;
      }
    }

    if (removedCount > 0) {
      this.persist();
    }
    return removedCount;
  }

  /**
   * Updates rendering state and diagnostic info for a preview.
   */
  public updateStatus(
    id: string,
    status: PreviewRenderStatus,
    details?: {
      durationMs?: number;
      errorDetails?: string;
      imagePath?: string;
      imageUrl?: string;
    }
  ): PreviewItem | undefined {
    const item = this.items.get(id);
    if (!item) return undefined;

    item.status = status;
    if (status === 'Rendered') {
      item.lastRenderedAt = Date.now();
      item.errorDetails = undefined;
    }

    if (details?.durationMs !== undefined) item.durationMs = details.durationMs;
    if (details?.errorDetails !== undefined) item.errorDetails = details.errorDetails;
    if (details?.imagePath !== undefined) item.imagePath = details.imagePath;
    if (details?.imageUrl !== undefined) item.imageUrl = details.imageUrl;

    this.persist();
    return item;
  }

  public getPreview(id: string): PreviewItem | undefined {
    return this.items.get(id);
  }

  public getAllPreviews(): PreviewItem[] {
    return Array.from(this.items.values());
  }

  /**
   * Returns a categorized catalog of previews.
   */
  public getCatalog(): PreviewCatalog {
    const catalog: PreviewCatalog = {
      previews: {},
      byModule: {},
      byFile: {},
      byGroup: {},
      totalCount: this.items.size,
    };

    for (const [id, item] of this.items.entries()) {
      catalog.previews[id] = item;

      // Group by module
      if (!catalog.byModule[item.module]) {
        catalog.byModule[item.module] = [];
      }
      catalog.byModule[item.module].push(id);

      // Group by file
      const filePath = item.definition.filePath;
      if (!catalog.byFile[filePath]) {
        catalog.byFile[filePath] = [];
      }
      catalog.byFile[filePath].push(id);

      // Group by preview group/tag
      const groupName = item.definition.parameters.group ?? 'Default';
      if (!catalog.byGroup[groupName]) {
        catalog.byGroup[groupName] = [];
      }
      catalog.byGroup[groupName].push(id);
    }

    return catalog;
  }

  /**
   * Loads index from JSON file if it exists.
   */
  public loadFromFile(filePath: string): boolean {
    this.filePath = filePath;
    try {
      if (fs.existsSync(filePath)) {
        const raw = fs.readFileSync(filePath, 'utf-8');
        const data = JSON.parse(raw) as { previews?: Record<string, PreviewItem> };
        if (data.previews) {
          this.items.clear();
          for (const [id, item] of Object.entries(data.previews)) {
            this.items.set(id, item);
          }
          return true;
        }
      }
    } catch (err) {
      console.warn(`[PreviewRegistry] Failed to read ${filePath}:`, err);
    }
    return false;
  }

  /**
   * Atomically writes index to previews.json.
   */
  public persist(): void {
    if (!this.filePath) return;

    try {
      const dir = path.dirname(this.filePath);
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }

      const catalog = this.getCatalog();
      const content = JSON.stringify(catalog, null, 2);
      const tempPath = `${this.filePath}.tmp.${Date.now()}`;

      fs.writeFileSync(tempPath, content, 'utf-8');
      fs.renameSync(tempPath, this.filePath);
    } catch (err) {
      console.warn(`[PreviewRegistry] Failed to persist to ${this.filePath}:`, err);
    }
  }

  public clear(): void {
    this.items.clear();
    this.persist();
  }
}
