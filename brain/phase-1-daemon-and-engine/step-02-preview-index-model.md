# Step 02: Preview Index Registry & Data Model

*Phase*: 1 — Auto-Watch Daemon & Incremental Engine  
*Status*: Blocked by Step 01  
*Target Module*: `cli`

---

## 1. Objective
Establish the domain data models for preview metadata and create a centralized registry (`previews.json`) that tracks all discovered previews across project modules, their configuration states, and rendered frame paths.

---

## 2. Functional Requirements
* Define strict TypeScript interfaces / schemas:
  * `PreviewItem`: Unique ID, function name, module, file path, line number, extracted parameters, timestamp, and render status (Pending, Rendering, Rendered, Error).
  * `PreviewCatalog`: Grouped index by module, file, and tag.
* Implement a `PreviewRegistry` manager:
  * In-memory cache for fast query resolution.
  * Atomic synchronization to `.compositor/previews.json`.
  * Incremental upsert and removal when files are added, modified, or deleted.

---

## 3. High-Level Architectural Guidance
* Ensure unique, deterministic IDs for each preview (e.g. `module:package.ClassName.FunctionName#PreviewName`).
* Support querying by module, by file path, or by tag.
* Guarantee thread-safe or concurrency-safe read/write operations on the JSON index.
* Decouple the registry storage from the file watcher and the HTTP server.

---

## 4. Verification & Quality Gates
* **Unit Tests**: Verify index creation, incremental updates upon file modifications, and entry deletion when a preview is removed from source.
* **Integrity Check**: Ensure `.compositor/previews.json` is formatted cleanly and adheres to the JSON schema.
