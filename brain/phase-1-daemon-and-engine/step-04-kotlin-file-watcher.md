# Step 04: Native Kotlin Coroutine Source Directory Watcher

*Phase*: 1 — Native LayoutLib Engine & Pure-Kotlin Ktor Daemon  
*Status*: 🟢 Completed (Verified with Unit Tests & Detekt)  
*Target Module*: `core-renderer` (package `io.compositor.watcher`)

---

## 1. Objective
Build a lightweight, cross-platform file system watcher in pure Kotlin using Coroutines and Flow. This monitors Android project source folders for `.kt` file changes, debounces rapid IDE save bursts, and triggers incremental LayoutLib re-renders.

---

## 2. Architectural Design & Responsibilities
* **Directory Monitoring**:
  * Recursively watch Kotlin source directories (`src/main/java`, `src/main/kotlin`).
  * Implement using Java NIO `WatchService` or a lightweight Kotlin directory watching abstraction.
* **Exclusion & Filtering**:
  * Strictly filter out non-source directories (`build/`, `.gradle/`, `.git/`, `.compositor/`).
  * Only emit change signals for `.kt` and relevant resource files (`res/values/*.xml`).
* **Coroutine Debouncing**:
  * Feed file change events into a Kotlin Coroutines `MutableSharedFlow`.
  * Apply a 200–300ms debounce window (`flow.debounce(...)`) to collapse multiple rapid editor save operations into a single atomic render pass.
* **Pipeline Dispatch**:
  * On debounced change:
    1. Re-scan the modified file using the Kotlin PSI parser.
    2. Identify affected `@Preview` composables.
    3. Trigger the `LayoutLibPreviewRenderer` for the affected previews.
    4. Notify connected clients over the Ktor WebSocket channel.

---

## 3. High-Level Integration Guidance
* Implement as a structured coroutine worker tied to a supervisor job (`CoroutineScope(Dispatchers.IO + SupervisorJob())`).
* Ensure resilient Windows file-lock handling: retry reads if the file is momentarily locked by the editor during write.

---

## 4. Verification & Quality Gates
* **Unit Tests**: Programmatically modify a temporary `.kt` file and verify the Flow debouncer emits exactly one event within the configured window.
* **Quality Gate**: Code must pass `./gradlew detekt`.
