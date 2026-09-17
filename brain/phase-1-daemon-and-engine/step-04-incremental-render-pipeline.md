# Step 04: Incremental Render Pipeline & Dispatcher

*Phase*: 1 — Auto-Watch Daemon & Incremental Engine  
*Status*: Complete ✅  
*Target Module*: `cli`

---

## 1. Objective
Establish an automated dispatch pipeline that receives change notifications from the file watcher, determines which previews are affected, and triggers fast incremental JVM rendering without rebuilding unrelated modules.

---

## 2. Functional Requirements
* **Dirty Target Detection**: Map the modified `.kt` file to its specific preview(s) so only affected composables are re-rendered.
* **Execution Orchestration**: Trigger the headless JVM LayoutLib render pass using the warmed Gradle daemon or direct JVM runner.
* **Artifact Routing**: Store rendered bitmaps under `.compositor/previews/<preview_id>.png`.
* **State Updates**: Update the status in the preview registry (`Rendering` -> `Rendered` or `Error`) and record execution duration.
* **Performance Budget**: Target sub-2.5 second turnaround time for incremental saves on warm daemons.

---

## 3. High-Level Architectural Guidance
* Maintain a queue or concurrency lock to prevent overlapping render tasks when multiple files change in quick succession.
* Gracefully capture compilation or runtime exceptions during rendering and surface them through the registry as actionable error models (including file and line details).
* Preserve previous valid render artifacts when a compilation error occurs so the user still has visual context.

---

## 4. Verification & Quality Gates
* **Integration Test**: Programmatically mutate a composable function in `samples/sample-app`; verify the pipeline executes, generates an updated image, and updates the registry status to `Rendered` with non-zero byte size.
