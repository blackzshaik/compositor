import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { usePreviewConfig, PreviewConfigProvider } from '../context/PreviewConfigContext';
import { usePreviewCatalog, PreviewCatalogProvider } from '../context/PreviewCatalogContext';
import { MOCK_CATALOG, MOCK_PREVIEW_ITEM } from './test-helpers';

// Consumer component to test PreviewConfigContext methods
const ConfigConsumer: React.FC = () => {
  const config = usePreviewConfig();

  return (
    <div>
      <div data-testid="profile">{config.deviceProfile}</div>
      <div data-testid="orientation">{config.orientation}</div>
      <div data-testid="theme">{config.themeMode}</div>
      <div data-testid="font">{config.fontScale}</div>
      <div data-testid="backdrop">{config.backdrop}</div>
      <div data-testid="zoom">{config.zoom}</div>

      <button onClick={() => config.setDeviceProfile('foldable')}>Set Foldable</button>
      <button onClick={() => config.setOrientation('landscape')}>Set Landscape</button>
      <button onClick={() => config.setThemeMode('light')}>Set Light</button>
      <button onClick={() => config.setFontScale(1.3)}>Set Font 1.3</button>
      <button onClick={() => config.setBackdrop('checkerboard')}>Set Checkerboard</button>
      <button onClick={() => config.setZoom(1.5)}>Set Zoom 1.5</button>
    </div>
  );
};

// Consumer component to test PreviewCatalogContext methods
const CatalogConsumer: React.FC = () => {
  const catalog = usePreviewCatalog();

  return (
    <div>
      <div data-testid="count">{catalog.filteredPreviews.length}</div>
      <div data-testid="active">{catalog.activePreviewId}</div>
      <button onClick={() => catalog.triggerRender(MOCK_PREVIEW_ITEM.id)}>Trigger Render</button>
      <button onClick={() => catalog.setActivePreviewId(null)}>Clear Active</button>
    </div>
  );
};

describe('Context Providers', () => {
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

  it('PreviewConfigContext updates values and persists to localStorage', () => {
    render(
      <PreviewConfigProvider>
        <ConfigConsumer />
      </PreviewConfigProvider>
    );

    expect(screen.getByTestId('profile')).toHaveTextContent('pixel-8');
    fireEvent.click(screen.getByText('Set Foldable'));
    expect(screen.getByTestId('profile')).toHaveTextContent('foldable');

    fireEvent.click(screen.getByText('Set Landscape'));
    expect(screen.getByTestId('orientation')).toHaveTextContent('landscape');

    fireEvent.click(screen.getByText('Set Light'));
    expect(screen.getByTestId('theme')).toHaveTextContent('light');

    fireEvent.click(screen.getByText('Set Font 1.3'));
    expect(screen.getByTestId('font')).toHaveTextContent('1.3');

    fireEvent.click(screen.getByText('Set Checkerboard'));
    expect(screen.getByTestId('backdrop')).toHaveTextContent('checkerboard');

    fireEvent.click(screen.getByText('Set Zoom 1.5'));
    expect(screen.getByTestId('zoom')).toHaveTextContent('1.5');
  });

  it('PreviewCatalogContext triggers render and allows clearing active preview', async () => {
    render(
      <PreviewConfigProvider>
        <PreviewCatalogProvider>
          <CatalogConsumer />
        </PreviewCatalogProvider>
      </PreviewConfigProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('count')).toHaveTextContent('2');
    });

    fireEvent.click(screen.getByText('Trigger Render'));
    fireEvent.click(screen.getByText('Clear Active'));
    expect(screen.getByTestId('active')).toBeEmptyDOMElement();
  });
});
