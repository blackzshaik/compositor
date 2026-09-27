import * as vscode from 'vscode';
import { getWebviewContent, getDistPath } from './htmlHelper.js';
import { VIEWS } from '../constants.js';

export interface WebviewMessage {
  command: string;
  previewId?: string;
  message?: string;
  [key: string]: unknown;
}

export class CompositorWebviewViewProvider implements vscode.WebviewViewProvider {
  public static readonly viewType = VIEWS.PREVIEW_VIEW;

  private _view?: vscode.WebviewView;
  private _serverPort: number = 3001;
  private _wsPort: number = 3002;
  private _activePreviewId: string | null = null;
  private _activeFilePath: string | null = null;

  constructor(
    private readonly _extensionUri: vscode.Uri,
    private readonly _onMessage?: (message: WebviewMessage) => void
  ) {}

  public resolveWebviewView(
    webviewView: vscode.WebviewView,
    _context: vscode.WebviewViewResolveContext,
    _token: vscode.CancellationToken
  ): void {
    this._view = webviewView;

    const distPath = getDistPath(this._extensionUri);

    webviewView.webview.options = {
      enableScripts: true,
      localResourceRoots: [
        this._extensionUri,
        vscode.Uri.file(distPath),
      ],
    };

    webviewView.description = 'Live Compose Preview';
    this.updateHtml();

    webviewView.webview.onDidReceiveMessage((message: WebviewMessage) => {
      if (this._onMessage) {
        this._onMessage(message);
      }
    });

    webviewView.onDidChangeVisibility(() => {
      if (webviewView.visible && this._activePreviewId) {
        this.postMessage({
          type: 'ACTIVE_PREVIEW_CHANGED',
          previewId: this._activePreviewId,
          filePath: this._activeFilePath,
        });
      }
    });
  }

  public setPorts(serverPort: number, wsPort: number): void {
    this._serverPort = serverPort;
    this._wsPort = wsPort;
    this.updateHtml();
  }

  public setActivePreview(previewId: string | null, filePath?: string | null): void {
    this._activePreviewId = previewId;
    this._activeFilePath = filePath ?? null;
    this.postMessage({
      type: 'ACTIVE_PREVIEW_CHANGED',
      previewId,
      filePath: this._activeFilePath,
    });
  }

  public postMessage(message: unknown): void {
    if (this._view && this._view.visible) {
      this._view.webview.postMessage(message);
    }
  }

  public refresh(): void {
    this.updateHtml();
  }

  private updateHtml(): void {
    if (this._view) {
      this._view.webview.html = getWebviewContent(this._view.webview, this._extensionUri, {
        serverPort: this._serverPort,
        wsPort: this._wsPort,
        activePreviewId: this._activePreviewId,
        activeFilePath: this._activeFilePath,
      });
    }
  }
}
