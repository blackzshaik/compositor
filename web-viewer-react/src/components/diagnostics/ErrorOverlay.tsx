import React, { useState } from 'react';
import {
  AlertTriangle,
  Copy,
  Check,
  ChevronDown,
  ChevronUp,
  X,
  FileCode,
  Sparkles,
} from 'lucide-react';
import { usePreviewConfig } from '../../context/PreviewConfigContext';
import { usePreviewCatalog } from '../../context/PreviewCatalogContext';

export const ErrorOverlay: React.FC = () => {
  const { errorDiagnostic, clearError } = usePreviewConfig();
  const { activePreview, triggerRender } = usePreviewCatalog();

  const [copied, setCopied] = useState<boolean>(false);
  const [showFullStack, setShowFullStack] = useState<boolean>(false);

  // If no error or the error is for a different preview, don't show overlay
  if (!errorDiagnostic || (activePreview && errorDiagnostic.previewId !== activePreview.id)) {
    return null;
  }

  const handleCopyDiagnostic = async () => {
    try {
      const text = [
        `[Compositor Render Diagnostic]`,
        `Preview ID: ${errorDiagnostic.previewId}`,
        `Title: ${errorDiagnostic.title}`,
        errorDiagnostic.filePath ? `File: ${errorDiagnostic.filePath}:${errorDiagnostic.line ?? '?'}` : null,
        `Message:\n${errorDiagnostic.message}`,
        errorDiagnostic.stackTrace ? `\nStack Trace:\n${errorDiagnostic.stackTrace}` : null,
      ]
        .filter(Boolean)
        .join('\n');

      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy diagnostic:', err);
    }
  };

  // Stack trace parser: separate user frames from internal framework frames
  const parsedStackFrames = errorDiagnostic.stackTrace
    ? errorDiagnostic.stackTrace.split('\n').map((line, idx) => {
        const isUserCode =
          line.includes('com.compositor') || line.includes('.kt:') || line.includes('.java:');
        const isFramework =
          line.includes('java.lang.reflect') ||
          line.includes('jdk.internal') ||
          line.includes('com.android.layoutlib');

        return { text: line, isUserCode, isFramework, id: idx };
      })
    : [];

  return (
    <div
      data-testid="error-diagnostic-overlay"
      className="absolute inset-0 bg-neutral-950/85 backdrop-blur-sm z-40 flex items-center justify-center p-4 overflow-y-auto"
    >
      <div className="w-full max-w-lg bg-neutral-900 border border-rose-800/60 rounded-2xl shadow-2xl shadow-rose-950/40 overflow-hidden flex flex-col text-neutral-200">
        {/* Error Header */}
        <div className="px-4 py-3 bg-rose-950/40 border-b border-rose-900/50 flex items-center justify-between">
          <div className="flex items-center gap-2 min-w-0">
            <div className="w-6 h-6 rounded-full bg-rose-900/60 border border-rose-600/50 flex items-center justify-center text-rose-400 shrink-0">
              <AlertTriangle className="w-3.5 h-3.5" />
            </div>
            <div className="min-w-0">
              <h4 className="text-xs font-bold text-rose-300 truncate">
                {errorDiagnostic.title || 'Render Error'}
              </h4>
              <div className="text-[10px] font-mono text-neutral-400 truncate">
                Preview: {errorDiagnostic.previewId}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-1 shrink-0">
            <button
              onClick={handleCopyDiagnostic}
              title="Copy Diagnostic for AI Prompt"
              className="flex items-center gap-1 px-2 py-1 text-[11px] rounded-md bg-neutral-800 hover:bg-neutral-700 text-neutral-300 transition-colors"
            >
              {copied ? (
                <>
                  <Check className="w-3 h-3 text-emerald-400" />
                  <span className="text-emerald-400">Copied</span>
                </>
              ) : (
                <>
                  <Copy className="w-3 h-3" />
                  <span>Copy</span>
                </>
              )}
            </button>

            <button
              onClick={clearError}
              title="Dismiss Error Overlay"
              className="p-1 rounded-md text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Error Body */}
        <div className="p-4 space-y-3 text-xs">
          {/* File location chip */}
          {errorDiagnostic.filePath && (
            <div className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-neutral-950 border border-neutral-800 font-mono text-[11px] text-amber-300">
              <FileCode className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
              <span className="font-semibold truncate">{errorDiagnostic.filePath}</span>
              {errorDiagnostic.line && (
                <span className="text-neutral-500">:line {errorDiagnostic.line}</span>
              )}
            </div>
          )}

          {/* Diagnostic Message */}
          <div className="bg-neutral-950 p-3 rounded-lg border border-neutral-800 font-mono text-xs text-rose-300 overflow-x-auto whitespace-pre-wrap max-h-36">
            {errorDiagnostic.message}
          </div>

          {/* Code snippet around error line */}
          {errorDiagnostic.codeSnippet && (
            <div className="space-y-1">
              <div className="text-[11px] font-semibold text-neutral-400">Source Context:</div>
              <div className="bg-neutral-950 p-2.5 rounded-lg border border-neutral-800 font-mono text-[11px] text-neutral-300 overflow-x-auto">
                <pre className="text-neutral-400">{errorDiagnostic.codeSnippet}</pre>
              </div>
            </div>
          )}

          {/* Collapsible Stack Trace */}
          {parsedStackFrames.length > 0 && (
            <div className="space-y-1">
              <button
                onClick={() => setShowFullStack(!showFullStack)}
                className="flex items-center justify-between w-full text-[11px] font-semibold text-neutral-400 hover:text-neutral-200 py-1"
              >
                <span>JVM Stack Trace ({parsedStackFrames.length} frames)</span>
                {showFullStack ? (
                  <ChevronUp className="w-3.5 h-3.5" />
                ) : (
                  <ChevronDown className="w-3.5 h-3.5" />
                )}
              </button>

              {showFullStack && (
                <div className="bg-neutral-950 p-2.5 rounded-lg border border-neutral-800 max-h-40 overflow-y-auto space-y-0.5 font-mono text-[10px]">
                  {parsedStackFrames.map((frame) => (
                    <div
                      key={frame.id}
                      className={`truncate ${
                        frame.isUserCode
                          ? 'text-purple-300 font-semibold bg-purple-950/30 px-1 rounded-xs'
                          : 'text-neutral-600'
                      }`}
                    >
                      {frame.text}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="px-4 py-2.5 bg-neutral-950/60 border-t border-neutral-800 flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-[10px] text-neutral-400">
            <Sparkles className="w-3 h-3 text-purple-400" />
            <span>Auto-dismisses upon successful compilation</span>
          </div>

          <button
            onClick={() => triggerRender(errorDiagnostic.previewId)}
            className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-rose-600 hover:bg-rose-500 text-white transition-colors"
          >
            Retry Render
          </button>
        </div>
      </div>
    </div>
  );
};
