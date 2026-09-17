import React, { useMemo } from 'react';
import { Maximize2, RefreshCw, Copy, Check, Sun, Moon, Type, Layers } from 'lucide-react';
import { usePreviewCatalog } from '../../context/PreviewCatalogContext';
import { usePreviewConfig } from '../../context/PreviewConfigContext';
import { PreviewItem } from '../../types/preview';
import { resolvePreviewUrl } from '../../utils/url';

interface MatrixCellConfig {
  id: string;
  previewId: string;
  title: string;
  subtitle: string;
  imageUrl: string;
  theme?: 'light' | 'dark';
  scale?: number;
  badgeIcon?: 'theme' | 'font' | 'group';
}

export const MatrixGrid: React.FC = () => {
  const { activePreview, catalog, triggerRender, setActivePreviewId, httpBase } = usePreviewCatalog();
  const { matrixPreset, setViewMode, setFontScale, setThemeMode } = usePreviewConfig();

  const [copiedId, setCopiedId] = React.useState<string | null>(null);

  // Generate cells based on active matrix preset
  const cells: MatrixCellConfig[] = useMemo(() => {
    if (!catalog) return [];

    const basePreview: PreviewItem | null = activePreview || Object.values(catalog.previews)[0] || null;
    if (!basePreview) return [];

    const baseImgUrl = resolvePreviewUrl(basePreview.imageUrl, httpBase);

    if (matrixPreset === 'theme') {
      // Light Mode vs Dark Mode comparison
      // If there are previews with UI_MODE_NIGHT_YES / NO, match them; otherwise render light and dark simulated cards
      return [
        {
          id: `${basePreview.id}-light`,
          previewId: basePreview.id,
          title: 'Light Theme',
          subtitle: 'UI_MODE_NIGHT_NO',
          imageUrl: baseImgUrl,
          theme: 'light',
          badgeIcon: 'theme',
        },
        {
          id: `${basePreview.id}-dark`,
          previewId: basePreview.id,
          title: 'Dark Theme',
          subtitle: 'UI_MODE_NIGHT_YES',
          imageUrl: baseImgUrl,
          theme: 'dark',
          badgeIcon: 'theme',
        },
      ];
    }

    if (matrixPreset === 'font-scale') {
      const scales = [1.0, 1.15, 1.3, 1.5];
      const labels: Record<number, string> = {
        1.0: 'Default (1.0x)',
        1.15: 'Large (1.15x)',
        1.3: 'Extra Large (1.3x)',
        1.5: 'Huge (1.5x)',
      };

      return scales.map((scale) => ({
        id: `${basePreview.id}-font-${scale}`,
        previewId: basePreview.id,
        title: labels[scale] || `${scale}x`,
        subtitle: `FontScale ${scale}x`,
        imageUrl: baseImgUrl,
        scale,
        badgeIcon: 'font',
      }));
    }

    if (matrixPreset === 'group') {
      const groupName = basePreview.definition.parameters.group;
      const groupPreviews = Object.values(catalog.previews).filter((p) =>
        groupName ? p.definition.parameters.group === groupName : true
      );

      return groupPreviews.map((p) => ({
        id: p.id,
        previewId: p.id,
        title: p.definition.parameters.name || p.definition.functionName,
        subtitle: p.definition.parameters.group ? `@Group("${p.definition.parameters.group}")` : p.module,
        imageUrl: resolvePreviewUrl(p.imageUrl, httpBase),
        badgeIcon: 'group',
      }));
    }

    // Default 'all': Render all previews in project
    return Object.values(catalog.previews).map((p) => ({
      id: p.id,
      previewId: p.id,
      title: p.definition.parameters.name || p.definition.functionName,
      subtitle: p.definition.filePath.split(/[/\\]/).pop() || p.module,
      imageUrl: resolvePreviewUrl(p.imageUrl, httpBase),
      badgeIcon: 'group',
    }));
  }, [catalog, activePreview, matrixPreset, httpBase]);

  const handleFocus = (cell: MatrixCellConfig) => {
    setActivePreviewId(cell.previewId);
    if (cell.theme) setThemeMode(cell.theme);
    if (cell.scale) setFontScale(cell.scale);
    setViewMode('single');
  };

  const handleCopy = async (cell: MatrixCellConfig) => {
    try {
      await navigator.clipboard.writeText(cell.imageUrl);
      setCopiedId(cell.id);
      setTimeout(() => setCopiedId(null), 2000);
    } catch (err) {
      console.error('Failed to copy image URL:', err);
    }
  };

  if (cells.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-12 text-neutral-400">
        <Layers className="w-12 h-12 text-neutral-600 mb-3" />
        <p className="text-sm">No preview items available to display in matrix view.</p>
      </div>
    );
  }

  return (
    <div
      data-testid="matrix-grid"
      className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 p-6 max-w-[1920px] mx-auto"
    >
      {cells.map((cell) => {
        return (
          <div
            key={cell.id}
            data-testid="matrix-cell"
            className="flex flex-col bg-neutral-900 border border-neutral-800 rounded-2xl overflow-hidden shadow-xl hover:border-purple-500/50 transition-all duration-200 group"
          >
            {/* Cell Header */}
            <div className="px-4 py-2.5 bg-neutral-950/80 border-b border-neutral-800 flex items-center justify-between">
              <div className="flex items-center gap-2 min-w-0">
                {cell.badgeIcon === 'theme' &&
                  (cell.theme === 'light' ? (
                    <Sun className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                  ) : (
                    <Moon className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                  ))}
                {cell.badgeIcon === 'font' && <Type className="w-3.5 h-3.5 text-blue-400 shrink-0" />}
                {cell.badgeIcon === 'group' && <Layers className="w-3.5 h-3.5 text-indigo-400 shrink-0" />}
                <div className="min-w-0">
                  <div className="text-xs font-semibold text-neutral-200 truncate">{cell.title}</div>
                  <div className="text-[10px] font-mono text-neutral-500 truncate">{cell.subtitle}</div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-1 shrink-0">
                <button
                  onClick={() => handleCopy(cell)}
                  title="Copy Image URL"
                  className="p-1 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
                >
                  {copiedId === cell.id ? (
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>
                <button
                  onClick={() => triggerRender(cell.previewId)}
                  title="Re-render this preview"
                  className="p-1 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={() => handleFocus(cell)}
                  title="Focus in Single View"
                  className="p-1 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
                >
                  <Maximize2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Preview Image Viewport */}
            <div
              className={`relative h-[340px] flex items-center justify-center p-4 overflow-hidden select-none ${
                cell.theme === 'light' ? 'bg-neutral-200' : 'bg-neutral-950'
              }`}
            >
              <img
                src={cell.imageUrl}
                alt={cell.title}
                loading="lazy"
                className="max-w-full max-h-full object-contain rounded-md shadow-md transition-transform duration-200 group-hover:scale-[1.02]"
                onError={(e) => {
                  (e.target as HTMLImageElement).src =
                    'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="300" viewBox="0 0 200 300"><rect fill="%23111" width="200" height="300"/><text fill="%23666" x="50%" y="50%" text-anchor="middle" font-family="sans-serif" font-size="12">Preview Unavailable</text></svg>';
                }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
};
