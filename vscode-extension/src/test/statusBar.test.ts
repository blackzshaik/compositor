import { describe, it, expect, beforeEach } from 'vitest';
import { CompositorStatusBar } from '../statusbar/CompositorStatusBar.js';

describe('CompositorStatusBar', () => {
  let statusBar: CompositorStatusBar;

  beforeEach(() => {
    statusBar = new CompositorStatusBar();
  });

  it('starts in offline state', () => {
    expect(statusBar.getState()).toBe('offline');
    expect(statusBar.getPreviewCount()).toBe(0);
  });

  it('transitions to live state with preview count', () => {
    statusBar.setLive(5);
    expect(statusBar.getState()).toBe('live');
    expect(statusBar.getPreviewCount()).toBe(5);
  });

  it('transitions to rendering and returns to live', () => {
    statusBar.setLive(3);
    statusBar.setRendering(true);
    expect(statusBar.getState()).toBe('rendering');

    statusBar.setRendering(false);
    expect(statusBar.getState()).toBe('live');
    expect(statusBar.getPreviewCount()).toBe(3);
  });

  it('transitions to connecting state', () => {
    statusBar.setConnecting();
    expect(statusBar.getState()).toBe('connecting');
  });

  it('transitions to offline state', () => {
    statusBar.setLive(4);
    statusBar.setOffline();
    expect(statusBar.getState()).toBe('offline');
  });
});
