import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DeviceFrame } from '../components/device/DeviceFrame';

describe('DeviceFrame Component', () => {
  it('renders Google Pixel 8 frame in portrait mode by default', () => {
    render(
      <DeviceFrame
        imageUrl="http://localhost:3001/api/preview.png"
        title="TestPreview"
        profileId="pixel-8"
        orientation="portrait"
      />
    );

    const frame = screen.getByTestId('device-frame-pixel-8');
    expect(frame).toBeInTheDocument();

    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('src', 'http://localhost:3001/api/preview.png');
    expect(img).toHaveAttribute('alt', 'TestPreview');

    const cutout = screen.getByLabelText('Camera cutout');
    expect(cutout).toBeInTheDocument();

    const homeIndicator = screen.getByLabelText('Home indicator');
    expect(homeIndicator).toBeInTheDocument();
  });

  it('renders Galaxy S24 Ultra frame correctly', () => {
    render(
      <DeviceFrame
        imageUrl="http://localhost:3001/api/preview.png"
        title="GalaxyPreview"
        profileId="galaxy-s24"
        orientation="portrait"
      />
    );

    const frame = screen.getByTestId('device-frame-galaxy-s24');
    expect(frame).toBeInTheDocument();
  });

  it('renders Tablet 10 frame with tablet camera cutout in both orientations', () => {
    const { rerender } = render(
      <DeviceFrame
        imageUrl="http://localhost:3001/api/preview.png"
        title="TabletPreview"
        profileId="tablet-10"
        orientation="landscape"
      />
    );

    expect(screen.getByTestId('device-frame-tablet-10')).toBeInTheDocument();
    expect(screen.getByLabelText('Tablet camera')).toBeInTheDocument();

    // Rerender tablet in portrait
    rerender(
      <DeviceFrame
        imageUrl="http://localhost:3001/api/preview.png"
        title="TabletPreview"
        profileId="tablet-10"
        orientation="portrait"
      />
    );
    expect(screen.getByLabelText('Tablet camera')).toBeInTheDocument();
  });

  it('renders Foldable and landscape Pixel 8', () => {
    const { rerender } = render(
      <DeviceFrame
        imageUrl="http://localhost:3001/api/preview.png"
        title="FoldablePreview"
        profileId="foldable"
        orientation="portrait"
      />
    );
    expect(screen.getByTestId('device-frame-foldable')).toBeInTheDocument();

    // Landscape Pixel 8
    rerender(
      <DeviceFrame
        imageUrl="http://localhost:3001/api/preview.png"
        title="LandscapePixel"
        profileId="pixel-8"
        orientation="landscape"
      />
    );
    expect(screen.getByTestId('device-frame-pixel-8')).toBeInTheDocument();
    expect(screen.getByLabelText('Camera cutout')).toBeInTheDocument();
    expect(screen.getByLabelText('Home indicator')).toBeInTheDocument();
  });

  it('renders Frameless mode without hardware bezels or cutouts', () => {
    render(
      <DeviceFrame
        imageUrl="http://localhost:3001/api/preview.png"
        title="FramelessPreview"
        profileId="frameless"
        orientation="portrait"
      />
    );

    const frame = screen.getByTestId('device-frame-frameless');
    expect(frame).toBeInTheDocument();
    expect(screen.queryByLabelText('Camera cutout')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Home indicator')).not.toBeInTheDocument();
  });

  it('displays loading spinner when isLoading is true', () => {
    const { container } = render(
      <DeviceFrame
        imageUrl="http://localhost:3001/api/preview.png"
        title="LoadingPreview"
        isLoading={true}
      />
    );

    const spinner = container.querySelector('.animate-spin');
    expect(spinner).toBeInTheDocument();
  });

  it('handles image error with fallback svg', () => {
    render(
      <DeviceFrame
        imageUrl="http://localhost:3001/invalid-image.png"
        title="ErrorFallbackPreview"
      />
    );

    const img = screen.getByRole('img');
    fireEvent.error(img);
    expect(img.getAttribute('src')).toContain('data:image/svg+xml');
  });
});
