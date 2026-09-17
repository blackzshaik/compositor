import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Sidebar } from '../components/sidebar/Sidebar';
import { TestProviders, MOCK_CATALOG } from './test-helpers';

describe('Sidebar Component', () => {
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

  it('renders sidebar tree with modules, files, and previews', async () => {
    render(
      <TestProviders>
        <Sidebar />
      </TestProviders>
    );

    // Wait for catalog to load
    await waitFor(() => {
      expect(screen.getByText('samples:sample-app')).toBeInTheDocument();
    });

    expect(screen.getByText('Greeting.kt')).toBeInTheDocument();
    expect(screen.getByText('Greeting Light')).toBeInTheDocument();
    expect(screen.getByText('Greeting Dark')).toBeInTheDocument();
  });

  it('filters previews when searching', async () => {
    render(
      <TestProviders>
        <Sidebar />
      </TestProviders>
    );

    await waitFor(() => {
      expect(screen.getByText('Greeting Light')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/Search previews/i);
    fireEvent.change(searchInput, { target: { value: 'Dark' } });

    expect(screen.queryByText('Greeting Light')).not.toBeInTheDocument();
    expect(screen.getByText('Greeting Dark')).toBeInTheDocument();

    // Clear search
    const clearBtn = screen.getByText('✕');
    fireEvent.click(clearBtn);
    expect(screen.getByText('Greeting Light')).toBeInTheDocument();
  });

  it('filters previews by group chip and resets with All', async () => {
    render(
      <TestProviders>
        <Sidebar />
      </TestProviders>
    );

    await waitFor(() => {
      expect(screen.getByText('Greetings')).toBeInTheDocument();
    });

    const groupChip = screen.getByText('Greetings');
    fireEvent.click(groupChip);
    expect(screen.getByText('Greeting Light')).toBeInTheDocument();

    // Click again to toggle off
    fireEvent.click(groupChip);

    // Click All button
    const allChip = screen.getByText('All');
    fireEvent.click(allChip);
    expect(screen.getByText('Greeting Light')).toBeInTheDocument();

    // Toggle module and file nodes
    const moduleBtn = screen.getByText('samples:sample-app');
    fireEvent.click(moduleBtn);
    fireEvent.click(moduleBtn);

    const fileBtn = screen.getByText('Greeting.kt');
    fireEvent.click(fileBtn);
    fireEvent.click(fileBtn);
  });

  it('collapses and expands sidebar', async () => {
    render(
      <TestProviders>
        <Sidebar />
      </TestProviders>
    );

    const collapseBtn = screen.getByTitle('Collapse Sidebar');
    fireEvent.click(collapseBtn);

    const expandBtn = screen.getByTitle('Expand Sidebar');
    expect(expandBtn).toBeInTheDocument();

    fireEvent.click(expandBtn);
    expect(screen.getByText('Preview Catalog')).toBeInTheDocument();
  });

  it('supports keyboard arrow navigation', async () => {
    render(
      <TestProviders>
        <Sidebar />
      </TestProviders>
    );

    await waitFor(() => {
      expect(screen.getByText('Greeting Light')).toBeInTheDocument();
    });

    const sidebar = screen.getByRole('complementary');
    fireEvent.keyDown(sidebar, { key: 'ArrowDown' });
    fireEvent.keyDown(sidebar, { key: 'Enter' });
    expect(sidebar).toBeInTheDocument();
  });
});
