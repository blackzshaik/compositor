import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from '../App';
import { MOCK_CATALOG } from './test-helpers';

describe('App Integration', () => {
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

  it('renders the complete Compositor studio dashboard', async () => {
    render(<App />);

    expect(screen.getByText('Compositor')).toBeInTheDocument();
    expect(screen.getByText('Phase 2')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Preview Catalog')).toBeInTheDocument();
      expect(screen.getByText('Greeting Light')).toBeInTheDocument();
    });

    expect(screen.getByTestId('canvas-viewport')).toBeInTheDocument();
  });
});
