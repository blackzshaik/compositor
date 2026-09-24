import React, { useState, useMemo, useEffect, useRef } from 'react';
import {
  Search,
  ChevronRight,
  ChevronDown,
  Layers,
  FileCode,
  Tag,
  PanelLeftClose,
  PanelLeft,
  CheckCircle2,
  AlertCircle,
  Clock,
  RefreshCw,
  FolderGit2,
} from 'lucide-react';
import { usePreviewCatalog } from '../../context/PreviewCatalogContext';
import { usePreviewConfig } from '../../context/PreviewConfigContext';
import { PreviewItem, PreviewRenderStatus } from '../../types/preview';

export const Sidebar: React.FC = () => {
  const {
    filteredPreviews,
    activePreviewId,
    setActivePreviewId,
    searchQuery,
    setSearchQuery,
    allGroups,
    selectedGroup,
    setSelectedGroup,
    loading,
    refreshCatalog,
  } = usePreviewCatalog();

  const { sidebarCollapsed, setSidebarCollapsed } = usePreviewConfig();

  // Expanded folders in the tree: keyed by "module" or "module:file"
  const [expandedNodes, setExpandedNodes] = useState<Record<string, boolean>>({});
  const [focusedIndex, setFocusedIndex] = useState<number>(-1);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Group previews into a hierarchy: Module -> File -> PreviewItem[]
  const tree = useMemo(() => {
    const map = new Map<string, Map<string, PreviewItem[]>>();

    for (const preview of filteredPreviews) {
      const mod = preview.module || 'default';
      const file = preview.definition.filePath || 'Unknown.kt';

      if (!map.has(mod)) {
        map.set(mod, new Map());
      }
      const fileMap = map.get(mod)!;
      if (!fileMap.has(file)) {
        fileMap.set(file, []);
      }
      fileMap.get(file)!.push(preview);
    }
    return map;
  }, [filteredPreviews]);

  // Expand all by default when tree updates
  useEffect(() => {
    const nextExpanded: Record<string, boolean> = {};
    for (const [mod, fileMap] of tree.entries()) {
      nextExpanded[mod] = true;
      for (const file of fileMap.keys()) {
        nextExpanded[`${mod}:${file}`] = true;
      }
    }
    setExpandedNodes((prev) => ({ ...nextExpanded, ...prev }));
  }, [tree]);

  const toggleNode = (key: string) => {
    setExpandedNodes((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  // Flattened list of preview IDs for keyboard navigation
  const flatPreviews = useMemo(() => filteredPreviews, [filteredPreviews]);

  // Keyboard navigation up / down
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (flatPreviews.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setFocusedIndex((prev) => {
        const next = prev < flatPreviews.length - 1 ? prev + 1 : 0;
        setActivePreviewId(flatPreviews[next].id);
        return next;
      });
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setFocusedIndex((prev) => {
        const next = prev > 0 ? prev - 1 : flatPreviews.length - 1;
        setActivePreviewId(flatPreviews[next].id);
        return next;
      });
    } else if (e.key === 'Enter' && focusedIndex >= 0 && focusedIndex < flatPreviews.length) {
      e.preventDefault();
      setActivePreviewId(flatPreviews[focusedIndex].id);
    }
  };

  // Status Badge Component
  const renderStatusBadge = (status: PreviewRenderStatus) => {
    switch (status) {
      case 'Rendered':
        return (
          <span title="Rendered successfully" className="flex items-center text-emerald-400">
            <CheckCircle2 className="w-3.5 h-3.5" />
          </span>
        );
      case 'Rendering':
        return (
          <span title="Rendering..." className="flex items-center text-amber-400 animate-spin">
            <RefreshCw className="w-3.5 h-3.5" />
          </span>
        );
      case 'Error':
        return (
          <span title="Rendering Error" className="flex items-center text-rose-400">
            <AlertCircle className="w-3.5 h-3.5" />
          </span>
        );
      case 'Pending':
      default:
        return (
          <span title="Pending" className="flex items-center text-neutral-500">
            <Clock className="w-3.5 h-3.5" />
          </span>
        );
    }
  };

  if (sidebarCollapsed) {
    return (
      <aside className="w-12 bg-neutral-900 border-r border-neutral-800 flex flex-col items-center py-4 gap-4 z-20">
        <button
          onClick={() => setSidebarCollapsed(false)}
          title="Expand Sidebar"
          className="p-2 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
        >
          <PanelLeft className="w-5 h-5" />
        </button>
        <div className="w-6 h-px bg-neutral-800" />
        <div className="writing-mode-vertical text-xs tracking-wider text-neutral-400 font-mono rotate-180 uppercase select-none">
          Previews ({filteredPreviews.length})
        </div>
      </aside>
    );
  }

  return (
    <aside
      tabIndex={0}
      onKeyDown={handleKeyDown}
      className="w-80 bg-neutral-900 border-r border-neutral-800 flex flex-col h-full z-20 select-none outline-hidden focus:ring-1 focus:ring-purple-500/30"
    >
      {/* Sidebar Header */}
      <div className="p-3 border-b border-neutral-800 flex items-center justify-between">
        <div className="flex items-center gap-2 text-neutral-200 font-semibold text-sm">
          <Layers className="w-4 h-4 text-purple-400" />
          <span>Preview Catalog</span>
          <span className="text-xs px-1.5 py-0.5 rounded-full bg-neutral-800 text-neutral-400 font-mono">
            {filteredPreviews.length}
          </span>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={() => refreshCatalog()}
            title="Refresh Catalog"
            className={`p-1.5 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors ${
              loading ? 'animate-spin text-purple-400' : ''
            }`}
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => setSidebarCollapsed(true)}
            title="Collapse Sidebar"
            className="p-1.5 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
          >
            <PanelLeftClose className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="p-3 border-b border-neutral-800/80">
        <div className="relative">
          <Search className="w-4 h-4 text-neutral-400 absolute left-2.5 top-2.5" />
          <input
            ref={searchInputRef}
            type="text"
            placeholder="Search previews... (name, file, tag)"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-neutral-950 text-neutral-200 text-xs pl-8 pr-3 py-2 rounded-md border border-neutral-800 focus:outline-hidden focus:border-purple-500 placeholder-neutral-500"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-2 text-neutral-500 hover:text-neutral-300 text-xs"
            >
              ✕
            </button>
          )}
        </div>

        {/* Group / Tag Chips */}
        {allGroups.length > 0 && (
          <div className="flex items-center gap-1.5 mt-2.5 overflow-x-auto pb-1 scrollbar-none">
            <button
              onClick={() => setSelectedGroup(null)}
              className={`text-[11px] px-2 py-0.5 rounded-full whitespace-nowrap transition-colors ${
                selectedGroup === null
                  ? 'bg-purple-600 text-white font-medium'
                  : 'bg-neutral-800 text-neutral-400 hover:text-neutral-200'
              }`}
            >
              All
            </button>
            {allGroups.map((group) => (
              <button
                key={group}
                onClick={() => setSelectedGroup(selectedGroup === group ? null : group)}
                className={`flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full whitespace-nowrap transition-colors ${
                  selectedGroup === group
                    ? 'bg-purple-600 text-white font-medium'
                    : 'bg-neutral-800 text-neutral-400 hover:text-neutral-200'
                }`}
              >
                <Tag className="w-2.5 h-2.5" />
                <span>{group}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Hierarchical Preview Tree */}
      <div className="flex-1 overflow-y-auto p-2 space-y-1">
        {filteredPreviews.length === 0 ? (
          <div className="p-6 text-center text-xs text-neutral-500">
            {loading ? 'Scanning previews...' : 'No previews found'}
          </div>
        ) : (
          Array.from(tree.entries()).map(([moduleName, fileMap]) => {
            const isModuleExpanded = expandedNodes[moduleName] ?? true;

            return (
              <div key={moduleName} className="space-y-0.5">
                {/* Module Node */}
                <button
                  onClick={() => toggleNode(moduleName)}
                  className="w-full flex items-center gap-1.5 px-2 py-1 text-xs font-semibold text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800/50 rounded-sm transition-colors text-left"
                >
                  {isModuleExpanded ? (
                    <ChevronDown className="w-3.5 h-3.5 text-neutral-500" />
                  ) : (
                    <ChevronRight className="w-3.5 h-3.5 text-neutral-500" />
                  )}
                  <FolderGit2 className="w-3.5 h-3.5 text-purple-400/80" />
                  <span className="truncate">{moduleName}</span>
                </button>

                {/* Files under Module */}
                {isModuleExpanded && (
                  <div className="pl-3 space-y-0.5">
                    {Array.from(fileMap.entries()).map(([filePath, previews]) => {
                      const fileKey = `${moduleName}:${filePath}`;
                      const isFileExpanded = expandedNodes[fileKey] ?? true;
                      const fileName = filePath.split(/[/\\]/).pop() || filePath;

                      return (
                        <div key={fileKey} className="space-y-0.5">
                          {/* File Node */}
                          <button
                            onClick={() => toggleNode(fileKey)}
                            className="w-full flex items-center gap-1.5 px-2 py-1 text-[11px] text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800/40 rounded-sm transition-colors text-left"
                          >
                            {isFileExpanded ? (
                              <ChevronDown className="w-3 h-3 text-neutral-600" />
                            ) : (
                              <ChevronRight className="w-3 h-3 text-neutral-600" />
                            )}
                            <FileCode className="w-3 h-3 text-neutral-400" />
                            <span className="truncate flex-1 font-mono">{fileName}</span>
                            <span className="text-[10px] text-neutral-600 font-mono">
                              {previews.length}
                            </span>
                          </button>

                          {/* Previews under File */}
                          {isFileExpanded && (
                            <div className="pl-4 space-y-0.5">
                              {previews.map((item) => {
                                const isActive = item.id === activePreviewId;
                                const title =
                                  item.definition.parameters.name || item.definition.functionName;

                                return (
                                  <button
                                    key={item.id}
                                    onClick={() => setActivePreviewId(item.id)}
                                    className={`w-full flex items-center justify-between gap-2 px-2.5 py-1.5 text-xs rounded-md transition-all text-left group ${
                                      isActive
                                        ? 'bg-purple-600 text-white font-medium shadow-xs shadow-purple-900/30'
                                        : 'text-neutral-300 hover:bg-neutral-800/70 hover:text-white'
                                    }`}
                                  >
                                    <div className="min-w-0 flex-1">
                                      <div className="truncate font-medium">{title}</div>
                                      {item.definition.parameters.name && (
                                        <div
                                          className={`text-[10px] font-mono truncate ${
                                            isActive ? 'text-purple-200' : 'text-neutral-500'
                                          }`}
                                        >
                                          @{item.definition.functionName}
                                        </div>
                                      )}
                                    </div>
                                    <div className="shrink-0 flex items-center gap-1">
                                      {renderStatusBadge(item.status)}
                                    </div>
                                  </button>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </aside>
  );
};
