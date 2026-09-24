# Compositor Installation & User Wiki

Welcome to the **Compositor** Installation and User Guide. This document provides complete instructions for setting up your environment, running the Compositor preview daemon, using the interactive studio interface, and experimenting with optional modules.

---

## 1. System Requirements & Prerequisites

Before running Compositor, make sure your development environment has the following tools installed and available on your system path:

| Requirement | Recommended Version | Environment Variable / Path | Purpose |
| :--- | :--- | :--- | :--- |
| **Java JDK** | **JDK 17** or **JDK 21** | `JAVA_HOME` pointing to JDK directory | Compiles Kotlin and executes Gradle tasks |
| **Android SDK** | **API 34** or **35** | `ANDROID_HOME` or `ANDROID_SDK_ROOT` | Provides Android framework classes (`android.jar`) and LayoutLib rendering engine |
| **Node.js** | **v18+** / **v20+** / **v22+** | Available on system `PATH` | Compiles the web viewer production bundle |

### Verifying Environment Variables

#### Windows (PowerShell)
```powershell
# Verify Java
java -version
$env:JAVA_HOME

# Verify Android SDK
$env:ANDROID_HOME

# Verify Node.js
node --version
npm --version
```

#### macOS / Linux (Bash or Zsh)
```bash
# Verify Java
java -version
echo $JAVA_HOME

# Verify Android SDK
echo $ANDROID_HOME

# Verify Node.js
node --version
npm --version
```

> [!NOTE]
> If `ANDROID_HOME` is not explicitly exported, Compositor automatically falls back to standard SDK paths:
> * Windows: `%LOCALAPPDATA%\Android\Sdk`
> * macOS: `~/Library/Android/sdk`
> * Linux: `~/Android/Sdk`

---

## 2. Quickstart: Launching the Studio

The **React 19 + TypeScript + Vite** web interface is the default native viewer. It compiles incrementally in ~2.6 seconds (0.5s when cached) and produces a lightweight, responsive bundle (~338 KB).

### Launching the Preview Daemon

Execute the `:compositor` Gradle task against your target Android module:

```bash
# Windows
.\gradlew :samples:sample-app:compositor

# macOS / Linux
./gradlew :samples:sample-app:compositor
```

### What Happens Automatically:
1. **Preview Discovery**: Compositor scans the target module's Kotlin sources for `@Preview` annotations.
2. **Incremental Web Packaging**: Gradle packages the web viewer assets into the daemon jar.
3. **LayoutLib Engine Initialization**: Android LayoutLib boots headless in a separate worker process.
4. **Daemon Launch**: The Ktor daemon starts on port **`3001`**.
5. **Browser Auto-Open**: Your default web browser automatically navigates to **`http://localhost:3001`**.

Terminal output should resemble:
```
> Task :samples:sample-app:compositor
Compositor: Discovered 2 preview(s).
Compositor daemon running at: http://localhost:3001
Compositor: Press Ctrl+C in this terminal to stop.
```

---

## 3. Studio Interface & Features Guide

When navigating to `http://localhost:3001`, you have access to the complete preview suite:

### 📱 Single Device View
* **Device Profiles**: Select hardware profiles from the device dropdown:
  * Pixel 8
  * Galaxy S24 Ultra
  * Foldable (expanded screen)
  * 10" Tablet
  * Frameless (shows raw composable boundaries without bezels)
* **Orientation Toggle**: Rotate the frame between Portrait and Landscape with one click.
* **Theme Switching**: Toggle instant preview between Light Mode (`UI_MODE_NIGHT_NO`) and Dark Mode (`UI_MODE_NIGHT_YES`).
* **Zoom & Pan**: Use zoom controls (`+`, `-`, fit-to-screen) or drag the viewport canvas.
* **Element Inspector**: Toggle the crosshair icon to overlay component bounding boxes and view dimensions, qualified class names, and hierarchy metadata.

