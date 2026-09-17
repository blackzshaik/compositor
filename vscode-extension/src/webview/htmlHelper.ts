import * as vscode from 'vscode';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

export interface WebviewHtmlOptions {
  serverPort?: number;
  wsPort?: number;
  activePreviewId?: string | null;
  activeFilePath?: string | null;
}

export function getNonce(): string {
  return crypto.randomBytes(16).toString('hex');
}

/**
 * Generates the complete HTML content for the Compositor Webview,
 * rewriting web-viewer assets to local webview URIs and injecting strict CSP.
 */
export function getWebviewContent(
  webview: vscode.Webview,
  extensionUri: vscode.Uri,
  options: WebviewHtmlOptions = {}
): string {
  const nonce = getNonce();
  const serverPort = options.serverPort ?? 3001;
  const wsPort = options.wsPort ?? 3002;
  const activePreviewId = options.activePreviewId ?? null;

  // Path to web-viewer/dist
  const distPath = path.resolve(extensionUri.fsPath, '..', 'web-viewer', 'dist');
  const indexHtmlPath = path.join(distPath, 'index.html');

  if (fs.existsSync(indexHtmlPath)) {
    try {
      let html = fs.readFileSync(indexHtmlPath, 'utf-8');

      // 1. Rewrite asset references (JS, CSS) to webview URIs
      html = html.replace(/(?:src|href)="\/assets\/([^"]+)"/g, (_match, filename: string) => {
        const fileUri = vscode.Uri.file(path.join(distPath, 'assets', filename));
        const webviewUri = webview.asWebviewUri(fileUri);
        return `${_match.startsWith('src') ? 'src' : 'href'}="${webviewUri.toString()}"`;
      });

      // 2. Build strict Content Security Policy
      const csp = [
        "default-src 'none'",
        `img-src ${webview.cspSource} http://localhost:* https: data: blob:`,
        `script-src 'nonce-${nonce}' ${webview.cspSource}`,
        `style-src ${webview.cspSource} 'unsafe-inline'`,
        `font-src ${webview.cspSource}`,
        `connect-src http://localhost:* ws://localhost:* ws://127.0.0.1:* http://127.0.0.1:*`,
      ].join('; ');

      const cspMeta = `<meta http-equiv="Content-Security-Policy" content="${csp}">`;

      // 3. Inject bridge script and configuration
      const bridgeScript = `
        <script nonce="${nonce}">
          window.__COMPOSITOR_CONFIG__ = {
            serverPort: ${serverPort},
            wsPort: ${wsPort},
            httpBase: "http://localhost:${serverPort}",
            wsUrl: "ws://localhost:${wsPort}",
            activePreviewId: ${JSON.stringify(activePreviewId)}
          };
          try {
            window.vscode = acquireVsCodeApi();
          } catch (e) {
            // Already acquired or not supported
          }
          window.addEventListener('message', function(event) {
            window.dispatchEvent(new CustomEvent('compositor-vscode-event', { detail: event.data }));
          });
        </script>
      `;

      // Replace or insert CSP meta tag
      if (html.includes('<head>')) {
        html = html.replace('<head>', `<head>\n    ${cspMeta}\n    ${bridgeScript}`);
      } else {
        html = `${cspMeta}\n${bridgeScript}\n${html}`;
      }

      return html;
    } catch {
      // Fall through to fallback page
    }
  }

  // Fallback HTML when web-viewer/dist is not available or loading fails
  const cspFallback = [
    "default-src 'none'",
    `img-src ${webview.cspSource} http://localhost:* data:`,
    `style-src 'unsafe-inline'`,
    `script-src 'nonce-${nonce}'`,
    `connect-src http://localhost:* ws://localhost:*`,
  ].join('; ');

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta http-equiv="Content-Security-Policy" content="${cspFallback}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Compositor Studio</title>
  <style>
    body {
      background-color: #0a0a0a;
      color: #e5e5e5;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      margin: 0;
      padding: 24px;
      box-sizing: border-box;
      text-align: center;
    }
    .card {
      background: #171717;
      border: 1px solid #262626;
      border-radius: 12px;
      padding: 32px;
      max-width: 480px;
    }
    h2 { margin-top: 0; color: #a855f7; font-size: 1.4rem; }
    p { color: #a3a3a3; font-size: 0.95rem; line-height: 1.5; }
    .status {
      display: inline-block;
      padding: 6px 14px;
      border-radius: 9999px;
      background: #27272a;
      color: #38bdf8;
      font-size: 0.85rem;
      margin-top: 16px;
    }
  </style>
</head>
<body>
  <div class="card">
    <h2>🎨 Compositor Live Preview</h2>
    <p>Connecting to Compositor Preview Server at <code>http://localhost:${serverPort}</code>...</p>
    <div class="status" id="status-text">Checking daemon status...</div>
  </div>
  <script nonce="${nonce}">
    const statusEl = document.getElementById('status-text');
    function checkStatus() {
      fetch('http://localhost:${serverPort}/api/daemon/status')
        .then(res => res.json())
        .then(data => {
          statusEl.textContent = 'Daemon Running (' + data.totalPreviews + ' previews loaded)';
          statusEl.style.color = '#4ade80';
        })
        .catch(() => {
          statusEl.textContent = 'Daemon Offline — Click Status Bar to Start';
          statusEl.style.color = '#f87171';
        });
    }
    checkStatus();
    setInterval(checkStatus, 3000);
  </script>
</body>
</html>`;
}
