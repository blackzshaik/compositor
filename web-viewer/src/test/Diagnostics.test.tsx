import React, { useEffect } from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ErrorBoundary } from '../components/diagnostics/ErrorBoundary';
import { ErrorOverlay } from '../components/diagnostics/ErrorOverlay';
import { TestProviders, MOCK_PREVIEW_ITEM } from './test-helpers';
import { usePreviewConfig } from '../context/PreviewConfigContext';

// Faulty component to trigger ErrorBoundary
const CrashComponent: React.FC<{ shouldCrash?: boolean }> = ({ shouldCrash = true }) => {
  if (shouldCrash) {
    throw new Error('Simulated React crash');
  }
  return <div>Component Normal</div>;
};

// Wrapper setting up an error diagnostic
const ErrorOverlayWrapper: React.FC = () => {
  const { setErrorDiagnostic } = usePreviewConfig();

  useEffect(() => {
    setErrorDiagnostic({
      previewId: MOCK_PREVIEW_ITEM.id,
      title: 'Compose Render Failure',
      message: 'Unresolved reference: NonExistentComposable at Greeting.kt:28',
      filePath: 'Greeting.kt',
      line: 28,
      codeSnippet: '27: @Composable\n28: NonExistentComposable()\n29: }',
      stackTrace:
        'java.lang.NoSuchMethodError: NonExistentComposable\n' +
        '  at com.compositor.sample.GreetingKt.GreetingPreview(Greeting.kt:28)\n' +
        '  at java.lang.reflect.Method.invoke(Method.java:568)',
      timestamp: Date.now(),
    });
  }, [setErrorDiagnostic]);

  return <ErrorOverlay />;
};

describe('Diagnostics & Error Boundary', () => {
  beforeEach(() => {
    // Suppress console.error in tests for intentional crash
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('catches React errors in ErrorBoundary and allows reset', () => {
    const { rerender } = render(
      <ErrorBoundary fallbackTitle="Custom Fallback Title">
        <CrashComponent shouldCrash={true} />
      </ErrorBoundary>
    );

    expect(screen.getByTestId('error-boundary-fallback')).toBeInTheDocument();
    expect(screen.getByText('Custom Fallback Title')).toBeInTheDocument();
    expect(screen.getAllByText(/Simulated React crash/)[0]).toBeInTheDocument();

    // Retry rendering with fixed component
    rerender(
      <ErrorBoundary fallbackTitle="Custom Fallback Title">
        <CrashComponent shouldCrash={false} />
      </ErrorBoundary>
    );

    const retryBtn = screen.getByText('Retry Rendering');
    fireEvent.click(retryBtn);

    expect(screen.getByText('Component Normal')).toBeInTheDocument();
  });

  it('renders ErrorOverlay with diagnostic details, snippet, and copy button', () => {
    const mockWriteText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText: mockWriteText,
      },
    });

    render(
      <TestProviders>
        <ErrorOverlayWrapper />
      </TestProviders>
    );

    expect(screen.getByTestId('error-diagnostic-overlay')).toBeInTheDocument();
    expect(screen.getByText('Compose Render Failure')).toBeInTheDocument();
    expect(screen.getByText(/Unresolved reference: NonExistentComposable/)).toBeInTheDocument();
    expect(screen.getByText('Greeting.kt')).toBeInTheDocument();
    expect(screen.getByText(':line 28')).toBeInTheDocument();

    // Toggle stack trace
    const stackToggle = screen.getByText(/JVM Stack Trace/);
    fireEvent.click(stackToggle);
    expect(screen.getByText(/com.compositor.sample.GreetingKt/)).toBeInTheDocument();

    // Copy diagnostic
    const copyBtn = screen.getByTitle('Copy Diagnostic for AI Prompt');
    fireEvent.click(copyBtn);
    expect(mockWriteText).toHaveBeenCalled();

    // Dismiss error
    const dismissBtn = screen.getByTitle('Dismiss Error Overlay');
    fireEvent.click(dismissBtn);
    expect(screen.queryByTestId('error-diagnostic-overlay')).not.toBeInTheDocument();
  });
});
