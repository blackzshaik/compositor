import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as vscode from 'vscode';
import { CompositorWebviewViewProvider } from '../webview/CompositorWebviewViewProvider.js';

describe('CompositorWebviewViewProvider', () => {
  let provider: CompositorWebviewViewProvider;
  const fakeExtensionUri = {
    fsPath: 'C:\\fake\\extension',
  } as unknown as vscode.Uri;

  beforeEach(() => {
    provider = new CompositorWebviewViewProvider(fakeExtensionUri);
  });

  it('resolves webview view and sets title and html', () => {
    const postMessageMock = vi.fn().mockResolvedValue(true);
    const mockWebviewView = {
      description: '',
      visible: true,
      webview: {
        options: {},
        html: '',
        onDidReceiveMessage: vi.fn(() => new vscode.Disposable(() => {})),
        postMessage: postMessageMock,
        asWebviewUri: (uri: vscode.Uri) => ({ toString: () => `vscode-resource://${uri.path}` }),
        cspSource: 'vscode-webview:',
      },
      onDidChangeVisibility: vi.fn(() => new vscode.Disposable(() => {})),
      onDidDispose: vi.fn(() => new vscode.Disposable(() => {})),
    } as unknown as vscode.WebviewView;

    provider.resolveWebviewView(
      mockWebviewView,
      {} as vscode.WebviewViewResolveContext,
      {} as vscode.CancellationToken
    );

    expect(mockWebviewView.description).toBe('Live Compose Preview');
    expect(mockWebviewView.webview.html).toContain('Compositor');

    provider.setActivePreview('com.example.sampleapp.GreetingPreview');
    expect(postMessageMock).toHaveBeenCalledWith({
      type: 'ACTIVE_PREVIEW_CHANGED',
      previewId: 'com.example.sampleapp.GreetingPreview',
      filePath: null,
    });
  });

  it('updates port configuration and refreshes view', () => {
    const mockWebviewView = {
      description: '',
      visible: true,
      webview: {
        options: {},
        html: '',
        onDidReceiveMessage: vi.fn(() => new vscode.Disposable(() => {})),
        postMessage: vi.fn().mockResolvedValue(true),
        asWebviewUri: (uri: vscode.Uri) => ({ toString: () => `vscode-resource://${uri.path}` }),
        cspSource: 'vscode-webview:',
      },
      onDidChangeVisibility: vi.fn(() => new vscode.Disposable(() => {})),
      onDidDispose: vi.fn(() => new vscode.Disposable(() => {})),
    } as unknown as vscode.WebviewView;

    provider.resolveWebviewView(
      mockWebviewView,
      {} as vscode.WebviewViewResolveContext,
      {} as vscode.CancellationToken
    );

    provider.setPorts(3005, 3006);
    expect(mockWebviewView.webview.html).toContain('http://localhost:3005');
  });
});
