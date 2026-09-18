# Step 05: Element Bounds & Semantic Inspector Overlay in Compose

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 04  
*Target Module*: `web-viewer`

---

## 1. Objective
Implement an interactive element inspector overlay using Compose `Canvas` / `Modifier.drawWithContent` that renders bounding boxes over UI elements when hovering or clicking in the web viewer, displaying component types, dimensions in dp, and semantic accessibility metadata.

---

## 2. Architectural Design & Responsibilities
* **Layout Hierarchy Consumer**:
  * Ingest the `hierarchy.json` endpoint from the Ktor daemon containing the tree of layout bounds (`[left, top, right, bottom]`), component types, and semantics.
* **Canvas Bounding Box Overlay**:
  * Implement an interactive overlay Composable positioned directly over the preview image.
  * Use `Modifier.pointerInput` to detect hover coordinates.
  * Draw translucent highlight rectangles around the hovered UI component using `drawRect` or `drawRoundRect`.
* **Element Inspector Drawer**:
  * Clicking an element locks the highlight and opens an Inspector Side Sheet.
  * Displays: Component Name (e.g. `androidx.compose.material3.Button`), Dimensions (`width x height` in dp), Padding, and Accessibility labels.
* **Inspector Toggle Mode**:
  * Dedicated toggle button (`Alt + I` or toolbar icon) to turn inspector mode on/off.

---

## 3. High-Level Architectural Guidance
* Accurately scale layout coordinates from device viewport pixels to the zoomed/panned canvas view.
* When inspector mode is deactivated, pointer events must pass through cleanly.

---

## 4. Verification & Quality Gates
* **Build Verification**: Run `./gradlew :web-viewer:wasmJsBrowserDistribution`; assert clean compilation and error-free execution in Wasm.
* **Quality Gate**: Code must pass `./gradlew detekt`.
