# AGENT.md: Operational Guidelines for AI Assistants

This repository hosts **Compositor**, an open-source headless Jetpack Compose preview engine, local web viewer, and AI vision bridge.

This project is developed with AI assistance under human review. To prevent low-quality code generation ("AI slop") and maintain high engineering standards, **every AI assistant operating in this codebase MUST adhere strictly to the rules in this document.**

---

## 1. The Anti-Slop Manifesto

1. **No Fake / Mock Implementations in Core Engines**:
   - Do not write placeholder functions that return hardcoded mock images or fake layout trees when implementing the real preview pipeline.
   - All rendering code must interface directly with the real Android LayoutLib / `compose-preview-renderer` / Paparazzi JVM APIs.
   - If a feature is in progress or blocked, document the interface and throw `NotImplementedError("Explicit reason")` rather than pretending it works with deceptive dummy data.

2. **Strict Type Safety & Zero Ambiguity**:
   - **Kotlin**: No untyped `Any?` returns when explicit domain models exist. Enable `explicitApi()` in library modules. Use sealed interfaces/classes for state and error modeling.
   - **TypeScript**: No `any` types allowed. Use strict mode (`strict: true`). All WebSocket frames, REST responses, and MCP payloads must be validated via schema or type guards (e.g. Zod or strict interfaces).

3. **No Silent Failures**:
   - Never write empty catch blocks (`try { ... } catch (e: Exception) {}`).
   - All errors must be either properly logged with diagnostic context, wrapped in domain error types, or propagated cleanly to the caller.

4. **Production-Grade Clean Architecture**:
   - Separate business logic from I/O and UI.
   - Follow single responsibility: the renderer rasterizes, the daemon watches files, the web server serves assets/websockets, and the web viewer displays.

5. **Test-Driven Rigor**:
   - Never mark a feature complete without automated tests (unit tests for parsers, integration tests for CLI commands, component tests for web UI).

---

## 2. Repository Layout & Module Boundaries

```
compositor/
├── brain/                     # AI Agent persistent memory, ADRs, and mistake catalog
│   ├── README.md              # Cognitive protocol for LLMs
│   ├── DECISIONS.md           # Architectural Decision Records (ADRs)
│   ├── MISTAKES_AND_CORRECTIONS.md # Traps, bugs, and platform post-mortems
│   └── STATE.md               # Living snapshot of progress and priorities
├── docs/                      # Architectural specifications & standards
│   ├── ARCHITECTURE.md        # In-depth subsystem design and data flow
│   ├── CODING_STANDARDS.md    # Language conventions, linting, formatting
│   └── CONTRIBUTING.md        # Open-source contribution guidelines
├── core-renderer/             # [Kotlin JVM] LayoutLib / compose-preview-renderer engine
├── cli/                       # [Kotlin / Node] Command-line daemon & file watcher
├── web-viewer/                # [TypeScript / Vite / Tailwind] Interactive preview dashboard
├── mcp-server/                # [TypeScript / Node] Model Context Protocol server for AI tools
├── samples/                   # Real-world testbed Android applications
│   └── sample-app/            # Baseline Jetpack Compose application
├── AGENT.md                   # This guideline document
├── LICENSE                    # Apache 2.0
└── README.md                  # Project overview and quickstart
```

### Inviolable Module Boundaries:
* `core-renderer` must **NOT** depend on `web-viewer` or `mcp-server`. It is a pure JVM library.
* `web-viewer` must communicate with the daemon exclusively via defined HTTP / WebSocket contracts.
* `mcp-server` acts as a client to the Compositor daemon; it exposes standard MCP tools for AI agents (`render_preview`, `list_previews`, `get_preview_image`).

---

## 3. The `brain/` AI Agent Protocol

1. **Mandatory Pre-Flight Check**:
   - Before executing any task, inspect [`brain/STATE.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/STATE.md) to understand current progress and [`brain/MISTAKES_AND_CORRECTIONS.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/MISTAKES_AND_CORRECTIONS.md) to avoid known pitfalls.
2. **Post-Task Debrief**:
   - Upon encountering and resolving any tricky problem (build failure, platform incompatibility, unexpected framework behavior), append a new entry to `brain/MISTAKES_AND_CORRECTIONS.md`.
   - Update `brain/STATE.md` to reflect newly verified components and next immediate priorities.
   - For architectural choices, log an ADR in `brain/DECISIONS.md`.

---

## 4. Local Environment & Quality Gates

* **Operating System**: Windows (PowerShell environment)
* **JDK Version**: Java 21 (`C:\Program Files\Android\openjdk\jdk-21.0.8`)
* **Android SDK**: `C:\Users\jahab\AppData\Local\Android\Sdk`
* **Node.js**: v24.x LTS with npm

### Automated Quality Checks:
* **Kotlin Static Analysis**: `./gradlew detekt` (enforces 120 char max line length, complexity, and safety).
* **Kotlin Test Coverage**: `./gradlew koverHtmlReport`.
* **TypeScript Linting**: `npm run lint` inside `web-viewer/` (enforces no `any` and 120 char max line length).
* **TypeScript Coverage**: `npm run test:coverage` inside `web-viewer/`.

---

## 5. Git & Commit Guidelines

* Use **Conventional Commits**:
  - `feat: <description>` for new capabilities
  - `fix: <description>` for bug fixes
  - `docs: <description>` for documentation updates
  - `refactor: <description>` for structural improvements without feature changes
  - `test: <description>` for adding or updating tests
  - `chore: <description>` for build scripts, configs, dependencies
* Make atomic, focused commits with concise, professional messages.
