# Compositor: Future Roadmap & Advanced Capabilities

This document catalogs advanced improvements, experimental features, and long-term vision items for **Compositor**. These items will be revisited once the core headless engine, pure-Kotlin daemon, and Gradle plugin are fully stabilized.

---

## 1. Interactive State & Gesture Simulation (Interactive Mode)
* **Concept**: Transform static screenshot previews into live interactive components within the browser canvas.
* **Mechanism**:
  * Capture mouse clicks, drags, and keyboard inputs from the browser canvas.
  * Stream pointer and key events over WebSockets to the host LayoutLib Compose runtime session.
  * Re-compose and stream back updated frames using dirty-rect or VP9/WebP streaming.
* **Benefit**: Developers can test counter increments, dropdown expansions, bottom-sheet gestures, and `TextField` input states without an emulator.

---

## 2. Advanced Multipreview & `@PreviewParameter` Matrix
* **Concept**: Full automatic expansion of custom multipreview annotations and data providers.
* **Mechanism**:
  * Scan and resolve custom meta-annotations (e.g. `@FontScalePreviews`, `@DevicePreviews`).
  * Instantiate `PreviewParameterProvider<T>` via reflection on the host JVM classloader and generate separate preview cards for each yielded sample value.
* **Benefit**: Instant visual matrix testing of edge-case strings (empty, long text, RTL Arabic/Hebrew) and device densities.

---

## 3. Figma-to-Compose Live Visual Diffing
* **Concept**: Overlay design team's Figma components directly on top of the live Compose render.
* **Mechanism**:
  * Connect to Figma REST API using a component node ID.
  * Fetch high-res design frame and project it with a transparency slider (0–100%) and pixel-diff highlighter over the live Compose preview.
* **Benefit**: Eliminates "design drift" by catching 4dp padding errors, incorrect font weights, or divergent corner radii instantly.

---

## 4. CI/CD Visual Regression & PR Review Bot
* **Concept**: Turn Compositor previews into automated GitHub Actions / GitLab CI visual regression checks.
* **Mechanism**:
  * Headless CLI command: `compositor test --golden-dir=snapshots/`.
  * Renders all previews in headless CI (Linux/macOS), diffs against golden baseline images, and posts visual diff images as automated PR comments.
* **Benefit**: Zero-device screenshot testing built directly into the standard Compositor workflow.

---

## 5. Cloud Preview Sharing & Static Storybook Export
* **Concept**: Export a self-contained static HTML/Wasm package containing all rendered previews for design team reviews.
* **Mechanism**:
  * `compositor export --output=docs/storybook`: Dumps all preview PNGs, metadata, and an embedded static web viewer that can be hosted on GitHub Pages or S3.
* **Benefit**: Designers and product managers can inspect all component states in a browser without needing Android development environments or SDKs installed.
