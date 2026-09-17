# Step 03: File Watcher Daemon & Event Pipeline

*Phase*: 1 — Auto-Watch Daemon & Incremental Engine  
*Status*: Blocked by Step 02  
*Target Module*: `cli`

---

## 1. Objective
Build a background file system watcher that monitors Android source directories for Kotlin file changes, applies intelligent debouncing and filtering, and feeds change events into the preview parsing and rendering pipeline.

---

## 2. Functional Requirements
* Recursively watch all source roots (e.g. `src/main/java`, `src/main/kotlin`) across all project modules.
* Ignore build artifacts, caches, and non-source files (`build/`, `.gradle/`, `.compositor/`, `.git/`).
* Debounce rapid multi-file save bursts (e.g. 200–300ms debounce window).
* Categorize file events:
  * **File Modified**: Re-parse previews in file, update registry, trigger incremental render.
  * **File Added**: Parse new previews and register.
  * **File Deleted**: Remove corresponding entries from the preview registry.

---

## 3. High-Level Architectural Guidance
* Leverage a high-performance cross-platform file watcher (e.g. `chokidar` in Node or native file watchers).
* Emit typed internal events (`FileChangeEvent`, `PreviewIndexUpdatedEvent`).
* Ensure resilient error handling: daemon must never crash on transient locked-file access or permission issues (common on Windows).
* Allow external control (pause watcher during full builds, resume on completion).

---

## 4. Verification & Quality Gates
* **Automated Integration Test**: Create, modify, and delete a dummy `.kt` file programmatically; verify the watcher accurately captures events and triggers the callback within the expected debounce window.
