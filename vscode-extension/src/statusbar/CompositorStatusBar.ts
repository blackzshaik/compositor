import * as vscode from 'vscode';
import { COMMANDS } from '../constants.js';

export type DaemonConnectionState = 'offline' | 'connecting' | 'live' | 'rendering';

export class CompositorStatusBar {
  private readonly _statusBarItem: vscode.StatusBarItem;
  private _state: DaemonConnectionState = 'offline';
  private _previewCount: number = 0;
  private _httpPort: number = 3001;

  constructor() {
    this._statusBarItem = vscode.window.createStatusBarItem(
      vscode.StatusBarAlignment.Right,
      100
    );
    this._statusBarItem.command = COMMANDS.SHOW_QUICK_MENU;
    this.updateItem();
  }

  public setHttpPort(port: number): void {
    this._httpPort = port;
    this.updateItem();
  }

  public setLive(count: number): void {
    this._state = 'live';
    this._previewCount = count;
    this.updateItem();
  }

  public setOffline(): void {
    this._state = 'offline';
    this.updateItem();
  }

  public setConnecting(): void {
    this._state = 'connecting';
    this.updateItem();
  }

  public setRendering(isRendering: boolean): void {
    if (isRendering) {
      this._state = 'rendering';
    } else {
      this._state = 'live';
    }
    this.updateItem();
  }

  public getState(): DaemonConnectionState {
    return this._state;
  }

  public getPreviewCount(): number {
    return this._previewCount;
  }

  public show(): void {
    this._statusBarItem.show();
  }

  public hide(): void {
    this._statusBarItem.hide();
  }

  public dispose(): void {
    this._statusBarItem.dispose();
  }

  private updateItem(): void {
    switch (this._state) {
      case 'offline':
        this._statusBarItem.text = '$(plug) Compositor: Offline';
        this._statusBarItem.tooltip = 'Compositor preview daemon is offline. Click to start or configure.';
        this._statusBarItem.backgroundColor = undefined;
        break;

      case 'connecting':
        this._statusBarItem.text = '$(sync~spin) Compositor: Connecting...';
        this._statusBarItem.tooltip = `Attempting to connect to Compositor daemon at http://localhost:${this._httpPort}...`;
        this._statusBarItem.backgroundColor = undefined;
        break;

      case 'live':
        this._statusBarItem.text = `$(paintcan) Compositor: Live (${this._previewCount})`;
        this._statusBarItem.tooltip = `Compositor live preview active on :${this._httpPort} (${this._previewCount} previews loaded). Click for actions.`;
        this._statusBarItem.backgroundColor = undefined;
        break;

      case 'rendering':
        this._statusBarItem.text = '$(sync~spin) Compositor: Rendering...';
        this._statusBarItem.tooltip = 'Incremental Compose preview rasterization in progress...';
        this._statusBarItem.backgroundColor = undefined;
        break;
    }
  }
}
