# Changelog

All notable changes to **Compositor** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0-alpha01] - 2026-09-27

### Added
* **Headless LayoutLib Engine (`core-renderer`)**:
  * Unbundled host JVM rasterization of Jetpack Compose `@Preview` composables without launching Android Studio or booting an emulator.
  * Embedded Kotlin PSI AST parser to discover previews across multi-module projects.
  * Reactive file watcher with coroutine debouncing for instant hot reload.
  * Ktor Netty HTTP & WebSocket server for live preview streaming.
* **Compositor Gradle Plugin (`io.compositor`)**:
  * Standalone Gradle plugin resolving AGP classpath, compile-classpath, and Android merged resources automatically.
  * Added tasks: `compositor` (live preview server), `compositorRender` (headless batch PNG rasterization), and `compositorMcp` (stdio MCP server).
* **Dual Web Viewer**:
  * **React Web Viewer (`web-viewer-react`)**: High-performance browser preview viewer with Google Pixel 8 and Samsung Galaxy S24 device bezels, light/dark theme toggle, font-scaling slider (0.85x - 1.5x), and zoom controls. Embedded directly in the daemon JAR.
  * **Compose Multiplatform Wasm Viewer (`web-viewer`)**: Experimental Kotlin/Wasm canvas implementation.
* **AI Vision Bridge (Model Context Protocol - MCP)**:
  * Embedded JetBrains Kotlin MCP SDK supporting 5 tools: `list_previews`, `render_preview`, `get_preview_image`, `inspect_layout_tree`, and `compare_previews`.
  * AI coding assistants (Cursor, Antigravity, Claude Desktop) can visually inspect Compose UI layouts.
* **VS Code & Cursor Extension (`compositor-vscode`)**:
  * Sidebar webview integrating live preview canvas directly inside the editor.
  * Active caret tracking to auto-focus the preview under cursor.
  * Automatic workspace Gradle daemon lifecycle management.
* **Community & Open Source Infrastructure**:
  * Apache 2.0 license.
  * Contributor Covenant v2.1 Code of Conduct (`CODE_OF_CONDUCT.md`).
  * Security disclosure policy (`SECURITY.md`).
  * GitHub Actions CI (`ci.yml`) and automated release packaging (`release.yml`).
  * Issue templates for bug reports and feature requests.

### Known Limitations (Alpha)
* Preview Parameter Providers (`@PreviewParameter`) are parsed with default constructor or first parameter instance.
* Interactive Compose state manipulation is view-only (no touch/gesture interaction yet).
* Custom font assets must be accessible via Android merged resources.
