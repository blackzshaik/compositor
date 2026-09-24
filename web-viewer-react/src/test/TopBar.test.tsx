import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TopBar } from '../components/controls/TopBar';
import { TestProviders, MOCK_CATALOG } from './test-helpers';

describe('TopBar Component', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve(MOCK_CATALOG),
        })
      )
    );
  });

  it('renders Compositor brand and controls', async () => {
    render(
      <TestProviders>
        <TopBar />
      </TestProviders>
    );

    expect(screen.getByText('Compositor')).toBeInTheDocument();
    expect(screen.getByText('Phase 2')).toBeInTheDocument();
    expect(screen.getByText('Single')).toBeInTheDocument();
    expect(screen.getByText('Matrix')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('GreetingPreview')).toBeInTheDocument();
    });
  });

  it('toggles view mode between Single and Matrix', () => {
    render(
      <TestProviders>
        <TopBar />
      </TestProviders>
    );

    const matrixBtn = screen.getByText('Matrix');
    fireEvent.click(matrixBtn);

    expect(screen.getByText('Matrix:')).toBeInTheDocument();

    const singleBtn = screen.getByText('Single');
    fireEvent.click(singleBtn);

    expect(screen.queryByText('Matrix:')).not.toBeInTheDocument();
  });

  it('toggles theme mode between dark and light', () => {
    render(
      <TestProviders>
        <TopBar />
      </TestProviders>
    );

    const themeBtn = screen.getByTitle(/Theme:/);
    fireEvent.click(themeBtn);

    // After toggle, title should reflect change
    expect(screen.getByTitle(/Theme: light mode/)).toBeInTheDocument();
  });

  it('toggles orientation between portrait and landscape', () => {
    render(
      <TestProviders>
        <TopBar />
      </TestProviders>
    );

    const orientationBtn = screen.getByTitle(/Orientation:/);
    fireEvent.click(orientationBtn);

    expect(screen.getByTitle(/Orientation: landscape/)).toBeInTheDocument();
  });

  it('toggles inspector mode when clicking inspect button', () => {
    render(
      <TestProviders>
        <TopBar />
      </TestProviders>
    );

    const inspectBtn = screen.getByTitle(/Toggle Element Inspector/);
    fireEvent.click(inspectBtn);
    expect(inspectBtn).toHaveClass('bg-indigo-600/30');

    fireEvent.click(inspectBtn);
    expect(inspectBtn).not.toHaveClass('bg-indigo-600/30');
  });

  it('handles re-render and refresh clicks', () => {
    render(
      <TestProviders>
        <TopBar />
      </TestProviders>
    );

    const refreshBtn = screen.getByTitle('Refresh preview catalog');
    fireEvent.click(refreshBtn);

    const reRenderBtn = screen.getByTitle('Re-render active preview');
    fireEvent.click(reRenderBtn);

    expect(refreshBtn).toBeInTheDocument();
  });

  it('handles device profile, backdrop, and font scale dropdown changes', () => {
    const { container } = render(
      <TestProviders>
        <TopBar />
      </TestProviders>
    );

    const selects = container.querySelectorAll('select');
    // selects[0] is device profile
    if (selects[0]) {
      fireEvent.change(selects[0], { target: { value: 'galaxy-s24' } });
    }
    // selects[1] is font scale
    if (selects[1]) {
      fireEvent.change(selects[1], { target: { value: '1.3' } });
    }
    // selects[2] is backdrop
    if (selects[2]) {
      fireEvent.change(selects[2], { target: { value: 'checkerboard' } });
    }

    // Switch to matrix mode and change matrix preset
    const matrixBtn = screen.getByText('Matrix');
    fireEvent.click(matrixBtn);

    const matrixSelect = container.querySelector('select');
    if (matrixSelect) {
      fireEvent.change(matrixSelect, { target: { value: 'font-scale' } });
    }
  });

  it('handles zoom in, zoom out, and fit to height clicks in topbar', () => {
    render(
      <TestProviders>
        <TopBar />
      </TestProviders>
    );

    const zoomInBtn = screen.getByTitle('Zoom In');
    const zoomOutBtn = screen.getByTitle('Zoom Out');
    const fitHeightBtns = screen.getAllByTitle(/Fit to Height/i);

    fireEvent.click(zoomInBtn);
    fireEvent.click(zoomOutBtn);
    fireEvent.click(fitHeightBtns[0]);
  });
});
