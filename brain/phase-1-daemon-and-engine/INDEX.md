# Phase 1: Real-Time Auto-Watch Daemon & Incremental Engine

## Mission
Automate the discovery and execution of `@Preview` composables across the project. Replace manual Gradle snapshot tasks with a persistent background daemon that watches `.kt` files, parses preview metadata, triggers fast incremental JVM rendering, and updates an in-memory preview registry.

---

## Architecture Overview
```
[ Source Files (*.kt) ]
         │
         ▼ (File System Watcher)
┌──────────────────────────────────────────────────┐
│ Compositor Daemon                                │
│  ├── 1. File Change Filter & Debouncer           │
│  ├── 2. Preview AST Parser (Extract Annotations) │
│  ├── 3. Preview Index Registry (previews.json)   │
│  ├── 4. Incremental Render Dispatcher (JVM)      │
│  └── 5. REST & WebSocket Broadcast API          │
└──────────────────────────────────────────────────┘
```

---

## Step Progression

| Step | File | Scope |
| :--- | :--- | :--- |
| **01** | [`step-01-preview-ast-parser.md`](./step-01-preview-ast-parser.md) | Scan Kotlin source files and extract `@Preview` declarations with their parameters. |
| **02** | [`step-02-preview-index-model.md`](./step-02-preview-index-model.md) | Design data models and maintain `previews.json` index of all available composables. |
| **03** | [`step-03-file-watcher-daemon.md`](./step-03-file-watcher-daemon.md) | Implement file watching, debouncing, and filtering for UI source changes. |
| **04** | [`step-04-incremental-render-pipeline.md`](./step-04-incremental-render-pipeline.md) | Hook into the JVM snapshot engine to execute targeted single-preview renders. |
| **05** | [`step-05-daemon-rest-api.md`](./step-05-daemon-rest-api.md) | Expose HTTP endpoints for listing previews, inspecting state, and triggering manual renders. |

---

## Protocol for AI Agents
1. Inspect [`brain/STATE.md`](../STATE.md) to confirm Phase 1 is active.
2. Read the active step markdown file.
3. Follow the high-level instructions, respecting modular boundaries in `AGENT.md`.
4. Run the verification strategy for the step.
5. Update `brain/STATE.md` upon completion.
