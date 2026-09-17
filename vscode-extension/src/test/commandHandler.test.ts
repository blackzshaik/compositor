import { describe, it, expect, vi } from 'vitest';
import * as vscode from 'vscode';
import { registerCommands } from '../commands/commandHandler.js';
import { DaemonManager } from '../daemon/DaemonManager.js';
import { CompositorWebviewViewProvider } from '../webview/CompositorWebviewViewProvider.js';

describe('commandHandler', () => {
  it('registers all required Compositor commands', () => {
    const mockDaemon = {
      triggerRender: vi.fn(),
      start: vi.fn(),
      stop: vi.fn(),
      restart: vi.fn(),
      isConnected: true,
    } as unknown as DaemonManager;

    const mockWebviewProvider = {
      postMessage: vi.fn(),
      setActivePreview: vi.fn(),
    } as unknown as CompositorWebviewViewProvider;

    const mockOutputChannel = {
      appendLine: vi.fn(),
      show: vi.fn(),
      dispose: vi.fn(),
    } as unknown as vscode.OutputChannel;

    const fakeUri = {
      fsPath: 'C:\\fake\\extension',
    } as unknown as vscode.Uri;

    const disposables = registerCommands({
      extensionUri: fakeUri,
      daemonManager: mockDaemon,
      webviewProvider: mockWebviewProvider,
      outputChannel: mockOutputChannel,
      serverPort: 3001,
      wsPort: 3002,
    });

    expect(disposables.length).toBeGreaterThanOrEqual(8);
    expect(vscode.commands.registerCommand).toHaveBeenCalledWith(
      'compositor.openPreviewToSide',
      expect.any(Function)
    );
    expect(vscode.commands.registerCommand).toHaveBeenCalledWith(
      'compositor.rerenderActivePreview',
      expect.any(Function)
    );
    expect(vscode.commands.registerCommand).toHaveBeenCalledWith(
      'compositor.restartDaemon',
      expect.any(Function)
    );
    expect(vscode.commands.registerCommand).toHaveBeenCalledWith(
      'compositor.toggleTheme',
      expect.any(Function)
    );
  });
});
