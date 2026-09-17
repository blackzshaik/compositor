# Phase 4: IDE Extensions & Plugin Ecosystem (VS Code / Cursor)

## Mission
Bring Compositor directly into the developer's primary IDE environment (VS Code and Cursor). By embedding the Web Viewer inside an interactive sidebar panel and providing command palette integration, developers can preview Compose UI side-by-side with their editor without opening an external browser window.

---

## Architecture Overview
```
┌────────────────────────────────────────────────────────┐
│ VS Code / Cursor Window                                │
├──────────────────────────┬─────────────────────────────┤
│ Editor Pane              │ Compositor Sidebar Webview  │
│                          │                             │
│  @Composable             │  ┌───────────────────────┐  │
│  fun Greeting(...) {     │  │ Pixel Mockup          │  │
│      ...                 │  │ (Live WebSocket Sync) │  │
│  }                       │  └───────────────────────┘  │
│                          │                             │
├──────────────────────────┴─────────────────────────────┤
│ Status Bar: [🎨 Compositor: Live Sync (3 Previews)]    │
└────────────────────────────────────────────────────────┘
```

---

## Step Progression

| Step | File | Scope |
| :--- | :--- | :--- |
| **01** | [`step-01-vscode-extension-scaffolding.md`](./step-01-vscode-extension-scaffolding.md) | Initialize VS Code extension package with TypeScript and manifest. |
| **02** | [`step-02-webview-panel-integration.md`](./step-02-webview-panel-integration.md) | Build WebviewViewProvider hosting the `web-viewer` dashboard. |
| **03** | [`step-03-commands-and-status-bar.md`](./step-03-commands-and-status-bar.md) | Add command palette actions and status bar live-sync indicator. |
| **04** | [`step-04-auto-detection-and-workspace-lifecycle.md`](./step-04-auto-detection-and-workspace-lifecycle.md) | Implement Android workspace auto-detection and daemon process lifecycle. |

---

## Protocol for AI Agents
1. Ensure Phases 1, 2, and 3 are stable before implementing Phase 4.
2. Adhere to VS Code Extension Guidelines and strict security boundaries (`Content-Security-Policy`).
3. Maintain zero-overhead activation: do not activate the extension unless an Android project or `.kt` file is open.
