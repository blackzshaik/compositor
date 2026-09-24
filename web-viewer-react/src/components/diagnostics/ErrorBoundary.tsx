import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertOctagon, RotateCcw } from 'lucide-react';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallbackTitle?: string;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  public override state: ErrorBoundaryState = {
    hasError: false,
    error: null,
    errorInfo: null,
  };

  public static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, error };
  }

  public override componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('[Compositor ErrorBoundary] Caught exception:', error, errorInfo);
    this.setState({ errorInfo });
  }

  public handleReset = (): void => {
    this.setState({ hasError: false, error: null, errorInfo: null });
  };

  public override render(): ReactNode {
    if (this.state.hasError) {
      return (
        <div
          data-testid="error-boundary-fallback"
          className="flex-1 flex flex-col items-center justify-center p-8 bg-neutral-950 text-neutral-200"
        >
          <div className="max-w-md w-full bg-neutral-900 border border-rose-900/50 rounded-2xl p-6 shadow-2xl flex flex-col items-center text-center">
            <div className="w-12 h-12 rounded-full bg-rose-950/60 border border-rose-700/50 flex items-center justify-center text-rose-400 mb-4">
              <AlertOctagon className="w-6 h-6" />
            </div>

            <h3 className="text-base font-bold text-rose-300 mb-1">
              {this.props.fallbackTitle || 'Component Rendering Error'}
            </h3>

            <p className="text-xs text-neutral-400 mb-4">
              An unexpected error occurred in this view. Other parts of the Compositor dashboard
              remain unaffected.
            </p>

            {this.state.error && (
              <div className="w-full bg-neutral-950 p-3 rounded-lg border border-neutral-800 text-left mb-4 overflow-auto max-h-36">
                <div className="font-mono text-xs text-rose-400 font-semibold mb-1">
                  {this.state.error.name}: {this.state.error.message}
                </div>
                {this.state.error.stack && (
                  <pre className="font-mono text-[10px] text-neutral-500 whitespace-pre-wrap">
                    {this.state.error.stack}
                  </pre>
                )}
              </div>
            )}

            <button
              onClick={this.handleReset}
              className="flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-200 transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Retry Rendering</span>
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
