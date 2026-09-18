# Phase 1: Native LayoutLib Engine & Pure-Kotlin Ktor Daemon

## Mission
Build the core engine of Compositor in **100% Kotlin (JVM)**:
1. Replace the Paparazzi JUnit spike with a direct, headless **LayoutLib rendering engine** in `core-renderer` that renders `@Preview` composables without test classes or boilerplate.
2. Build an embedded, lightweight **Ktor daemon** with coroutine-based file watching that serves previews over HTTP/WebSockets and bundles the web viewer static assets—requiring **zero Node.js runtime** for end users.

---

## Architectural Workflow (Pure Kotlin)
```
[ User Android Source (*.kt) ]
             │
             ▼ (Kotlin Coroutines File Watcher)
┌────────────────────────────────────────────────────────┐
│ Compositor Daemon (Pure Kotlin JVM)                    │
│  ├── 1. Kotlin Embedded PSI (AST Parser)               │
│  │   • Extracts @Preview metadata from source files    │
│  │                                                     │
│  ├── 2. Native LayoutLib Engine (core-renderer)        │
│  │   • Resolves AGP classpath & merged R.jar           │
│  │   • In-memory RenderSession execution (no JUnit)   │
│  │   • Emits high-res PNG in < 1.5s                    │
│  │                                                     │
│  └── 3. Embedded Ktor Server                           │
│      • Serves embedded Web Viewer UI from JAR          │
│      • Exposes REST & WebSocket preview events         │
└────────────────────────────────────────────────────────┘
```

---

## Step Progression

| Step | File | Scope | Status |
| :--- | :--- | :--- | :--- |
| **01** | [`step-01-layoutlib-headless-engine.md`](./step-01-layoutlib-headless-engine.md) | Decouple from Paparazzi; build direct LayoutLib in-memory JVM renderer in `core-renderer`. | 🟢 Complete |
| **02** | [`step-02-kotlin-psi-preview-parser.md`](./step-02-kotlin-psi-preview-parser.md) | Parse Kotlin `.kt` files using Kotlin Compiler Embedded PSI to extract `@Preview` metadata. | 🟢 Complete |
| **03** | [`step-03-ktor-preview-daemon.md`](./step-03-ktor-preview-daemon.md) | Implement embedded Ktor HTTP & WebSocket server, serving embedded web assets. | 🟢 Complete |
| **04** | [`step-04-kotlin-file-watcher.md`](./step-04-kotlin-file-watcher.md) | Build coroutine-based source directory watcher with debounced event dispatch. | 🟢 Complete |
| **05** | [`step-05-e2e-pure-kotlin-pipeline.md`](./step-05-e2e-pure-kotlin-pipeline.md) | Wire the pipeline end-to-end: file save -> PSI parse -> LayoutLib render -> Ktor push. | 🟢 Complete |

---

## Protocol for AI Agents
1. Inspect [`brain/STATE.md`](../STATE.md) to confirm Phase 1 is active.
2. Follow each step's high-level instructions strictly.
3. Write pure Kotlin code (targeting Java 21, Kotlin 2.x). Do not introduce Node.js runtime requirements.
4. Run Detekt (`./gradlew detekt`) and tests before completing each step.
