# Step 04: Visual Regression & Layout Diffing Tool

*Phase*: 3 — AI Agent Vision Bridge  
*Status*: Blocked by Step 03  
*Target Module*: `mcp-server`

---

## 1. Objective
Build an automated visual regression and diffing tool (`compare_previews`) allowing AI coding agents to verify code edits against baseline states, measuring exact pixel differences and identifying unintended layout shifts.

---

## 2. Tool Specifications

### Tool: `compare_previews`
* **Description**: Compares the current render of a composable against its previous baseline snapshot (or compares two named previews), generating a pixel-diff percentage and a visual diff overlay.
* **Input Schema**:
  * `previewId` (string, required): The target preview identifier.
  * `baselineTimestamp` (number, optional): Compare against a specific historical snapshot.
* **Return Payload**:
  * `hasVisualDifferences`: Boolean flag.
  * `differencePercentage`: Float (e.g. `4.2%`).
  * `diffImage`: Base64 PNG highlighting visual differences in magenta/red overlay.
  * `summary`: Descriptive explanation of the visual impact.

---

## 3. High-Level Architectural Guidance
* Utilize a fast in-memory image diff library (e.g. `pixelmatch` or canvas-based pixel comparison).
* Maintain a local cache of the previous render frame under `.compositor/baselines/` when a session begins or git commit changes.
* This tool enables autonomous closed-loop UI refactoring: an AI can adjust padding, call `compare_previews`, verify that only the intended element shifted, and conclude the edit successfully.

---

## 4. Verification & Quality Gates
* **Automated Unit Tests**: Provide two test fixture images (identical vs 5% modified); assert that `compare_previews` reports 0% difference on identical images and accurate percentage on modified images.
