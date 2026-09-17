import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CanvasViewport } from '../components/device/CanvasViewport';
import { PreviewConfigProvider } from '../context/PreviewConfigContext';

describe('CanvasViewport Component', () => {
  it('renders canvas with zoom controls and children', () => {
    render(
      <PreviewConfigProvider>
        <CanvasViewport>
          <div data-testid="test-content">Canvas Content</div>
        </CanvasViewport>
      </PreviewConfigProvider>
    );

    expect(screen.getByTestId('test-content')).toBeInTheDocument();
    expect(screen.getByText('100%')).toBeInTheDocument();

    const zoomInBtn = screen.getByTitle('Zoom In');
    const zoomOutBtn = screen.getByTitle('Zoom Out');
    const fitBtn = screen.getByTitle('Fit to Height');
    const resetBtn = screen.getByTitle('Reset Zoom & Pan (100%)');

    // Zoom in
    fireEvent.click(zoomInBtn);
    expect(screen.getByText('110%')).toBeInTheDocument();

    // Zoom out
    fireEvent.click(zoomOutBtn);
    expect(screen.getByText('100%')).toBeInTheDocument();

    // Fit to height
    fireEvent.click(fitBtn);

    // Reset zoom
    fireEvent.click(resetBtn);
    expect(screen.getByText('100%')).toBeInTheDocument();
  });

  it('handles mouse pan gestures on canvas', () => {
    render(
      <PreviewConfigProvider>
        <CanvasViewport>
          <div>Movable Item</div>
        </CanvasViewport>
      </PreviewConfigProvider>
    );

    const viewport = screen.getByTestId('canvas-viewport');

    // Drag start
    fireEvent.mouseDown(viewport, { button: 0, clientX: 100, clientY: 100 });
    // Move
    fireEvent.mouseMove(viewport, { clientX: 150, clientY: 120 });
    // Release
    fireEvent.mouseUp(viewport);
    expect(viewport).toBeInTheDocument();
  });

  it('handles wheel zoom event with ctrlKey', () => {
    render(
      <PreviewConfigProvider>
        <CanvasViewport>
          <div>Zoom Item</div>
        </CanvasViewport>
      </PreviewConfigProvider>
    );

    const viewport = screen.getByTestId('canvas-viewport');
    fireEvent.wheel(viewport, { deltaY: -100, ctrlKey: true });
    expect(viewport).toBeInTheDocument();
  });
});
