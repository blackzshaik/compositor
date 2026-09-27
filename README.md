# Compositor 🎨📱

[![CI](https://github.com/compositor-org/compositor/actions/workflows/ci.yml/badge.svg)](https://github.com/compositor-org/compositor/actions/workflows/ci.yml)
[![Release](https://img.shields.io/badge/Release-0.1.0--alpha01-blue.svg)](https://github.com/compositor-org/compositor/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-21-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6.svg?logo=typescript&logoColor=white)](https://www.typescriptlang.org)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](docs/CONTRIBUTING.md)

**Compositor** is an open-source, headless Jetpack Compose preview engine, live web viewer, and AI vision bridge.

It brings Android Studio's `@Preview` capability to **VS Code, Cursor, terminal environments, and AI coding assistants**—rendering native Compose UI directly on your host machine without launching Android Studio or booting an emulator.

---

## ⚡ The Problem

When developing native Android applications outside Android Studio (e.g. in Cursor, VS Code, or terminal) or when collaborating with AI coding agents:
1. **Developers are blind to UI changes**: Even adjusting a 4dp margin requires launching a heavy emulator, building a full APK, and deploying via `adb`—a 30 to 60-second cycle.
2. **AI assistants cannot "see" Compose code**: While web AI agents benefit from instant DOM feedback and headless browser screenshots, Android AI agents are completely blind to Jetpack Compose visual output.
3. **Android Studio is resource-heavy**: Keeping Android Studio open alongside modern AI tools consumes 8+ GB of RAM and drains battery life.

---

## 💡 The Solution

**Compositor** decouples the Compose preview engine from Android Studio:
* Executes `@Preview` composables on the host desktop JVM using Android's headless **LayoutLib** engine.
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
│  [Local Server (localhost:3001)]                       │
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

## 🚀 Quick Start

### 1. Prerequisites
* **Java 21 JDK** (AGP 8.x+ requirement)
* **Android SDK** (API 34 or 35 platform installed)

### 2. Add to Your Android Project
Add the Compositor plugin to your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") // or com.android.library
    id("org.jetbrains.kotlin.android")
    id("io.compositor") version "0.1.0-alpha01"
}

// Optional customization
compositor {
    port.set(3001)             // Default: 3001
    autoOpenBrowser.set(true)   // Auto-launch web viewer
    preferredTheme.set("system")// "light", "dark", or "system"
}
```

### 3. Run Live Previews
```bash
# Start live daemon + Web Viewer (http://localhost:3001)
./gradlew compositor

# Or batch render all previews to PNG in .compositor/previews/
./gradlew compositorRender
```

---

## 🔌 VS Code & Cursor Integration

1. Download the latest `compositor-preview-v0.1.0-alpha01.vsix` from [GitHub Releases](https://github.com/compositor-org/compositor/releases).
2. Install via command line or editor extensions view:
   ```bash
   code --install-extension compositor-preview-v0.1.0-alpha01.vsix
   # or in Cursor:
   cursor --install-extension compositor-preview-v0.1.0-alpha01.vsix
   ```
3. Open any `@Composable` file with `@Preview` annotations. The **Compositor Preview** panel will automatically open in the sidebar!

---

## 🤖 AI Agent Vision (Model Context Protocol - MCP)

Compositor embeds a full Model Context Protocol server, enabling AI assistants to autonomously inspect and fix Jetpack Compose layouts.

Add to your `mcpServers` configuration (e.g. Cursor, Claude Desktop, Antigravity):

```json
{
  "mcpServers": {
    "compositor": {
      "command": "./gradlew",
      "args": ["compositorMcp", "-q"]
    }
  }
}
```

Available MCP Vision Tools:
- `list_previews`: Discovers all `@Preview` composables across the project.
- `render_preview`: Triggers an on-demand render of a preview.
- `get_preview_image`: Retrieves rendered base64 PNG images for multi-modal LLM vision.
- `inspect_layout_tree`: Inspects element bounding boxes, padding, and layout bounds.
- `compare_previews`: Computes visual pixel-diff percentages between rendered frames.

---

## 📂 Repository Structure

```
compositor/
├── core-renderer/             # [Kotlin/JVM] Headless LayoutLib preview rasterizer & Ktor daemon
├── plugin/                    # [Gradle Plugin] Standalone 'io.compositor' plugin
├── web-viewer/                # [Kotlin/Wasm] Compose Multiplatform Web Viewer
├── web-viewer-react/          # [TypeScript/React] Ultra-fast native web viewer frontend
├── vscode-extension/          # [TypeScript] VS Code & Cursor sidebar extension
├── samples/sample-app/        # [Android/Compose] Reference sample application
├── docs/                      # Architectural specifications & documentation
│   ├── ARCHITECTURE.md        # Architecture deep dive & IPC protocols
│   ├── CODING_STANDARDS.md    # Code quality gates & conventions
│   ├── CONTRIBUTING.md        # Contributor guide
│   └── WIKI.md                # Comprehensive user guide
├── CODE_OF_CONDUCT.md         # Contributor Covenant Code of Conduct
├── SECURITY.md                # Vulnerability disclosure policy
├── LICENSE                    # Apache 2.0 License
└── README.md                  # This file
```

---

## 🤝 Contributing & Community

We welcome contributions! Please check out:
- [Contributing Guide](docs/CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Security Policy](SECURITY.md)

---

## 📄 License

Compositor is open-source software licensed under the [Apache License, Version 2.0](LICENSE).
