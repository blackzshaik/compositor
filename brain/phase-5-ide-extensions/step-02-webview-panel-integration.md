# Step 02: Sidebar Webview Panel Integration

*Phase*: 4 — IDE Extensions & Plugin Ecosystem  
*Status*: Blocked by Step 01  
*Target Module*: `vscode-extension`

---

## 1. Objective
Implement a `WebviewViewProvider` that mounts the Compositor Web Viewer dashboard directly inside the VS Code / Cursor sidebar panel (or opens it as an editor tab alongside code).

---

## 2. Functional Requirements
* **Webview Provider**:
  * Implement `vscode.WebviewViewProvider` to manage the sidebar view lifecycle.
  * Load the built `web-viewer/dist` HTML, CSS, and JS bundle into the webview.
* **Security & CSP**:
  * Configure strict `Content-Security-Policy` permitting communication only with local Compositor daemon endpoints (`http://localhost:3001` and `ws://localhost:3002`).
* **Bidirectional Communication**:
  * Pass messages between the VS Code extension host and webview (e.g. active file in editor -> auto-focus matching preview in webview).

---

## 3. High-Level Architectural Guidance
* Support dynamic layout switching: allow the user to dock the preview in the primary sidebar, secondary sidebar, or open as an editor tab (`vscode.window.createWebviewPanel`).
* Preserve webview state when the panel is hidden (`retainContextWhenHidden: true`) to avoid visual flashing during tab switches.

---

## 4. Verification & Quality Gates
* **Extension Host Test**: Launch extension in an Extension Development Host instance; assert the sidebar webview initializes and connects to the local preview server successfully.
