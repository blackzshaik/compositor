# Step 04: Visual Regression & Layout Diffing Tool

*Phase*: 4 — AI Agent Vision Bridge  
*Status*: Complete ✅  
*Target Module*: `:core-renderer`

---

## 1. Objective
Build an automated visual regression and diffing tool (`compare_previews`) using a pure-Kotlin `VisualDiffEngine` and `BaselineManager`, allowing AI coding agents to verify code edits against baseline states, measuring exact pixel differences, identifying layout shifts, and generating magenta-highlighted diff overlays.

---

## 2. Tool Specifications

### Tool: `compare_previews`
* **Description**: Compares the current render of a composable against its previous baseline snapshot (or compares two named previews), generating a pixel-diff percentage and a visual diff overlay.
* **Input Schema**:
  * `previewId` (string, required): The target preview identifier.
  * `baselineId` (string, optional): An explicit baseline snapshot identifier.
  * `tolerance` (number, optional: `0.0` to `1.0`, default `0.1`): Color distance sensitivity threshold.
* **Return Payload**:
  * `hasVisualDifferences`: Boolean flag.
  * `differencePercentage`: Float (e.g. `4.2%`).
  * `diffImageBase64`: Base64 PNG highlighting visual differences in magenta overlay (`0xFFFF007F`).
  * `summary`: Descriptive explanation of the visual impact and total pixel metrics.

---

## 3. High-Level Architectural Guidance
* Pure Kotlin `VisualDiffEngine` implements Euclidean RGB color distance:
  $$\Delta C = \sqrt{(\Delta R)^2 + (\Delta G)^2 + (\Delta B)^2} / \sqrt{255^2 \times 3}$$
* Handles mismatched dimensions gracefully by taking the maximum width and height and padding transparently.
* `BaselineManager` manages baseline snapshots under `.compositor/baselines/` with sanitized IDs and ISO-8601 timestamps.

---

## 4. Verification & Quality Gates
* **Unit Tests**:
  * `VisualDiffEngineTest`: Validates identical images (0%), modified images (>0%), dimension padding, and byte-array conversions.
  * `BaselineManagerTest`: Validates ID sanitization, snapshot persistence, and retrieval.
* **Integration Tests**: `CompositorMcpServerTest` validates end-to-end `compare_previews` execution.
