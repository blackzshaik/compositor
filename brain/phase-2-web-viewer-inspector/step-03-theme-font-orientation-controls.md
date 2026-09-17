# Step 03: Theme, Typography & Orientation Controls

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 02  
*Target Module*: `web-viewer`

---

## 1. Objective
Add an interactive top inspector toolbar enabling developers to toggle Android UI configurations in real-time: Light/Dark mode, Typography font scaling (0.85x to 1.5x), and Screen Orientation (Portrait vs. Landscape).

---

## 2. Functional Requirements
* **Theme Toggle**:
  * Quick switch between Light Mode and Dark Mode (`uiMode = Configuration.UI_MODE_NIGHT_YES / NO`).
  * If the preview already defines a dark mode variant, switch instantly to it; otherwise request on-demand render with dark theme flag.
* **Font Scaling Slider**:
  * Continuous slider or stepped buttons (0.85x Small, 1.0x Normal, 1.15x Large, 1.3x Extra Large, 1.5x Huge).
  * Used to catch typography clipping and layout breakage.
* **Orientation Toggle**:
  * Flip between Portrait and Landscape viewports, updating device bezel rotation smoothly with CSS transitions.
* **Background Color Picker**:
  * Toggle light/dark canvas backdrop or transparent checkerboard to inspect transparent composables.

---

## 3. High-Level Architectural Guidance
* Model these configuration settings in a central `PreviewConfigContext`.
* When an active control changes, either select the matching variant from the preview catalog or dispatch a `POST /api/previews/:id/render` call with requested parameter overrides.
* Persist active preferences across session reloads.

---

## 4. Verification & Quality Gates
* **Interactive Unit Tests**: Verify slider changes trigger state updates and theme toggling alters the image source URL or variant selector accurately.
