# Step 05: Element Bounds & Semantic Inspector Overlay

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 04  
*Target Module*: `web-viewer`

---

## 1. Objective
Implement an interactive element inspector overlay on the browser canvas that highlights composable boundaries, margins/paddings, and semantic node information when hovering or clicking on the rendered preview.

---

## 2. Functional Requirements
* **Layout Hierarchy Ingestion**: Consume layout bounding box data (`hierarchy.json` containing `[left, top, right, bottom]`, element names, and semantics) served alongside preview bitmaps.
* **Hover Highlights**: Display translucent bounding box rectangles over hovered composable elements (e.g. Buttons, Text, Columns).
* **Selection Details Panel**:
  * Clicking an element pins the highlight and opens an Inspector Drawer.
  * Displays: Element Type (e.g. `androidx.compose.material3.Text`), Dimensions (`width x height` in dp), Padding, and accessibility semantics.
* **Toggle Inspector Mode**: Easily enable/disable the inspector via a dedicated toggle button or keyboard shortcut (`Alt + I`).

---

## 3. High-Level Architectural Guidance
* Render the bounding boxes as an SVG or HTML5 Canvas layer positioned precisely over the rendered bitmap image.
* Calculate coordinate scaling dynamically so bounding boxes remain aligned across all zoom levels and device bezel sizes.
* Ensure pointer events pass through cleanly when inspector mode is deactivated.

---

## 4. Verification & Quality Gates
* **Unit & Interaction Tests**: Simulate hover events on scaled coordinates and verify highlight rectangle matches expected bounding box coordinates.
