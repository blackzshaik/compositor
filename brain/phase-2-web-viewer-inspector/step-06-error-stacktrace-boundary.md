# Step 06: Error Diagnostics & Resilient Error Boundary

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Complete ✅  
*Target Module*: `web-viewer`

---

## 1. Objective
Build an intuitive diagnostic overlay and React Error Boundary that displays compilation errors, runtime exceptions, and missing resource alerts directly over the device canvas with source code file links and actionable fixes.

---

## 2. Functional Requirements
* **Live Error State Catching**: Listen for `RENDER_ERROR` WebSocket events from the daemon and render an inline error card over the active preview frame.
* **Formatted Diagnostics**:
  * Highlight error type (e.g. `Unresolved reference`, `MissingResourceException`, `NoSuchMethodError`).
  * Display the specific source file and line number where the failure occurred.
  * Provide a code snippet preview around the offending line.
* **Auto-Dismiss on Fix**: When the developer resolves the issue in their editor and the daemon re-renders successfully, clear the error overlay automatically.
* **React Error Boundary**: Prevent frontend runtime crashes from unmounting the sidebar or toolbar.

---

## 3. High-Level Architectural Guidance
* Keep the last known valid preview image visible with a dimmed backdrop under the error card, so the developer never loses context.
* Include a "Copy Error Diagnostic" button to facilitate quick pasting into AI assistant prompts or bug reports.
* Format stack traces cleanly, collapsing internal framework frames (e.g. JVM reflection / LayoutLib internals) while highlighting user application code.

---

## 4. Verification & Quality Gates
* **Component Error Tests**: Dispatch a mock `RENDER_ERROR` event and verify the error card displays the correct line number, message, and action buttons without unmounting the main dashboard.
