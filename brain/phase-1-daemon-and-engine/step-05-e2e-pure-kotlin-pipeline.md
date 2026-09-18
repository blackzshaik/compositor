# Step 05: End-to-End Pure-Kotlin Pipeline Integration & Cleanup

*Phase*: 1 — Native LayoutLib Engine & Pure-Kotlin Ktor Daemon  
*Status*: Blocked by Step 04  
*Target Module*: `core-renderer` & `cli`

---

## 1. Objective
Integrate all Phase 1 components into an automated end-to-end pipeline:
1. File save is detected by the Kotlin Coroutines watcher.
2. The modified file is parsed via Kotlin Embedded PSI.
3. The affected `@Preview` is rendered in-memory via the native LayoutLib engine without any JUnit test classes.
4. The Ktor server broadcasts an update over WebSockets and streams the new PNG.
5. Clean up the temporary Paparazzi test harness and Node.js server scripts from the Tracer Bullet.

---

## 2. Verification Sequence & Acceptance Criteria
* **Boilerplate Cleanup**:
  * Delete `samples/sample-app/src/test/java/com/compositor/sample/GreetingPreviewSnapshotTest.kt`.
  * Remove the Paparazzi plugin from `samples/sample-app/build.gradle.kts`.
  * Verify `samples/sample-app` contains only standard Android Compose code.
* **Pipeline Execution**:
  * Launch the pure-Kotlin Compositor daemon via CLI command or main function.
  * Mutate `samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt` (e.g. change text or background color).
  * Save the file.
* **Acceptance Gates**:
  1. No Node.js runtime process is involved anywhere in the pipeline.
  2. The updated preview image is rendered to `.compositor/previews/` in under 1.5 seconds.
  3. The Ktor WebSocket emits `PREVIEW_UPDATED` with the new timestamp.
  4. The browser view at `http://localhost:3000` (or `3001`) automatically refreshes to display the mutated preview.

---

## 3. Post-Execution Checklist
1. All Kotlin code passes `./gradlew detekt` and JUnit test suite.
2. Commit changes: `feat: implement pure-kotlin layoutlib preview pipeline and clean up spike`.
3. Update [`brain/STATE.md`](../STATE.md) marking Phase 1 complete.
