# Compositor Wiki & User Manual 📖🎨

Welcome to the **Compositor** Wiki! This guide walks you step-by-step through installing, running, and verifying Compositor on your local machine.

---

## 📑 Table of Contents

1. [Prerequisites & System Setup](#1-prerequisites--system-setup)
2. [Quick Verification in 60 Seconds (Zero Config)](#2-quick-verification-in-60-seconds-zero-config)
3. [Live Web Viewer & Hot Reload](#3-live-web-viewer--hot-reload)
4. [Using Compositor in Your Own Android Project](#4-using-compositor-in-your-own-android-project)
5. [VS Code & Cursor Extension Setup](#5-vs-code--cursor-extension-setup)
6. [AI Agent Vision Setup (Model Context Protocol - MCP)](#6-ai-agent-vision-setup-model-context-protocol---mcp)
7. [Command Reference & Configuration](#7-command-reference--configuration)
8. [Troubleshooting & FAQs](#8-troubleshooting--faqs)

---

## 1. Prerequisites & System Setup

Before getting started, ensure you have the following installed:

* **Java Development Kit (JDK) 21**:
  - Required for Android Gradle Plugin (AGP 8.x+) and LayoutLib bytecode compatibility.
  - Verify with: `java -version`
* **Android SDK**:
  - API level 34 or 35 platform installed (`platforms/android-34` or `platforms/android-35`).
  - Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` environment variable, or define `sdk.dir` in `local.properties`:
    ```properties
    # Windows example in local.properties:
    sdk.dir=C\:\\Users\\<your-user>\\AppData\\Local\\Android\\Sdk
    ```
* **Node.js 18+ (Optional)**:
  - *Note*: End users using Gradle need **zero Node.js or npm**. Node is only required if you are compiling or developing the `vscode-extension`.

---

## 2. Quick Verification in 60 Seconds (Zero Config)

The repository includes a ready-to-test benchmark application in `samples/sample-app`. You can verify headless rasterization immediately from your terminal.

### Step 1: Run Headless Batch Render
Run the `compositorRender` task on the sample project:

```bash
# On Windows PowerShell / Command Prompt:
.\gradlew.bat :samples:sample-app:compositorRender

# On macOS / Linux:
./gradlew :samples:sample-app:compositorRender
```

### Step 2: Verify the Output Image
Compositor parses the `@Preview` annotations using its embedded Kotlin PSI scanner, compiles the composables, and invokes LayoutLib headlessly on your desktop JVM.

Check the rendered PNG in:
```text
samples/sample-app/.compositor/previews/app_com_compositor_sample_GreetingPreview.png
```

Open this image in any photo viewer to confirm that the dark blue banner reading *"Welcome to the Future of Android, Blackz !"* rendered cleanly at high resolution.

---

## 3. Live Web Viewer & Hot Reload

Compositor includes a local Ktor preview daemon and a hardware-accelerated **Compose Multiplatform (Kotlin/Wasm)** web viewer.

### Step 1: Start the Interactive Daemon

Run:
```bash
.\gradlew.bat :samples:sample-app:compositor
```

This launches the Ktor server on port **3001** and hosts the Web Viewer.

> [!TIP]
> If you have `autoOpenBrowser.set(true)` in your Gradle build, your default browser opens automatically. Otherwise, navigate to **`http://localhost:3001`**.

### Step 2: Explore the Web Viewer Features
Inside the browser canvas, you will see:
* **Hardware Device Mockups**: View your UI inside realistic Google Pixel 8 and Samsung Galaxy bezels.
* **Theme Toggle**: Switch between **Light Theme** and **Dark Theme** instantly.
* **Font Scaling Slider**: Drag from `0.85x` to `1.50x` to verify accessibility and typography wrapping without touching code.
* **Device Rotation**: Toggle between **Portrait** and **Landscape** modes.
* **Multi-Preview Matrix**: If multiple `@Preview` annotations are defined, view them side-by-side in a responsive grid.
* **Element Inspector**: Toggle overlay bounding boxes to inspect exact `dp` coordinates and margins.

### Step 3: Test Hot Reload
1. Keep the browser window open at `http://localhost:3001`.
2. Open `samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt` in your code editor.
3. Change the background color or greeting text:
   ```kotlin
   Text(
       text = "Compositor Live Reload Works!",
       color = Color.Yellow,
       fontSize = 22.sp
   )
   ```
4. Save the file (`Ctrl+S` / `Cmd+S`).
5. **Watch the Web Viewer**: The file watcher debounces the event, triggers an in-memory re-rasterization, and pushes the new bitmap over WebSocket in **< 1.5 seconds**!

---

## 4. Using Compositor in Your Own Android Project

To add Compositor to an existing external Android project:

### Step 1: Publish the Plugin Locally
From the `compositor` repository root, publish the plugin to your machine's `~/.m2/repository` (`mavenLocal`):

```bash
.\gradlew.bat :core-renderer:publishToMavenLocal :plugin:publishToMavenLocal
```

### Step 2: Configure Your Project Repositories
In your external project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
```

### Step 3: Apply the Plugin
In your application or library module's `build.gradle.kts` (e.g., `app/build.gradle.kts`):

```kotlin
plugins {
    alias(libs.plugins.android.application) // or id("com.android.application")
    alias(libs.plugins.kotlin.android)     // or id("org.jetbrains.kotlin.android")
    id("io.compositor") version "0.1.0-SNAPSHOT"
}

// Optional configuration block:
compositor {
    port.set(3001)                // Daemon HTTP/WS port (default: 3001)
    autoOpenBrowser.set(true)     // Automatically launch browser on startup
    preferredTheme.set("dark")    // "light", "dark", or "system"
    variantName.set("debug")      // Build variant to inspect (default: "debug")
}
```

Now you can run `./gradlew compositor` or `./gradlew compositorRender` directly in your project!

---

## 5. VS Code & Cursor Extension Setup

For developers using **VS Code** or **Cursor**, Compositor provides a dedicated extension with an embedded sidebar webview and cursor-to-preview resolution.

### Step 1: Build the Extension Package
In the `vscode-extension` directory:

```bash
cd vscode-extension
npm install
npm run build
```

To bundle a `.vsix` file:
```bash
npx @vscode/vsce package --no-dependencies
```
This generates `compositor-vscode-0.1.0.vsix`.

### Step 2: Install into VS Code / Cursor
Install via the CLI:
```bash
# In VS Code:
code --install-extension compositor-vscode-0.1.0.vsix

# In Cursor:
cursor --install-extension compositor-vscode-0.1.0.vsix
```
*Or in the UI*: Open the Extensions panel (`Ctrl+Shift+X`), click the **`...`** (More Actions) menu at the top, select **Install from VSIX...**, and choose the `.vsix` file.

### Step 3: Using the Extension
1. Open any Android workspace containing `@Preview` composables.
2. Click the **Compositor Palette icon** in the primary Activity Bar to reveal the live preview panel.
3. Open any Kotlin file (e.g., `Greeting.kt`). Place your cursor inside a `@Composable @Preview` function.
4. Open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) and choose:
   - **`Compositor: Open Preview`**: Focuses the preview panel.
   - **`Compositor: Re-render Composable at Cursor`** (`Ctrl+Alt+P` / `Cmd+Alt+P`): Instantly forces a re-render of the specific composable under your caret.
   - **`Compositor: Start Daemon`** / **`Compositor: Stop Daemon`**: Manage the background Gradle process.

---

## 6. AI Agent Vision Setup (Model Context Protocol - MCP)

Compositor includes a built-in **Model Context Protocol (MCP)** server built with the official JetBrains Kotlin MCP SDK. This allows AI assistants (Antigravity, Cursor, Claude Code) to inspect UI previews, layout bounds, and visual regression diffs.

### Option A: Standard I/O (Stdio) Transport
Recommended for local coding agents and CLI runners.

Run the MCP server via Gradle:
```bash
.\gradlew.bat compositorMcp
```

#### Configuration for Claude Desktop / Cursor:
Add to your `claude_desktop_config.json` or Cursor MCP settings:

```json
{
  "mcpServers": {
    "compositor": {
      "command": "cmd.exe",
      "args": [
        "/c",
        "gradlew.bat",
        ":samples:sample-app:compositorMcp",
        "-q"
      ],
      "cwd": "C:/path/to/compositor"
    }
  }
}
```

### Option B: Server-Sent Events (SSE) Transport
If the Compositor daemon is already running via `./gradlew compositor`:
* **SSE Endpoint**: `http://localhost:3001/sse`
* **Message POST Endpoint**: `http://localhost:3001/mcp`

### Available AI Tools Reference:

| Tool Name | Parameters | Description |
| :--- | :--- | :--- |
| `list_previews` | *none* | Returns a JSON catalog of all discovered `@Preview` composables in the workspace. |
| `render_preview` | `name` (string) | Triggers on-demand LayoutLib rasterization for a specific preview function. |
| `get_preview_image` | `name` (string) | Returns the Base64-encoded PNG image of the rasterized preview for visual verification. |
| `inspect_layout_tree` | `name` (string) | Returns semantic bounding boxes, element tags, and `dp` coordinates for layout debugging. |
| `compare_previews` | `name`, `threshold` | Performs visual regression diffing against `.compositor/baselines/` and returns pixel delta. |

---

## 7. Command Reference & Configuration

| Gradle Command | Function |
| :--- | :--- |
| `./gradlew compositor` | Launches Ktor daemon, starts file watcher, and opens Web Viewer. |
| `./gradlew compositorRender` | Headless batch export of all `@Preview` composables to PNG. |
| `./gradlew compositorMcp` | Starts Stdio MCP server for AI coding agents. |
| `./gradlew test` | Executes the complete test suite across all subprojects. |

### Extension Configuration DSL (`build.gradle.kts`):

```kotlin
compositor {
    // Port for the Ktor HTTP & WebSocket server (default: 3001)
    port.set(3001)

    // Whether to automatically launch the browser when running ./gradlew compositor (default: false)
    autoOpenBrowser.set(true)

    // Default theme for the Web Viewer: "light", "dark", or "system" (default: "system")
    preferredTheme.set("dark")

    // The Android build variant to analyze and resolve classpaths for (default: "debug")
    variantName.set("debug")
}
```

---

## 8. Troubleshooting & FAQs

### Q: "Cannot find System Image / SDK platform"
**Solution**: Ensure you have an Android platform installed in your SDK directory (e.g. `platforms/android-34`). Also verify your `local.properties` contains `sdk.dir=...`.

### Q: "Port 3001 is already in use"
**Solution**: If another process is using port 3001, configure a custom port in your `build.gradle.kts`:
```kotlin
compositor {
    port.set(3050)
}
```
Or kill the existing process occupying port 3001.

### Q: "Dynamic agent loading warning on JDK 21"
**Notice**: You may see `WARNING: A Java agent has been loaded dynamically (byte-buddy-agent)`. This is standard OpenJDK 21 diagnostic behavior for runtime bytecode modification and does **not** affect rendering or cause errors.

### Q: "Where are baseline images stored for visual diffs?"
**Answer**: Baseline screenshots are saved in `.compositor/baselines/` within your module directory. You can commit these to Git for team visual regression tracking.
