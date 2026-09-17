import * as vscode from 'vscode';
import { VIEWS, CONFIG_KEYS, DEFAULTS } from './constants.js';
import { CompositorStatusBar } from './statusbar/CompositorStatusBar.js';
import { CompositorWebviewViewProvider } from './webview/CompositorWebviewViewProvider.js';
import { WorkspaceDetector } from './workspace/WorkspaceDetector.js';
import { DaemonManager } from './daemon/DaemonManager.js';
import { registerCommands } from './commands/commandHandler.js';
import { resolvePreviewAtCursor } from './commands/cursorResolver.js';
import { CompositorPanel } from './webview/CompositorPanel.js';

let daemonManager: DaemonManager | null = null;
let statusBar: CompositorStatusBar | null = null;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
  const outputChannel = vscode.window.createOutputChannel(DEFAULTS.OUTPUT_CHANNEL_NAME);
  context.subscriptions.push(outputChannel);

  outputChannel.appendLine('[Compositor] Extension activating...');

  // 1. Read configurations
  const config = vscode.workspace.getConfiguration();
  const autoStartDaemon = config.get<boolean>(CONFIG_KEYS.AUTO_START_DAEMON, true);
  const serverPort = config.get<number>(CONFIG_KEYS.SERVER_PORT, DEFAULTS.HTTP_PORT);
  const wsPort = config.get<number>(CONFIG_KEYS.WS_PORT, DEFAULTS.WS_PORT);
  const jdkPathOverride = config.get<string>(CONFIG_KEYS.JDK_PATH, '');
  const androidSdkOverride = config.get<string>(CONFIG_KEYS.ANDROID_SDK_PATH, '');

  const workspaceRoot = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? process.cwd();

  // 2. Detect workspace environment
  const detection = WorkspaceDetector.detect(workspaceRoot, {
    jdkPath: jdkPathOverride || undefined,
    androidSdkPath: androidSdkOverride || undefined,
  });

  outputChannel.appendLine(`[Compositor] Workspace: ${workspaceRoot}`);
  outputChannel.appendLine(`[Compositor] Android Project: ${detection.isAndroidProject ? 'Yes' : 'No'}`);
  if (detection.androidSdkPath) {
    outputChannel.appendLine(`[Compositor] Android SDK: ${detection.androidSdkPath}`);
  }
  if (detection.jdkPath) {
    outputChannel.appendLine(`[Compositor] JDK 21: ${detection.jdkPath}`);
  }
  if (detection.issues.length > 0) {
    detection.issues.forEach((issue) => outputChannel.appendLine(`[Compositor Warning] ${issue}`));
  }

  // 3. Initialize Status Bar
  statusBar = new CompositorStatusBar();
  statusBar.setHttpPort(serverPort);
  statusBar.show();
  context.subscriptions.push({ dispose: () => statusBar?.dispose() });

  // 4. Initialize Webview View Provider
  const webviewProvider = new CompositorWebviewViewProvider(context.extensionUri, (message) => {
    outputChannel.appendLine(`[Compositor Webview] Message: ${JSON.stringify(message)}`);
  });
  webviewProvider.setPorts(serverPort, wsPort);

  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider(VIEWS.PREVIEW_VIEW, webviewProvider, {
      webviewOptions: { retainContextWhenHidden: true },
    })
  );

  // 5. Initialize Daemon Manager
  daemonManager = new DaemonManager(
    {
      workspaceRoot,
      httpPort: serverPort,
      wsPort,
      jdkPath: detection.jdkPath ?? undefined,
      androidSdkPath: detection.androidSdkPath ?? undefined,
    },
    outputChannel
  );

  daemonManager.addListener((event, payload) => {
    switch (event) {
      case 'WS_CONNECTED':
      case 'DAEMON_STARTED':
      case 'DAEMON_ONLINE':
        statusBar?.setLive(daemonManager?.totalPreviews ?? 0);
        break;

      case 'DAEMON_STOPPED':
      case 'DAEMON_OFFLINE':
      case 'DAEMON_ERROR':
        statusBar?.setOffline();
        break;

      case 'PREVIEW_RENDER_STARTED':
        statusBar?.setRendering(true);
        webviewProvider.postMessage({ type: 'PREVIEW_RENDER_STARTED', payload });
        CompositorPanel.currentPanel?.postMessage({ type: 'PREVIEW_RENDER_STARTED', payload });
        break;

      case 'PREVIEW_UPDATED':
        statusBar?.setRendering(false);
        webviewProvider.postMessage({ type: 'PREVIEW_UPDATED', payload });
        CompositorPanel.currentPanel?.postMessage({ type: 'PREVIEW_UPDATED', payload });
        break;

      case 'RENDER_ERROR':
        statusBar?.setRendering(false);
        webviewProvider.postMessage({ type: 'RENDER_ERROR', payload });
        CompositorPanel.currentPanel?.postMessage({ type: 'RENDER_ERROR', payload });
        break;
    }
  });

  context.subscriptions.push({ dispose: () => daemonManager?.dispose() });

  // 6. Register Commands
  const commands = registerCommands({
    extensionUri: context.extensionUri,
    daemonManager,
    webviewProvider,
    outputChannel,
    serverPort,
    wsPort,
  });
  context.subscriptions.push(...commands);

  // 7. Track Active Editor for Cursor / Preview Synchronization
  context.subscriptions.push(
    vscode.window.onDidChangeActiveTextEditor((editor) => {
      if (editor && editor.document.languageId === 'kotlin') {
        const target = resolvePreviewAtCursor(editor.document.getText(), editor.selection.active.line);
        const previewId = target?.previewId ?? null;
        webviewProvider.setActivePreview(previewId, editor.document.uri.fsPath);
        if (CompositorPanel.currentPanel) {
          CompositorPanel.currentPanel.setActivePreview(previewId, editor.document.uri.fsPath);
        }
      }
    })
  );

  // 8. Auto-start daemon if configured and in an Android project
  if (autoStartDaemon && detection.isAndroidProject) {
    statusBar.setConnecting();
    daemonManager.start().catch((err) => {
      outputChannel.appendLine(`[Compositor Error] Auto-start daemon failed: ${String(err)}`);
      statusBar?.setOffline();
    });
  }

  outputChannel.appendLine('[Compositor] Extension activation complete.');
}

export function deactivate(): void {
  if (daemonManager) {
    daemonManager.dispose();
    daemonManager = null;
  }
  if (statusBar) {
    statusBar.dispose();
    statusBar = null;
  }
}