### 🗂️ Multi-Preview Matrix View
Click the **Matrix** toggle button on the top navigation bar. Matrix View renders variants side-by-side in a responsive CSS Grid:
* **Light vs Dark**: Compares daytime and nighttime themes side-by-side.
* **Font Scales**: Displays 4 font scales (`1.0x`, `1.15x`, `1.3x`, `1.5x`) to verify dynamic typography and accessibility.
* **Group Previews**: Shows all previews sharing the same `@Preview(group = "...")` parameter.
* **All Previews**: Renders every composable preview discovered in the module.
* **Matrix Card Actions**:
  * **Copy Image URL**: Copies the high-resolution PNG asset link to clipboard.
  * **Re-render**: Forces LayoutLib to immediately re-rasterize the variant.
  * **Focus**: Clicks into Single Device view centered on that variant.

### ⚡ Live Hot-Reloading Workflow
1. Keep the `./gradlew compositor` process running in your terminal.
2. Open any Composable file in your IDE (e.g. `Greeting.kt`).
3. Make an edit and save the file.
4. Compositor detects file modifications, re-executes LayoutLib rendering, and broadcasts live updates over WebSockets (`/ws`) without requiring a manual browser refresh.

---

## 4. Running the Experimental Compose Multiplatform (CMP) UI

Compositor also includes an experimental Compose Multiplatform (Kotlin/Wasm + Skiko Canvas) web viewer under `web-viewer/`. By default, this is deactivated to prevent ~3-minute Wasm compilation delays.

### Option A: Package CMP into the Compositor Daemon
To build and serve the experimental CMP frontend inside the daemon:

```bash
./gradlew :samples:sample-app:compositor -Pcompositor.experimental.cmp=true
```
*(Or `./gradlew :samples:sample-app:compositor -Pcompositor.webViewer=cmp`)*

* **Behavior**: Builds `:web-viewer:wasmJsBrowserDistribution` and serves the Skiko Canvas WebAssembly client at `http://localhost:3001`.
* **Note**: Initial clean Wasm compilation takes ~3 minutes due to Binaryen optimization and Kotlin Wasm webpack compilation.

### Option B: Run Standalone CMP Dev Server
To work directly on the CMP Kotlin source code with its own development server:

```bash
./gradlew :web-viewer:wasmJsBrowserDevelopmentRun
```
* **Behavior**: Starts the CMP development server at `http://localhost:8080`, communicating with the Ktor preview daemon on port 3001.

---

## 5. VS Code Extension Integration

Compositor provides a dedicated VS Code extension (`vscode-extension/`):

1. Open the project repository in **Visual Studio Code**.
2. Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on macOS) to open the Command Palette.
3. Type and run:
   ```
   Compositor: Start Daemon
   ```
4. Run:
   ```
   Compositor: Open Preview Studio
   ```
   *(Or click the Compositor icon in the Activity Bar sidebar).*
5. The React studio interface opens directly inside an integrated VS Code webview panel with full WebSocket hot-reload support.

---

## 6. Architecture & Subproject Reference

| Subproject | Language / Stack | Role |
| :--- | :--- | :--- |
| **`core-renderer`** | Kotlin (JVM) | Headless LayoutLib rendering engine, Ktor daemon, WebSocket events, and MCP server |
| **`web-viewer-react`** | React 19, TypeScript, Tailwind CSS, Vite | **Primary** native studio web interface, Matrix view, Element Inspector |
| **`web-viewer`** | Kotlin/Wasm, Compose Multiplatform | **Experimental** WebAssembly browser client |
| **`plugin`** | Kotlin (Gradle API) | Gradle plugin registering `:compositor` and `:compositorRender` tasks |
| **`vscode-extension`** | TypeScript, VS Code API, Vite | Visual Studio Code panel and sidebar provider |
| **`samples/sample-app`** | Kotlin, Jetpack Compose, Android | Reference Android application containing preview examples |

---

## 7. Troubleshooting & FAQ

### Q: Port 3001 is already in use
**A**: If port 3001 is occupied, specify an alternative port via system property or configuration:
```bash
./gradlew :samples:sample-app:compositor -Dcompositor.port=3005
```

### Q: `npm run build` fails during Gradle build
**A**: Ensure Node.js (v18+) is installed and accessible from terminal. Run `cd web-viewer-react && npm install` to ensure all dependencies are resolved.

### Q: LayoutLib fails to locate `android.jar`
**A**: Verify that `ANDROID_HOME` is set to your Android SDK location, and that platform `android-34` or `android-35` is installed via Android Studio SDK Manager.
