# Compositor 🎨📱

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-21-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6.svg?logo=typescript&logoColor=white)](https://www.typescriptlang.org)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](docs/CONTRIBUTING.md)

**Compositor** is an open-source, headless Jetpack Compose preview engine, live web viewer, and AI vision bridge.

It brings Android Studio's `@Preview` capability to **VS Code, Cursor, terminal environments, and AI coding assistants**—rendering native Compose UI directly in your browser without launching Android Studio or booting an emulator.

---

## ⚡ The Problem

When developing native Android applications outside Android Studio (e.g. in Cursor, VS Code, or terminal) or when collaborating with AI coding agents:
1. **Developers are blind to UI changes**: Even adjusting a 4dp margin requires launching a heavy emulator, building a full APK, and deploying via `adb`—a 30 to 60-second cycle.
2. **AI assistants cannot "see" Compose code**: While web AI agents benefit from instant DOM feedback and headless browser screenshots, Android AI agents are completely blind to Jetpack Compose visual output.
3. **Android Studio is resource-heavy**: Keeping Android Studio open alongside modern AI tools consumes 8+ GB of RAM and drains battery life.

---

## 💡 The Solution

**Compositor** decouples the Compose preview engine from Android Studio:
* Executes `@Preview` composables on the host desktop JVM using Android's headless **LayoutLib** engine (`compose-preview-renderer`).
* Rasterizes UI components to crisp, high-resolution bitmaps in **sub-2 seconds**.
* Streams the rendered output over WebSockets to a sleek, responsive **browser-based device mockup**.
* Exposes a **Model Context Protocol (MCP)** server so AI agents (Cursor, Antigravity, Claude) can inspect screenshots and layout bounds directly to self-correct UI layout bugs!

```
┌────────────────────────────────────────────────────────┐
│                   Local Machine                        │
│                                                        │
│  [Android Source (*.kt)]                               │
│           │                                            │
│    (File Watcher)                                      │
│           ▼                                            │
│  [Compositor Daemon (CLI)]                             │
│    • Headless LayoutLib execution on JVM               │
│    • High-res frame rasterization (~1-2 sec)           │
│           │                                            │
│    (WebSocket / HTTP Push)                             │
│           ▼                                            │
│  [Local Server (localhost:3000)]                       │
│      ├── Browser Web Viewer (For You)                  │
│      │   • Pixel / Galaxy device bezels                │
│      │   • Light / Dark mode toggle                    │
│      │   • Font scaling & rotation                     │
│      │                                                 │
│      └── MCP Server (For AI Agents)                    │
│          • render_preview(name)                        │
│          • get_preview_image()                         │
└────────────────────────────────────────────────────────┘
```

---

## ✨ Features

- **No Android Studio Required**: Run lightweight editors like VS Code, Cursor, or Zed.
- **Sub-2-Second Turnaround**: Instant JVM execution with no APK packaging or emulator booting.
- **Realistic Web Device Frames**: Inspect your UI inside Google Pixel and Samsung Galaxy mockups.
- **Interactive Controls**: Toggle light/dark themes, test font scaling (0.85x to 1.5x), and rotate orientations on the fly.
- **Multi-Preview Grid**: View all defined `@Preview` variations simultaneously.
- **AI Agent Vision (MCP)**: Native Model Context Protocol support allows AI coding agents to visually verify Compose layouts.

---

## 📂 Repository Structure

```
compositor/
├── brain/                     # AI Agent persistent memory, ADRs & mistake catalog
│   ├── README.md              # Cognitive protocol for LLMs
│   ├── DECISIONS.md           # Architectural Decision Records (ADRs)
│   ├── MISTAKES_AND_CORRECTIONS.md # Traps, bugs & post-mortems
│   └── STATE.md               # Living snapshot of progress & priorities
├── docs/                      # Architectural specifications & standards
│   ├── ARCHITECTURE.md        # Subsystem deep dive & IPC protocols
│   ├── CODING_STANDARDS.md    # Language rules & quality gates
│   └── CONTRIBUTING.md        # Contributor guide
├── core-renderer/             # [Kotlin/JVM] Headless LayoutLib preview rasterizer
├── cli/                       # [Kotlin / Node] File watcher, build runner, daemon
├── web-viewer/                # [TypeScript / Vite / Tailwind] Browser canvas & device UI
├── mcp-server/                # [TypeScript / Node] Model Context Protocol vision bridge
├── samples/
│   └── sample-app/            # Baseline Compose app for end-to-end testing
├── AGENT.md                   # AI Assistant quality manifesto & navigation guide
├── LICENSE                    # Apache 2.0 License
└── README.md                  # This file
```

---

## 🚀 Quick Start & Wiki

For the complete, step-by-step user guide, configuration reference, and VS Code/MCP setup, see the **[Compositor Wiki](docs/WIKI.md)**.

### 1. Prerequisites
* **Java 21 JDK** (Android Gradle Plugin 8.x+ requirement)
* **Android SDK** (API 34 or 35 platform installed)

### 2. Verify in 60 Seconds
```bash
# Headless batch render to PNG
./gradlew :samples:sample-app:compositorRender

# Start interactive live daemon + Compose Multiplatform Web Viewer
./gradlew :samples:sample-app:compositor
```
The live Web Viewer will be available at **`http://localhost:3001`**.

---

## 🗺️ Roadmap & Current Status

- [x] **Phase 1: Native LayoutLib Engine & Ktor Daemon (100% Kotlin)**
  - [x] Unbundled LayoutLib host JVM rasterizer
  - [x] Embedded Kotlin PSI preview AST scanner
  - [x] Ktor Netty HTTP & WebSocket streaming daemon
- [x] **Phase 2: Production Web Viewer (Compose Multiplatform Wasm)**
  - [x] Hardware-accelerated Canvas with Pixel 8 and Galaxy S24 frames
  - [x] Instant Light/Dark theme toggle & font scale slider (0.85x to 1.5x)
  - [x] Element inspector bounds overlay & diagnostics
- [x] **Phase 3: Shippable Android Gradle Plugin (`id("io.compositor")`)**
  - [x] Standalone plugin with automated AGP classpath & resource resolution
  - [x] Tasks: `compositor`, `compositorRender`, `compositorMcp`
- [x] **Phase 4: AI Agent Vision Bridge (Model Context Protocol - MCP)**
  - [x] Official JetBrains Kotlin MCP SDK embedded in daemon & CLI
  - [x] 5 vision tools: `list_previews`, `render_preview`, `get_preview_image`, `inspect_layout_tree`, `compare_previews`
- [x] **Phase 5: IDE Extensions (VS Code & Cursor)**
  - [x] Sidebar webview panel with context-aware caret-to-preview resolution
  - [x] Automated workspace detection & daemon process management

---

## 📄 License

Compositor is open-source software licensed under the [Apache License, Version 2.0](LICENSE).

