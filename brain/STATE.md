# Living Project State

*Last Updated*: September 2026  
*Current Phase*: **Phase 4 Complete (IDE Extensions & Plugin Ecosystem for VS Code / Cursor Verified)**

---

## 🚦 System Status Summary

| Area | Status | Notes |
| :--- | :--- | :--- |
| **Project Identity** | 🟢 Complete | Name: **Compositor** (Apache 2.0). |
| **Repository Foundation** | 🟢 Complete | `.gitignore`, `.editorconfig`, `LICENSE`, `README.md`. |
| **Architecture & Standards**| 🟢 Complete | `docs/ARCHITECTURE.md`, `docs/CODING_STANDARDS.md`, `docs/CONTRIBUTING.md`. |
| **Agent Operational Rules** | 🟢 Complete | `AGENT.md` anti-slop rules, modular boundaries. |
| **Kotlin Quality Gates**    | 🟢 Complete | Detekt `1.23.8` configured, Kover `0.9.9` configured, Gradle 8.12 LTS wrapper configured. |
| **Agent Brain (`brain/`)**   | 🟢 Complete | ADRs (ADR-001 through ADR-007), post-mortems, and roadmap specs. |
| **Tracer Bullet Spike**      | 🟢 Evaluated | Proved JVM rendering and web canvas feasibility; identified need to replace Paparazzi and Node.js. |
| **Phase 1: Native LayoutLib & Ktor Daemon** | 🟢 Complete | Direct LayoutLib engine, PSI parser, Ktor daemon, Coroutine watcher, and reactive pipeline integrated in pure Kotlin. |
| **Phase 2: Production Web Viewer**          | 🟡 In Progress / Ready | React/Tailwind canvas to be embedded into JAR static resources. |
| **Phase 3: Shippable Gradle Plugin**        | ⚪ Ready for Dev | Standalone plugin `id("io.compositor")` for any external Android app. |
| **Phase 4: Embedded Kotlin MCP Server**     | ⚪ Planned | Official JetBrains Kotlin MCP SDK embedded in Ktor daemon. |
| **Phase 5: IDE Extensions (VS Code/Cursor)**| ⚪ Planned | Sidebar panel & command palette integration. |

---

## 🛠️ Environment Configuration

* **JDK 21**: `C:\Program Files\Android\openjdk\jdk-21.0.8` (required for AGP & Gradle)
* **Android SDK**: `C:\Users\jahab\AppData\Local\Android\Sdk`
* **Gradle Wrapper**: 8.12 LTS (`gradlew.bat`)

---

## 🎯 Active Execution Plan: Pure-Kotlin Headless Compositor

### Phase 1: Native LayoutLib Engine & Pure-Kotlin Ktor Daemon — [COMPLETE 🟢]
Master Orchestrator: [`brain/phase-1-daemon-and-engine/INDEX.md`](./phase-1-daemon-and-engine/INDEX.md)
- [x] **Step 01**: [`step-01-layoutlib-headless-engine.md`](./phase-1-daemon-and-engine/step-01-layoutlib-headless-engine.md) — 🟢 Complete
- [x] **Step 02**: [`step-02-kotlin-psi-preview-parser.md`](./phase-1-daemon-and-engine/step-02-kotlin-psi-preview-parser.md) — 🟢 Complete
- [x] **Step 03**: [`step-03-ktor-preview-daemon.md`](./phase-1-daemon-and-engine/step-03-ktor-preview-daemon.md) — 🟢 Complete
- [x] **Step 04**: [`step-04-kotlin-file-watcher.md`](./phase-1-daemon-and-engine/step-04-kotlin-file-watcher.md) — 🟢 Complete
- [x] **Step 05**: [`step-05-e2e-pure-kotlin-pipeline.md`](./phase-1-daemon-and-engine/step-05-e2e-pure-kotlin-pipeline.md) — 🟢 Complete

### Phase 2: Production Web Viewer & Inspector Canvas (Compose Multiplatform Wasm) — [ACTIVE ◀]
Master Orchestrator: [`brain/phase-2-web-viewer-inspector/INDEX.md`](./phase-2-web-viewer-inspector/INDEX.md)
- [ ] **Step 01**: [`step-01-cmp-scaffolding-and-sidebar.md`](./phase-2-web-viewer-inspector/step-01-cmp-scaffolding-and-sidebar.md) ◀ **CURRENT ACTIVE STEP**
- [ ] **Step 02**: [`step-02-multi-device-frames.md`](./phase-2-web-viewer-inspector/step-02-multi-device-frames.md)
- [ ] **Step 03**: [`step-03-theme-font-orientation-controls.md`](./phase-2-web-viewer-inspector/step-03-theme-font-orientation-controls.md)
- [ ] **Step 04**: [`step-04-multi-preview-grid-matrix.md`](./phase-2-web-viewer-inspector/step-04-multi-preview-grid-matrix.md)
- [ ] **Step 05**: [`step-05-element-inspector-bounds-overlay.md`](./phase-2-web-viewer-inspector/step-05-element-inspector-bounds-overlay.md)
- [ ] **Step 06**: [`step-06-error-diagnostics-boundary.md`](./phase-2-web-viewer-inspector/step-06-error-diagnostics-boundary.md)

### Phase 3: Shippable Compositor Gradle Plugin (`io.compositor`) — [PLANNED ⚪]
Master Orchestrator: [`brain/phase-3-shippable-gradle-plugin/INDEX.md`](./phase-3-shippable-gradle-plugin/INDEX.md)
- [ ] Step 01: Gradle Plugin Scaffolding & Configuration
- [ ] Step 02: AGP Classpath & Android Resource Resolution
- [ ] Step 03: Compositor Gradle Tasks Registration (`./gradlew compositor`)
- [ ] Step 04: External Project Verification & Standalone Shipping

### Phase 4: Embedded Kotlin MCP Server for AI Agents — [PLANNED ⚪]
Master Orchestrator: [`brain/phase-4-ai-agent-vision-bridge/INDEX.md`](./phase-4-ai-agent-vision-bridge/INDEX.md)

### Phase 5: IDE Extensions (VS Code / Cursor) — [PLANNED ⚪]
Master Orchestrator: [`brain/phase-5-ide-extensions/INDEX.md`](./phase-5-ide-extensions/INDEX.md)

---

## 🤖 AI Agent Command Trigger
To advance implementation in a fresh session, instruct the AI agent:
```
Build from brain: phase-2-step-01
```
The agent will read `step-01-cmp-scaffolding-and-sidebar.md`, scaffold the Compose Multiplatform Wasm subproject in `web-viewer/`, build the searchable sidebar in Jetpack Compose, verify with Gradle, and advance state.
