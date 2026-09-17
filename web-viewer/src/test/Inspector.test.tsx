import React, { useEffect } from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { InspectorOverlay } from '../components/inspector/InspectorOverlay';
import { InspectorDrawer } from '../components/inspector/InspectorDrawer';
import { TestProviders, MOCK_CATALOG } from './test-helpers';
import { usePreviewConfig } from '../context/PreviewConfigContext';

const InspectorWrapper: React.FC<{ enabled?: boolean }> = ({ enabled = true }) => {
  const { setInspectorEnabled } = usePreviewConfig();
  useEffect(() => {
    setInspectorEnabled(enabled);
  }, [enabled, setInspectorEnabled]);

  return (
    <div style={{ position: 'relative', width: 360, height: 780 }}>
      <InspectorOverlay containerWidth={360} containerHeight={780} />
      <InspectorDrawer />
    </div>
  );
};

describe('Inspector Components', () => {
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

  it('renders nothing when inspector is disabled', () => {
    render(
      <TestProviders>
        <InspectorWrapper enabled={false} />
      </TestProviders>
    );

    expect(screen.queryByTestId('inspector-overlay')).not.toBeInTheDocument();
    expect(screen.queryByTestId('inspector-drawer')).not.toBeInTheDocument();
  });

  it('renders overlay bounding boxes when inspector is enabled', async () => {
    render(
      <TestProviders>
        <InspectorWrapper enabled={true} />
      </TestProviders>
    );

    const overlay = await screen.findByTestId('inspector-overlay');
    expect(overlay).toBeInTheDocument();

    // Hover an element in the overlay
    const boxes = overlay.querySelectorAll('div');
    expect(boxes.length).toBeGreaterThan(0);

    fireEvent.mouseEnter(boxes[0]);
    fireEvent.mouseLeave(boxes[0]);

    // Click first box
    fireEvent.click(boxes[0]);

    // Drawer should open and display layout geometry
    await waitFor(() => {
      expect(screen.getByTestId('inspector-drawer')).toBeInTheDocument();
      expect(screen.getByText('Layout Geometry')).toBeInTheDocument();
    });

    // Click another box (which may have padding / semantics)
    if (boxes[1]) {
      fireEvent.mouseEnter(boxes[1]);
      fireEvent.click(boxes[1]);
    }
    if (boxes[2]) {
      fireEvent.mouseEnter(boxes[2]);
      fireEvent.click(boxes[2]);
    }

    // Close drawer
    const closeBtn = screen.getByTitle('Close Inspector Drawer');
    fireEvent.click(closeBtn);
    expect(screen.queryByTestId('inspector-drawer')).not.toBeInTheDocument();

    // Click background of overlay
    fireEvent.click(overlay);
  });
});
