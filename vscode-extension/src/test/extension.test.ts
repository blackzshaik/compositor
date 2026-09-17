import { describe, it, expect } from 'vitest';
import * as vscode from 'vscode';
import { activate, deactivate } from '../extension.js';

describe('extension entrypoint', () => {
  it('activates successfully and registers subscriptions', async () => {
    const mockSubscriptions: vscode.Disposable[] = [];
    const mockContext = {
      subscriptions: mockSubscriptions,
      extensionUri: {
        fsPath: 'C:\\fake\\extension',
      } as unknown as vscode.Uri,
    } as unknown as vscode.ExtensionContext;

    await activate(mockContext);

    expect(vscode.window.createOutputChannel).toHaveBeenCalled();
    expect(vscode.window.createStatusBarItem).toHaveBeenCalled();
    expect(vscode.window.registerWebviewViewProvider).toHaveBeenCalled();
    expect(mockSubscriptions.length).toBeGreaterThan(0);

    // Test clean deactivation
    expect(() => deactivate()).not.toThrow();
  });
});
