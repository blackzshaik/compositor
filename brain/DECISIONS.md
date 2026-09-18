# Architectural Decision Records (ADRs)

This document records the foundational architectural decisions made in Compositor, including context, options considered, decisions, and consequences.

---

## ADR-001: Headless LayoutLib JVM Rendering vs. Emulators / Wasm

* **Date**: September 2026
* **Status**: Accepted
* **Context**:
  Developers outside Android Studio and AI agents need to visualize Jetpack Compose UI without booting an emulator (which takes 30-60 seconds) or opening Android Studio (which consumes 8+ GB RAM).
* **Options Considered**:
  1. *Compile to Kotlin Multiplatform (Compose Wasm / HTML)*: Rejected for native Android code because standard Android apps depend on Android SDK classes (`android.content.Context`, `R.string`, `androidx.compose.ui.tooling.preview.Preview`) which do not compile to Wasm.
  2. *Headless Android Emulator (via Docker/QEMU)*: Rejected due to heavy resource consumption, complex virtualization setup on host machines, and high startup latency.
  3. *Headless JVM LayoutLib Rendering (`compose-preview-renderer` / Paparazzi)*: Accepted. Android Studio itself uses LayoutLib to render `@Preview` without an emulator. Google has unbundled this into standalone Maven artifacts.
* **Decision**:
  Build `core-renderer` on top of the host JVM using LayoutLib / `compose-preview-renderer` to rasterize `@Preview` composables into in-memory bitmaps and export PNGs in under 2 seconds.
* **Consequences**:
  - Blazing fast turnaround (< 2s).
  - Works with unmodified Android SDK projects.
  - Requires JVM 21 and Android SDK platform libraries on the host machine.

---

## ADR-002: Modular Monorepo Architecture

* **Date**: September 2026
* **Status**: Accepted
* **Context**:
  Compositor consists of multiple distinct execution environments: a JVM renderer, a CLI daemon, a browser-based web viewer, and an MCP server.
* **Decision**:
  Structure the repository as a clean modular monorepo:
  - `core-renderer/`: Pure Kotlin JVM library.
  - `cli/`: Daemon and filesystem watcher.
  - `web-viewer/`: React/Vite/Tailwind frontend.
  - `mcp-server/`: Model Context Protocol server exposing tools to LLMs.
  - `samples/sample-app/`: Benchmark Compose Android app.
* **Consequences**:
  - Strict separation of concerns.
  - No circular dependencies or leaky abstractions.
  - Each module can be tested and evolved independently.

---

## ADR-003: Dual-Stack Quality Gates (Kotlin & TypeScript)

* **Date**: September 2026
* **Status**: Accepted
* **Context**:
  To prevent low-quality code ("AI slop") and ensure enterprise-grade stability, static analysis and code coverage must be enforced across both Kotlin and TypeScript codebases.
* **Decision**:
  - **Kotlin**:
    - Detekt (`1.23.8`) with custom ruleset in `config/detekt/detekt.yml` (120 char max line length, complexity limits, forbidden double-bang `!!`).
    - Kover (`0.9.9` by JetBrains) for automated test coverage reporting.
  - **TypeScript**:
    - ESLint 9+ with `@typescript-eslint` (strictly banning `any`, enforcing 120 char max line length, and unused variable prevention).
    - Prettier for consistent formatting.
    - Vitest with `@vitest/coverage-v8` for unit tests and code coverage thresholds.
* **Consequences**:
  - Automated verification of code quality before commits or PRs.
  - Breathing room provided during initial bootstrap: checks are runnable on demand via Gradle and npm scripts before being locked into hard git pre-commit hooks.

---

## ADR-004: IDE Extension Architecture & Webview IPC Protocol

* **Date**: September 2026
* **Status**: Accepted
* **Context**:
  Compose developers require preview visual feedback inside VS Code and Cursor without context switching to an external browser window.
* **Decision**:
  - Implement a dedicated `vscode-extension` package targeting VS Code `^1.90.0`.
  - Embed the built `web-viewer/dist` inside a `vscode.WebviewViewProvider` (sidebar) and `vscode.WebviewPanel` (editor tab) using strict Content-Security-Policy (CSP) with local resource URI rewriting (`webview.asWebviewUri`).
  - Implement zero-overhead lazy activation (`onLanguage:kotlin`, `workspaceContains:**/build.gradle.kts`).
  - Provide cursor-to-composable resolution (`resolvePreviewAtCursor`) to enable 1-click targeted re-rendering directly from editor caret positions.
