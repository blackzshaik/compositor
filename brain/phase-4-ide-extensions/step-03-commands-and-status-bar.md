# Step 03: Command Palette & Status Bar Integration

*Phase*: 4 — IDE Extensions & Plugin Ecosystem  
*Status*: Blocked by Step 02  
*Target Module*: `vscode-extension`

---

## 1. Objective
Add rich developer ergonomics by registering interactive commands in the Command Palette and placing a real-time status indicator in the bottom status bar.

---

## 2. Functional Requirements
* **Command Palette Actions**:
  * `Compositor: Open Preview to the Side` — Opens the active file's preview in an adjacent editor column.
  * `Compositor: Re-render Active Preview` — Triggers an immediate targeted re-render of the composable under the editor cursor.
  * `Compositor: Restart Daemon` — Restarts the background file watcher and preview server process.
  * `Compositor: Toggle Theme (Light/Dark)` — Toggles preview theme without touching source code.
* **Status Bar Item**:
  * Displays `[🎨 Compositor: Live (<count> Previews)]` when daemon is connected.
  * Displays `[🎨 Compositor: Offline (Click to Start)]` when daemon is stopped.
  * Click triggers quick action menu.

---

## 3. High-Level Architectural Guidance
* Detect the active text editor cursor position: when the user triggers `Re-render Active Preview`, resolve the enclosing `@Preview` function name under the cursor automatically.
* Keep status bar polling minimal: rely on WebSocket status pushes from the daemon rather than periodic polling.

---

## 4. Verification & Quality Gates
* **Unit Tests**: Verify command registrations and test cursor position resolution to matching composable names.
