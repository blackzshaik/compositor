import React, { useEffect } from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MatrixGrid } from '../components/matrix/MatrixGrid';
import { TestProviders, MOCK_CATALOG } from './test-helpers';
import { usePreviewConfig } from '../context/PreviewConfigContext';

// Helper component to switch matrix presets in tests
const MatrixWithControls: React.FC<{ preset?: 'theme' | 'font-scale' | 'group' | 'all' }> = ({
  preset = 'theme',
}) => {
  const { setMatrixPreset } = usePreviewConfig();
  useEffect(() => {
    setMatrixPreset(preset);
  }, [preset, setMatrixPreset]);

  return <MatrixGrid />;
};

describe('MatrixGrid Component', () => {
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

  it('renders theme matrix with Light and Dark cards', async () => {
    render(
      <TestProviders>
        <MatrixWithControls preset="theme" />
      </TestProviders>
    );

    await waitFor(() => {
      expect(screen.getByText('Light Theme')).toBeInTheDocument();
      expect(screen.getByText('Dark Theme')).toBeInTheDocument();
    });

    const cells = screen.getAllByTestId('matrix-cell');
    expect(cells.length).toBe(2);
  });

  it('renders font scale matrix with multiple typography sizes', async () => {
    render(
      <TestProviders>
        <MatrixWithControls preset="font-scale" />
      </TestProviders>
    );

    await waitFor(() => {
      expect(screen.getByText('Default (1.0x)')).toBeInTheDocument();
      expect(screen.getByText('Large (1.15x)')).toBeInTheDocument();
      expect(screen.getByText('Extra Large (1.3x)')).toBeInTheDocument();
      expect(screen.getByText('Huge (1.5x)')).toBeInTheDocument();
    });

    const cells = screen.getAllByTestId('matrix-cell');
    expect(cells.length).toBe(4);
  });

  it('renders group matrix with all previews belonging to group', async () => {
    render(
      <TestProviders>
        <MatrixWithControls preset="group" />
      </TestProviders>
    );

    await waitFor(() => {
      expect(screen.getByText('Greeting Light')).toBeInTheDocument();
      expect(screen.getByText('Greeting Dark')).toBeInTheDocument();
    });
  });

  it('handles focus to single view and copy image actions', async () => {
    // Mock navigator.clipboard
    const mockWriteText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText: mockWriteText,
      },
    });

    render(
      <TestProviders>
        <MatrixWithControls preset="theme" />
      </TestProviders>
    );

    await waitFor(() => {
      expect(screen.getByText('Light Theme')).toBeInTheDocument();
    });

    const copyBtns = screen.getAllByTitle('Copy Image URL');
    fireEvent.click(copyBtns[0]);
    expect(mockWriteText).toHaveBeenCalled();

    const focusBtns = screen.getAllByTitle('Focus in Single View');
    fireEvent.click(focusBtns[0]);

    const reRenderBtns = screen.getAllByTitle('Re-render this preview');
    fireEvent.click(reRenderBtns[0]);
  });

  it('renders all previews preset', async () => {
    render(
      <TestProviders>
        <MatrixWithControls preset="all" />
      </TestProviders>
    );

    await waitFor(() => {
      expect(screen.getByText('Greeting Light')).toBeInTheDocument();
      expect(screen.getByText('Greeting Dark')).toBeInTheDocument();
    });
  });
});
