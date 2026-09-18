# Step 04: Multi-Preview Matrix Grid in Compose Multiplatform

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 03  
*Target Module*: `web-viewer`

---

## 1. Objective
Introduce a "Matrix / Grid View" mode implemented using Compose `LazyVerticalGrid` that displays multiple variations of a composable side-by-side (such as Light vs. Dark theme or multiple typography scales simultaneously).

---

## 2. Architectural Design & Responsibilities
* **View Mode Selector**:
  * Segmented button in the toolbar to toggle between **Single Device View** and **Matrix Grid View**.
* **Compose `LazyVerticalGrid` Canvas**:
  * Implement `@Composable fun PreviewMatrixGrid(...)` using adaptive columns (`GridCells.Adaptive(minSize = 320.dp)`).
  * Render each preview card inside an elevated Material 3 `Card` with a clear title header and variant tag (e.g. "Dark Mode • 1.0x").
* **Matrix Presets**:
  * *Theme Matrix*: Displays Light mode and Dark mode variants side-by-side.
  * *Font Scale Matrix*: Displays 4 preview cards comparing 0.85x, 1.0x, 1.25x, and 1.5x typography scales.
  * *Group Matrix*: Renders all previews belonging to the same `@Preview(group = "...")` tag.
* **Unified Zoom**:
  * Zoom and pan gestures scale the entire grid uniformly, allowing instant visual alignment comparisons.

---

## 3. High-Level Integration Guidance
* Use lazy composition so grid cards outside the visible viewport do not consume unnecessary memory.
* Clicking any card in the matrix switches the view into Single Device mode focused on that specific preview.

---

## 4. Verification & Quality Gates
* **Build Verification**: Run `./gradlew :web-viewer:wasmJsBrowserDistribution`; assert clean compilation and error-free execution in Wasm.
* **Quality Gate**: Code must pass `./gradlew detekt`.
