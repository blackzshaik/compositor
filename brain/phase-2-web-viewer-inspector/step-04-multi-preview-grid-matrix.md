# Step 04: Multi-Preview Matrix & Side-by-Side Comparison

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 03  
*Target Module*: `web-viewer`

---

## 1. Objective
Introduce a "Matrix / Grid View" mode allowing developers to visualize and compare multiple preview configurations side-by-side on a unified canvas (e.g. Light vs. Dark mode simultaneously, or all font scales at once).

---

## 2. Functional Requirements
* **View Modes**:
  * **Single Device View**: Focus on a single preview within a full device bezel.
  * **Matrix View**: Responsive grid displaying all variations of a composable simultaneously.
* **Matrix Presets**:
  * *Theme Matrix*: Light mode card beside Dark mode card.
  * *Font Scale Matrix*: 4-panel layout displaying normal, large, and extra-large typography side-by-side.
  * *Multi-Preview Group*: Renders all previews belonging to the same `@Preview(group = "...")` in a flow layout.
* **Synchronized Zoom**: Zooming or panning in matrix mode scales all frames uniformly for direct visual comparison.

---

## 3. High-Level Architectural Guidance
* Render each matrix cell with a compact header label (e.g. "Dark Mode • 1.0x", "Light Mode • 1.3x") and action buttons (focus, re-render, copy image).
* Optimize rendering so off-screen grid cells lazy-load their image bitmaps.
* Ensure layout reflows gracefully on ultrawide monitors and standard laptop screens.

---

## 4. Verification & Quality Gates
* **Component Tests**: Test switching to matrix view, verifying correct count of grid cells rendered and responsive layout wrapping.
