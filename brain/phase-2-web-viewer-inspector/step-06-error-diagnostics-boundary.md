# Step 06: Error Diagnostics & Resilient Error Card in Compose

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 05  
*Target Module*: `web-viewer`

---

## 1. Objective
Build an intuitive diagnostic overlay using Compose Material 3 (`Card`, `AnimatedVisibility`) that displays compilation errors, runtime exceptions, and missing resource alerts over the device canvas with source code links, line numbers, and auto-dismissal when the developer saves a fix.

---

## 2. Architectural Design & Responsibilities
* **WebSocket Error Listener**:
  * Listen for `RENDER_ERROR` events pushed from the Ktor daemon over the WebSocket connection.
  * Parse error payloads containing exception messages, stack traces, source file paths, and offending line numbers.
* **Diagnostic Card Composable**:
  * Implement `@Composable fun ErrorDiagnosticCard(...)` with `AnimatedVisibility`.
  * Highlight the error type (e.g. `Unresolved reference`, `MissingResourceException`, `CompilationError`).
  * Display the exact file name and line number in an elevated, readable card over the canvas.
  * Format stack traces cleanly, collapsing internal framework frames.
* **Auto-Dismiss on Successful Re-render**:
  * When a subsequent file save resolves the error and the daemon emits `PREVIEW_UPDATED`, automatically animate the error card out of view.
* **Copy Diagnostic Details**:
  * Provide a "Copy Details" button to copy the error diagnostic into the clipboard for instant pasting into AI assistant chats.

---

## 3. High-Level Architectural Guidance
* Keep the last valid preview image visible behind a dimmed translucent backdrop so the developer doesn't lose context while debugging.
* Ensure UI crashes or invalid data never unmount the top toolbar or navigation drawer.

---

## 4. Verification & Quality Gates
* **Build Verification**: Run `./gradlew :web-viewer:wasmJsBrowserDistribution`; assert clean compilation and error-free execution in Wasm.
* **Quality Gate**: Code must pass `./gradlew detekt`.
