import * as vscode from 'vscode';
import path from 'node:path';
import { getWebviewContent, getDistPath, WebviewHtmlOptions } from './htmlHelper.js';

export class CompositorPanel {
  public static currentPanel: CompositorPanel | undefined;
  public static readonly viewType = 'compositor.editorPanel';

  private readonly _panel: vscode.WebviewPanel;
  private readonly _extensionUri: vscode.Uri;
  private _disposables: vscode.Disposable[] = [];
  private _options: WebviewHtmlOptions;

  public static createOrShow(
    extensionUri: vscode.Uri,
    options: WebviewHtmlOptions = {}
  ): CompositorPanel {
    const column = vscode.window.activeTextEditor
      ? vscode.ViewColumn.Beside
      : vscode.ViewColumn.One;

    if (CompositorPanel.currentPanel) {
      CompositorPanel.currentPanel._panel.reveal(column);
      if (options.activePreviewId) {
        CompositorPanel.currentPanel.setActivePreview(options.activePreviewId, options.activeFilePath);
      }
      return CompositorPanel.currentPanel;
    }

    const distPath = getDistPath(extensionUri);

    const panel = vscode.window.createWebviewPanel(
      CompositorPanel.viewType,
      'Compositor Preview',
      column,
      {
        enableScripts: true,
        retainContextWhenHidden: true,
        localResourceRoots: [
          extensionUri,
          vscode.Uri.file(distPath),
        ],
      }
    );

    CompositorPanel.currentPanel = new CompositorPanel(panel, extensionUri, options);
    return CompositorPanel.currentPanel;
  }

  private constructor(
    panel: vscode.WebviewPanel,
    extensionUri: vscode.Uri,
    options: WebviewHtmlOptions
  ) {
    this._panel = panel;
    this._extensionUri = extensionUri;
    this._options = options;

    this.update();

    this._panel.onDidDispose(() => this.dispose(), null, this._disposables);

    this._panel.webview.onDidReceiveMessage(
      (message: unknown) => {
        if (typeof message === 'object' && message !== null) {
          console.info('[Compositor Editor Panel] Message from webview:', message);
        }
      },
      null,
      this._disposables
    );
  }

  public setActivePreview(previewId: string | null, filePath?: string | null): void {
    this._options.activePreviewId = previewId;
    this._options.activeFilePath = filePath ?? null;
    this._panel.webview.postMessage({
      type: 'ACTIVE_PREVIEW_CHANGED',
      previewId,
      filePath,
    });
  }

  public postMessage(message: unknown): void {
    this._panel.webview.postMessage(message);
  }

  public update(): void {
    this._panel.webview.html = getWebviewContent(
      this._panel.webview,
      this._extensionUri,
      this._options
    );
  }

  public dispose(): void {
    CompositorPanel.currentPanel = undefined;
    this._panel.dispose();
    while (this._disposables.length) {
      const x = this._disposables.pop();
      if (x) {
        x.dispose();
      }
    }
  }
}
