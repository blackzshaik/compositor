# Step 03: Embedded Ktor HTTP & WebSocket Preview Daemon

*Phase*: 1 — Native LayoutLib Engine & Pure-Kotlin Ktor Daemon  
*Status*: 🟢 Completed (Verified with Test Suite & Detekt)  
*Target Module*: `core-renderer` (embedded daemon library)

---

## 1. Objective
Replace the Node.js preview server with an embedded, high-performance, coroutine-native **Ktor server** in Kotlin. The Ktor daemon serves preview assets, broadcasts live updates over WebSockets, and hosts the pre-compiled Web Viewer frontend directly from JAR resources—eliminating any Node.js requirement for end users.

---

## 2. Architectural Design & Responsibilities
* **Ktor Server Engine**:
  * Configure an embedded Ktor application engine (using Ktor Netty or CIO) targeting ports 3001 (API) and 3000 (UI), or unified on a single port.
  * Enable CORS, Content Negotiation with `kotlinx.serialization.json`, and WebSockets plugins.
* **Embedded Web UI Hosting**:
  * Package the pre-compiled `web-viewer/dist` HTML, CSS, and JS bundle into the JAR's `resources/web/` directory.
  * Configure Ktor routing to serve `/` and static assets directly from classpath resources.
* **REST API Routing**:
  * `GET /api/previews`: Returns the current preview catalog from memory.
  * `GET /api/previews/{id}/image`: Streams the generated PNG image with `Cache-Control: no-cache`.
  * `POST /api/previews/{id}/render`: Triggers an on-demand render pass.
  * `GET /api/status`: Reports daemon health, active Android project, and watcher state.
* **WebSocket Channel**:
  * WebSocket endpoint at `/ws` that broadcasts `PREVIEW_UPDATED`, `PREVIEW_ADDED`, and `RENDER_ERROR` JSON payloads to all connected browser clients.

---

## 3. High-Level Integration Guidance
* Implement lifecycle methods: `startDaemon(port: Int, projectRoot: File)` and `stopDaemon()`.
* Ensure clean shutdown hooks so the daemon releases network sockets gracefully when killed.
* Run as a lightweight background daemon consuming less than 150 MB heap.

---

## 4. Verification & Quality Gates
* **Integration Tests**: Start the Ktor server in a test fixture using Ktor's `testApplication`; verify endpoint routing, JSON serialization, and WebSocket event broadcasting.
* **Quality Gate**: Code must pass `./gradlew detekt`.
