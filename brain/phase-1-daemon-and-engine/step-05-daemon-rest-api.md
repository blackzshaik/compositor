# Step 05: Daemon REST & WebSocket API Suite

*Phase*: 1 — Auto-Watch Daemon & Incremental Engine  
*Status*: Blocked by Step 04  
*Target Module*: `cli`

---

## 1. Objective
Expand the local daemon server to expose a comprehensive REST and WebSocket API suite allowing the Web Viewer, IDE extensions, and AI agents to query preview catalogs, trigger on-demand renders, stream images, and listen for live updates.

---

## 2. API Contract Specifications

### REST Endpoints:
* `GET /api/previews`: Returns the complete catalog of discovered previews, their parameters, and render statuses.
* `GET /api/previews/:id`: Returns detailed metadata for a specific preview.
* `GET /api/previews/:id/image`: Streams the latest rendered PNG bitmap with appropriate cache headers.
* `POST /api/previews/:id/render`: Triggers a targeted re-render of a specific composable preview.
* `GET /api/daemon/status`: Returns daemon health, watcher status, active project root, and memory metrics.

### WebSocket Protocol:
* Emits events to all connected clients:
  * `PREVIEW_REGISTERED`: A new preview was discovered in source.
  * `PREVIEW_RENDER_STARTED`: A render pass has begun.
  * `PREVIEW_UPDATED`: A fresh frame is ready with image URL and timestamp.
  * `RENDER_ERROR`: A compilation or rendering failure occurred, with parsed diagnostic messages.

---

## 3. High-Level Architectural Guidance
* Maintain CORS headers for local web client consumption (`http://localhost:3000`).
* Support query parameter cache-busting (e.g. `?v=<timestamp>`).
* Ensure robust error responses: return JSON error payloads with standard HTTP status codes (400, 404, 500) rather than hanging connections.

---

## 4. Verification & Quality Gates
* **Automated API Tests**: Run Vitest suite querying each REST endpoint and asserting status codes, payload schemas, and headers.
* **WebSocket Test**: Connect a mock client, trigger an update event, and verify message reception within 50ms.
