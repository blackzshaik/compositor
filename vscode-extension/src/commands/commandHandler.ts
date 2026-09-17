import * as vscode from 'vscode';
import { COMMANDS } from '../constants.js';
import { resolvePreviewAtCursor } from './cursorResolver.js';
import { CompositorPanel } from '../webview/CompositorPanel.js';
import { CompositorWebviewViewProvider } from '../webview/CompositorWebviewViewProvider.js';
import { DaemonManager } from '../daemon/DaemonManager.js';

export interface CommandContext {
  extensionUri: vscode.Uri;
  daemonManager: DaemonManager;
  webviewProvider: CompositorWebviewViewProvider;
  outputChannel: vscode.OutputChannel;
  serverPort: number;
  wsPort: number;
}

export function registerCommands(ctx: CommandContext): vscode.Disposable[] {
  const disposables: vscode.Disposable[] = [];

  // 1. Open Preview to the Side
  disposables.push(
    vscode.commands.registerCommand(COMMANDS.OPEN_PREVIEW_TO_SIDE, async () => {
      const editor = vscode.window.activeTextEditor;
      let activePreviewId: string | null = null;
      let activeFilePath: string | null = null;

      if (editor && editor.document.languageId === 'kotlin') {
        activeFilePath = editor.document.uri.fsPath;
        const target = resolvePreviewAtCursor(editor.document.getText(), editor.selection.active.line);
        if (target) {
          activePreviewId = target.previewId;
        }
      }

      CompositorPanel.createOrShow(ctx.extensionUri, {
        serverPort: ctx.serverPort,
        wsPort: ctx.wsPort,
        activePreviewId,
        activeFilePath,
      });
    })
  );

  // 2. Focus Preview Sidebar
  disposables.push(
    vscode.commands.registerCommand(COMMANDS.FOCUS_SIDEBAR, async () => {
      await vscode.commands.executeCommand('compositor.previewView.focus');
    })
  );

  // 3. Re-render Active Preview
  disposables.push(
    vscode.commands.registerCommand(COMMANDS.RERENDER_ACTIVE_PREVIEW, async () => {
      const editor = vscode.window.activeTextEditor;
      if (!editor || editor.document.languageId !== 'kotlin') {
        vscode.window.showWarningMessage('Open a Kotlin file with @Preview to re-render.');
        return;
      }

      const sourceText = editor.document.getText();
      const cursorLine = editor.selection.active.line;
      const target = resolvePreviewAtCursor(sourceText, cursorLine);

      if (!target) {
        vscode.window.showWarningMessage('No @Preview or @Composable found at current cursor.');
        return;
      }

      vscode.window.showInformationMessage(`[Compositor] Re-rendering: ${target.previewId}`);
      const success = await ctx.daemonManager.triggerRender(target.previewId);
      if (!success) {
        vscode.window.showErrorMessage(`Failed to enqueue render for ${target.previewId}`);
      }
    })
  );

  // 4. Toggle Preview Theme
  disposables.push(
    vscode.commands.registerCommand(COMMANDS.TOGGLE_THEME, () => {
      ctx.webviewProvider.postMessage({ type: 'TOGGLE_THEME' });
      if (CompositorPanel.currentPanel) {
        CompositorPanel.currentPanel.postMessage({ type: 'TOGGLE_THEME' });
      }
      vscode.window.showInformationMessage('Compositor: Toggled preview theme.');
    })
  );

  // 5. Start Daemon
  disposables.push(
    vscode.commands.registerCommand(COMMANDS.START_DAEMON, async () => {
      vscode.window.showInformationMessage('Compositor: Starting preview daemon...');
      await ctx.daemonManager.start();
    })
  );

  // 6. Stop Daemon
  disposables.push(
    vscode.commands.registerCommand(COMMANDS.STOP_DAEMON, async () => {
      await ctx.daemonManager.stop();
      vscode.window.showInformationMessage('Compositor: Daemon stopped.');
    })
  );

  // 7. Restart Daemon
  disposables.push(
    vscode.commands.registerCommand(COMMANDS.RESTART_DAEMON, async () => {
      vscode.window.showInformationMessage('Compositor: Restarting preview daemon...');
      await ctx.daemonManager.restart();
    })
  );

  // 8. Show Quick Menu
  disposables.push(
    vscode.commands.registerCommand(COMMANDS.SHOW_QUICK_MENU, async () => {
      const isOnline = ctx.daemonManager.isConnected;
      const items: (vscode.QuickPickItem & { command: string })[] = [
        {
          label: '$(split-horizontal) Open Preview to the Side',
          description: 'Dock live Compose preview alongside code',
          command: COMMANDS.OPEN_PREVIEW_TO_SIDE,
        },
        {
          label: '$(layout-sidebar-left) Focus Sidebar Canvas',
          description: 'Focus Compositor sidebar panel',
          command: COMMANDS.FOCUS_SIDEBAR,
        },
        {
          label: '$(refresh) Re-render Active Preview',
          description: 'Trigger immediate rasterization of composable at cursor',
          command: COMMANDS.RERENDER_ACTIVE_PREVIEW,
        },
        {
          label: '$(color-mode) Toggle Theme (Light/Dark)',
          description: 'Toggle preview theme mode',
          command: COMMANDS.TOGGLE_THEME,
        },
        {
          label: isOnline ? '$(debug-restart) Restart Daemon' : '$(play) Start Daemon',
          description: isOnline ? 'Restart file watcher and preview server' : 'Start background preview server',
          command: isOnline ? COMMANDS.RESTART_DAEMON : COMMANDS.START_DAEMON,
        },
        {
          label: '$(output) Show Compositor Logs',
          description: 'View daemon stdout and diagnostic output',
          command: 'compositor.showLogs',
        },
      ];

      const selected = await vscode.window.showQuickPick(items, {
        placeHolder: `Compositor Preview Actions (${isOnline ? 'Online' : 'Offline'})`,
      });

      if (selected) {
        if (selected.command === 'compositor.showLogs') {
          ctx.outputChannel.show();
        } else {
          await vscode.commands.executeCommand(selected.command);
        }
      }
    })
  );

  return disposables;
}