* **Consequences**:
  - Zero-lag inline previews in VS Code & Cursor.
  - Full isolation: daemon runs independently on local ports 3001/3002 with output streamed to VS Code Output Channel.

---

## ADR-005: Decoupling from Paparazzi Spike to Native LayoutLib Engine

* **Date**: September 2026
* **Status**: Accepted
* **Context**:
  The initial Tracer Bullet used Cash App's Paparazzi as a quick spike to verify JVM rendering. However, Paparazzi is fundamentally a screenshot testing tool that requires developers to write JUnit test classes (`@Test fun snapshot()`) for every preview. A true developer preview tool must render `@Preview` composables directly from main source code with zero test boilerplate.
* **Decision**:
  Implement a dedicated, headless LayoutLib rendering engine in `core-renderer` that directly interfaces with Android's LayoutLib and Compose runtime via reflection, bypassing JUnit and test harnesses entirely.
* **Consequences**:
  - Developers write standard `@Preview` composables in regular source files without any test code.
  - Direct in-memory rendering session management with sub-second render times.

---

## ADR-006: Pure Kotlin Architecture (Ktor + Kotlin PSI + Zero-Node End User Runtime)

* **Date**: September 2026
* **Status**: Accepted
* **Context**:
  The tracer bullet daemon was written in TypeScript/Node.js. This required Android developers to install Node.js and npm in addition to JDK 21 and the Android SDK, creating unnecessary onboarding friction and separating the tooling from the native Android language.
* **Decision**:
  Re-architect the entire daemon, CLI, and AST parsing in **pure Kotlin**:
  - **Ktor**: Lightweight, coroutine-native HTTP and WebSocket server for daemon IPC.
  - **Clikt**: Idiomatic Kotlin CLI parser for command-line arguments.
  - **Kotlin Compiler Embedded PSI**: Native AST parsing of `.kt` source files to extract `@Preview` metadata without running full Gradle builds.
  - **Pre-compiled Web Viewer**: The React/Tailwind web viewer is built and bundled directly as static assets inside the JAR/plugin resources. The end user needs **zero Node.js/npm runtime**.
* **Consequences**:
  - Single runtime dependency for users: JDK 21 (which all Android developers already have).
  - Native coroutine concurrency and seamless code sharing across renderer and daemon.

---

## ADR-007: Shippable Entity via Compositor Gradle Plugin (`io.compositor`)

* **Date**: September 2026
* **Status**: Accepted
* **Context**:
  Compositor cannot remain confined to an internal sample submodule. It must be a standalone, shippable tool that any developer can easily apply to their existing external Android app on any machine.
* **Decision**:
  Package Compositor as a standard Android Gradle Plugin (`id("io.compositor")`):
  - Integrates natively with the Android Gradle Plugin (AGP) artifact collections to automatically resolve the project's compiled classpath, merged Android resources (`res/`), and manifest.
  - Registers clean Gradle tasks: `./gradlew compositor` (starts server and launches browser) and `./gradlew compositorRender`.
  - Also provide a standalone CLI binary (`compositor watch`) that attaches to projects via the Gradle Tooling API.
* **Consequences**:
  - One-line setup for existing Android projects: `plugins { id("io.compositor") version "0.1.0" }`.
  - Full, automatic access to Android resources (`R.string`, `R.drawable`, `R.style`), avoiding missing resource crashes in LayoutLib.

---

## ADR-008: Compose Multiplatform (Kotlin/Wasm) for Web Viewer

* **Date**: September 2026
* **Status**: Accepted
* **Context**:
  The web viewer frontend was initially envisioned in React/TypeScript, which required `npm`, `package.json`, and `node_modules` in the repository. This contradicted the goal of having a pure-Kotlin Android developer experience and fragmented the codebase across two different toolchains.
* **Decision**:
  Re-architect the `web-viewer` as a **Compose Multiplatform (CMP) for Web** application targeting **Kotlin/Wasm (`wasmJs`)**:
  - Built 100% in Kotlin using Jetpack Compose UI primitives (`@Composable`, Material 3).
  - Compiled directly by Gradle via `org.jetbrains.compose` and `wasmJsBrowserDistribution`.
  - Renders directly onto an HTML5 `<canvas>` via Skiko/WasmGC with 60/120fps hardware acceleration.
  - Interacts with the Ktor daemon over Ktor Client WebSockets in pure Kotlin.
  - Purges `npm`, `package.json`, and `node_modules` entirely from the repository.
* **Consequences**:
  - 100% Kotlin across the entire repository (Backend, Renderer, and Frontend).
  - Single unified build system: `./gradlew build` builds everything.
  - Zero npm or Node.js dependencies for both developers and users.

