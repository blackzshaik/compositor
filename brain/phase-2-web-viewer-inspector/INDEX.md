# Phase 2: Production Web Viewer & Interactive Canvas (Compose Multiplatform Wasm)

## Mission
Build the web viewer frontend in **100% Kotlin** using **Compose Multiplatform for Web (Kotlin/Wasm - `wasmJs`)**. 
By compiling Jetpack Compose UI code directly to WebAssembly with hardware-accelerated Skiko Canvas rendering, the web viewer requires **zero npm, zero Node.js, and zero JavaScript build tools**. It builds entirely via Gradle (`./gradlew :web-viewer:wasmJsBrowserDistribution`).

---

## Architectural Overview
```
┌─────────────────────────────────────────────────────────────────────────────┐
│              COMPOSE MULTIPLATFORM WEB VIEWER (Kotlin/Wasm)                 │
├─────────────────┬───────────────────────────────────────────────────────────┤
│ Preview Sidebar │ Main Canvas Viewport                                      │
│  • Search & Tag │  • Device Shell Canvas (Pixel / Galaxy / Tablet)          │
│  • Module Tree  │  • Floating TopAppBar (Theme, Font Scale, Rotate)         │
│  • LazyColumn   │  • Multi-Preview Matrix Grid (LazyVerticalGrid)           │
│    Composable   │  • Element Bounds Canvas Overlay & Error Callouts         │
└─────────────────┴───────────────────────────────────────────────────────────┘
```

---

## Step Progression

| Step | File | Scope |
| :--- | :--- | :--- |
| **01** | [`step-01-cmp-scaffolding-and-sidebar.md`](./step-01-cmp-scaffolding-and-sidebar.md) | Scaffold Compose Multiplatform Wasm subproject & build searchable preview sidebar in Compose. |
| **02** | [`step-02-multi-device-frames.md`](./step-02-multi-device-frames.md) | Device bezel mockups (Pixel 8, Galaxy S24, Tablet) rendered via Compose Canvas & Shapes. |
| **03** | [`step-03-theme-font-orientation-controls.md`](./step-03-theme-font-orientation-controls.md) | Material 3 inspector controls: Light/Dark switch, font scale slider (0.85x-1.5x), and rotation. |
| **04** | [`step-04-multi-preview-grid-matrix.md`](./step-04-multi-preview-grid-matrix.md) | Side-by-side comparison matrix using Compose `LazyVerticalGrid`. |
| **05** | [`step-05-element-inspector-bounds-overlay.md`](./step-05-element-inspector-bounds-overlay.md) | Interactive hover overlays highlighting Composable bounds and semantic nodes. |
| **06** | [`step-06-error-diagnostics-boundary.md`](./step-06-error-diagnostics-boundary.md) | Error card with stack trace callouts and auto-dismiss on successful re-render. |

---

## Protocol for AI Agents
1. All UI code must be written in **pure Kotlin** using Compose Multiplatform Material 3.
2. Build and verify using Gradle: `./gradlew :web-viewer:wasmJsBrowserDistribution`.
3. Do **NOT** introduce any `package.json`, npm packages, or Node.js scripts.
4. Adhere to strict Detekt rules and Compose best practices (immutable state, hoisting, derivedStateOf).
