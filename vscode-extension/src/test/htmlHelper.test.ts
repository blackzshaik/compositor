import { describe, it, expect } from 'vitest';
import * as vscode from 'vscode';
import { getNonce, getWebviewContent } from '../webview/htmlHelper.js';

describe('htmlHelper', () => {
  it('generates a unique 32-character hex nonce', () => {
    const nonce1 = getNonce();
    const nonce2 = getNonce();
    expect(nonce1).toHaveLength(32);
    expect(nonce2).toHaveLength(32);
    expect(nonce1).not.toBe(nonce2);
  });

  it('generates fallback HTML with CSP when index.html is missing', () => {
    const mockWebview = {
      asWebviewUri: (uri: vscode.Uri) => ({
        toString: () => `vscode-resource://${uri.path}`,
      }),
      cspSource: 'vscode-webview:',
    } as unknown as vscode.Webview;

    const fakeExtensionUri = {
      fsPath: 'C:\\fake\\extension\\path',
    } as unknown as vscode.Uri;

    const html = getWebviewContent(mockWebview, fakeExtensionUri, {
      serverPort: 3001,
      wsPort: 3002,
    });

    expect(html).toContain('<!DOCTYPE html>');
    expect(html).toContain('Content-Security-Policy');
    expect(html).toContain('default-src \'none\'');
    expect(html).toContain('http://localhost:3001');
    expect(html).toContain('Compositor Live Preview');
  });
});
