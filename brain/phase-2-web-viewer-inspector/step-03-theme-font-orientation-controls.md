# Step 03: Theme, Typography & Orientation Controls in Compose

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 02  
*Target Module*: `web-viewer`

---

## 1. Objective
Build an interactive top toolbar in Compose Material 3 (`TopAppBar`) allowing developers to toggle light/dark theme modes, test typography font scaling (0.85x to 1.5x) via a dynamic slider, flip screen orientation, and toggle live WebSocket connection status.

---

## 2. Architectural Design & Responsibilities
* **Top App Bar Composable**:
  * Implement `@Composable fun InspectorTopBar(...)` using Material 3 `TopAppBar`.
  * Display project title, connected daemon status badge, and quick action controls.
* **Theme Switching**:
  * Material 3 `Switch` or `IconToggleButton` to switch between Light Mode and Dark Mode.
  * Either selects a matching `@Preview(uiMode = ...)` variant from the catalog or triggers an on-demand re-render via daemon API with the dark theme override flag.
* **Typography Font Scale Slider**:
  * Material 3 `Slider` with discrete snap steps: 0.85x (Small), 1.0x (Normal), 1.15x (Large), 1.3x (Extra Large), 1.5x (Huge).
  * Immediately displays active font scale and dispatches re-render requests to verify text wrapping.
* **Orientation Toggle**:
  * `IconButton` flipping device orientation between Portrait and Landscape with animated rotation transitions.
* **Live WebSocket Sync Status**:
  * Pulsing status dot indicating real-time WebSocket connection to the Ktor daemon (`Live Sync` vs `Disconnected / Retrying`).

---

## 3. High-Level Integration Guidance
* Hoist inspector configuration state into a clean data class: `data class InspectorConfig(val isDarkTheme: Boolean, val fontScale: Float, val isLandscape: Boolean)`.
* Provide a "Refresh" button that forces an immediate re-fetch of the preview frame.

---

## 4. Verification & Quality Gates
* **Build Verification**: Run `./gradlew :web-viewer:wasmJsBrowserDistribution`; assert clean compilation and error-free execution in Wasm.
* **Quality Gate**: Code must pass `./gradlew detekt`.
