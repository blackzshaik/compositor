# Compositor Web Viewer (Compose Multiplatform Wasm - Experimental)

> [!NOTE]
> This module is the **experimental** Compose Multiplatform (Kotlin/Wasm + Skiko Canvas) web viewer.
> The primary, production-grade web interface is located in [`web-viewer-react/`](../web-viewer-react) (React 19 + TypeScript + Vite).

## Overview
An experimental WebAssembly browser client built using Compose Multiplatform targeting `wasmJs`. It connects to the Ktor preview daemon on port 3001 via REST and WebSockets (`/ws`).

## How to Run / Build
* **Standalone Dev Server**:
  ```bash
  ./gradlew :web-viewer:wasmJsBrowserDevelopmentRun
  ```
* **Package into Compositor Daemon**:
  ```bash
  ./gradlew compositor -Pcompositor.experimental.cmp=true
  # Or:
  ./gradlew compositor -Pcompositor.webViewer=cmp
  ```

