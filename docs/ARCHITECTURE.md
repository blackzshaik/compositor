# Compositor Architecture Specification

## 1. System Overview

Compositor bridges the gap between native Android Jetpack Compose development and lightweight development environments (VS Code, Cursor, terminal) as well as AI coding agents.

Instead of running an Android emulator or launching Android Studio, Compositor executes `@Preview` composables headlessly on the host JVM using Android's **LayoutLib** engine (`compose-preview-renderer`), renders high-resolution raster images, and streams them in real-time to a local web viewer and an MCP (Model Context Protocol) API.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             COMPOSITOR SYSTEM                               │
└─────────────────────────────────────────────────────────────────────────────┘

 [ Android Project Source (*.kt) ]
                │
                ▼ (Filesystem Watcher)
 ┌──────────────────────────────────────────────────┐
 │ Compositor Daemon (CLI)                          │
 │  • Monitors changes in @Composable files         │
 │  • Triggers incremental compilation              │
 └──────────────────────┬───────────────────────────┘
                        │
                        ▼ (JVM In-Process / CLI Call)
 ┌──────────────────────────────────────────────────┐
 │ Core Preview Renderer                            │
 │  • Resolves classpath & Android SDK resources   │
 │  • Executes @Preview via LayoutLib Engine        │
 │  • Generates high-res PNG / WebP frame buffer    │
 └──────────────────────┬───────────────────────────┘
                        │
                        ▼ (Internal Event Bus)
 ┌──────────────────────────────────────────────────┐
 │ Local Server (HTTP & WebSocket)                  │
 └──────────┬───────────────────────────┬───────────┘
            │ (WebSocket push)          │ (REST / MCP Tools)
            ▼                           ▼
 ┌─────────────────────┐    ┌───────────────────────────────────┐
 │ Web Viewer UI       │    │ AI Agent Bridge (MCP Server)      │
 │ • Device mockup     │    │ • Tool: render_preview(name)      │
 │ • Dark/Light toggle │    │ • Tool: get_preview_image()       │
 │ • Font scaling      │    │ • Tool: get_layout_hierarchy()    │
 │ • Multi-preview grid│    │                                   │
 │ (For Developers)    │    │ (For AI: Antigravity, Cursor, etc)│
 └─────────────────────┘    └───────────────────────────────────┘
```

---

## 2. Subsystem Breakdown

### 2.1. Core Preview Renderer (`core-renderer`)
* **Technology**: Kotlin / Java Virtual Machine (Java 21 target).
* **Core Responsibilities**:
  * Discover `@Preview` annotations across source files via AST parsing (Kotlin PSI or Bytecode inspection).
  * Construct the runtime classpath combining project compile dependencies, Android framework JARs, and Compose runtime libraries.
  * Load and initialize Google's `compose-preview-renderer` / LayoutLib bridge.
  * Rasterize composables into standard in-memory `BufferedImage` objects and export to PNG/WebP.
  * Extract layout bounds (bounding boxes, semantics) to support inspectable overlay UI and AI spatial understanding.

### 2.2. Daemon & CLI Orchestrator (`cli`)
* **Technology**: Kotlin / Node CLI.
* **Core Responsibilities**:
  * Project root detection and `build.gradle.kts` inspection.
  * Efficient file watching (ignoring non-UI changes like test fixtures or build outputs).
  * Incremental build caching to achieve sub-2-second preview turnaround.
  * Hosting the local HTTP and WebSocket server for real-time frontend syncing.

### 2.3. Web Viewer Frontend (`web-viewer`)
* **Technology**: TypeScript, Vite, React / Tailwind CSS.
* **Core Responsibilities**:
  * Realistic device frames (Google Pixel, Samsung Galaxy, Foldable, Tablet).
  * Live hot-reload updates via WebSocket connection without full page reload.
  * Developer controls:
    * Light / Dark mode toggle.
    * Font scaling slider (0.85x to 1.5x).
    * Screen orientation (Portrait / Landscape).
    * Multi-preview grid to compare multiple `@Preview` configurations side-by-side.
  * Error boundary display showing compilation or rendering errors with actionable stack traces.

### 2.4. AI Agent Bridge (`mcp-server`)
* **Technology**: TypeScript (Node.js) implementing Model Context Protocol (MCP) spec.
* **Core Responsibilities**:
  * Exposes tools to AI assistants (Antigravity, Cursor, Claude Code, Cline, etc.):
    * `list_previews()`: Returns all discoverable `@Preview` composables in the project.
    * `render_preview(composableName)`: Triggers a fresh render of the requested preview.
    * `get_preview_image(composableName)`: Returns base64 PNG data for multimodal vision models.
    * `get_preview_diagnostics()`: Returns layout warnings, overflow detection, or contrast suggestions.

---

## 3. Communication Protocols

### 3.1. WebSocket Frame Schema (Daemon -> Web Viewer)
```json
{
  "event": "PREVIEW_RENDERED",
  "payload": {
    "previewId": "com.example.app.ui.GreetingPreview",
    "timestamp": 1789500000000,
    "imageUrl": "/api/previews/com.example.app.ui.GreetingPreview.png?v=1789500000000",
    "dimensions": { "width": 1080, "height": 2400 },
    "renderDurationMs": 842,
    "hasErrors": false
  }
}
```

### 3.2. Error Event Schema
```json
{
  "event": "RENDER_ERROR",
  "payload": {
    "previewId": "com.example.app.ui.GreetingPreview",
    "message": "Unresolved reference: LocalAppTheme",
    "stackTrace": "...",
    "sourceFile": "Greeting.kt",
    "line": 42
  }
}
```

---

## 4. Performance Goals

* **Cold Start Render**: < 5 seconds.
* **Incremental Hot Reload (File change to Web Viewer update)**: < 1.8 seconds.
* **Memory Footprint**: < 350 MB JVM heap (significantly lighter than Android Studio's 4-8 GB).
