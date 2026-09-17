import { describe, it, expect, vi } from 'vitest';
import * as vscode from 'vscode';
import { DaemonManager } from '../daemon/DaemonManager.js';

describe('DaemonManager', () => {
  const mockOutputChannel = {
    appendLine: vi.fn(),
    show: vi.fn(),
    dispose: vi.fn(),
  } as unknown as vscode.OutputChannel;

  it('initializes with offline state and zero previews', () => {
    const manager = new DaemonManager(
      {
        workspaceRoot: 'C:\\fake\\workspace',
        httpPort: 39999, // Unused port
        wsPort: 39998,
      },
      mockOutputChannel
    );

    expect(manager.isConnected).toBe(false);
    expect(manager.totalPreviews).toBe(0);
    manager.dispose();
  });

  it('notifies registered listeners of lifecycle events', () => {
    const manager = new DaemonManager(
      {
        workspaceRoot: 'C:\\fake\\workspace',
        httpPort: 39999,
        wsPort: 39998,
      },
      mockOutputChannel
    );

    const receivedEvents: string[] = [];
    const subscription = manager.addListener((event) => {
      receivedEvents.push(event);
    });

    // Trigger stop on already stopped manager
    manager.stop();
    expect(receivedEvents).toContain('DAEMON_STOPPED');

    subscription.dispose();
    manager.dispose();
  });

  it('checkHealth returns false when daemon port is not listening', async () => {
    const manager = new DaemonManager(
      {
        workspaceRoot: 'C:\\fake\\workspace',
        httpPort: 39999,
        wsPort: 39998,
      },
      mockOutputChannel
    );

    const healthy = await manager.checkHealth();
    expect(healthy).toBe(false);
    manager.dispose();
  });
});
