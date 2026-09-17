# Phase 2: Production Web Viewer & Interactive Canvas

## Mission
Elevate the web viewer from a single-image mockup into a rich, full-featured developer inspection dashboard. Support browsing all discovered previews across modules, toggling realistic device bezels, inspecting font scaling and dark themes, viewing multi-preview comparison grids, and inspecting layout bounds.

---

## Architecture Overview
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             WEB VIEWER CANVAS                               │
├─────────────────┬───────────────────────────────────────────────────────────┤
│ Preview Sidebar │ Main Canvas Viewport                                      │
│  • Search & Tag │  • Device Shell (Pixel / Galaxy / Tablet)                 │
│  • Module Tree  │  • Floating Inspector Bar (Theme, Font Scale, Rotate)     │
│  • Composable   │  • Multi-Preview Grid Mode                                │
│    List         │  • Element Bounds Overlay & Error Callouts                │
└─────────────────┴───────────────────────────────────────────────────────────┘
```

---

## Step Progression

| Step | File | Scope |
| :--- | :--- | :--- |
| **01** | [`step-01-preview-sidebar-tree.md`](./step-01-preview-sidebar-tree.md) | Multi-module navigation sidebar with search and tag filtering. |
| **02** | [`step-02-multi-device-frames.md`](./step-02-multi-device-frames.md) | Realistic device bezels: Pixel 8, Galaxy S24, Foldable, and Tablet. |
| **03** | [`step-03-theme-font-orientation-controls.md`](./step-03-theme-font-orientation-controls.md) | Controls for light/dark mode, font scaling (0.85x-1.5x), and rotation. |
| **04** | [`step-04-multi-preview-grid-matrix.md`](./step-04-multi-preview-grid-matrix.md) | Side-by-side grid comparing multiple preview configurations simultaneously. |
| **05** | [`step-05-element-inspector-bounds-overlay.md`](./step-05-element-inspector-bounds-overlay.md) | Interactive hover overlays highlighting Composable bounds and semantics. |
| **06** | [`step-06-error-stacktrace-boundary.md`](./step-06-error-stacktrace-boundary.md) | Resilient error boundary with inline stack traces and source links. |

---

## Protocol for AI Agents
1. Verify Phase 1 completion before starting Phase 2.
2. Read the active step document and adhere to Tailwind CSS and React quality standards in `docs/CODING_STANDARDS.md`.
3. Verify changes with `npm run build`, `npm run lint`, and `npm test` inside `web-viewer/`.
