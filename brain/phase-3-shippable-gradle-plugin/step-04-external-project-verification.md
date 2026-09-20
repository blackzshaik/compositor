# Step 04: External Project Verification & Standalone Shipping

*Phase*: 3 — Shippable Compositor Gradle Plugin  
*Status*: Complete 🟢  
*Target Module*: `plugin` & external validation

---

## 1. Objective
Verify that the Compositor Gradle Plugin functions as a truly shippable, plug-and-play tool by testing it against an external, independent Android project (outside the Compositor repo) using local Maven publishing or composite build integration.

---

## 2. Verification Protocol & Sequence
* **Local Publication**:
  * Publish Compositor plugin and core engine artifacts to the local Maven repository using `./gradlew publishToMavenLocal`.
* **External Android App Setup**:
  * Point an external Android Compose project (or isolated test fixture outside the monorepo) to `mavenLocal()`.
  * Add `id("io.compositor") version "0.1.0-SNAPSHOT"` to the external app's `build.gradle.kts`.
* **Execution & Verification**:
  * Execute `./gradlew compositor` in the external app directory.
  * Confirm that:
    1. The embedded Ktor daemon launches and opens the web browser.
    2. All `@Preview` composables in the external project are automatically discovered.
    3. Merged Android resources (`R.string`, `R.drawable`, custom themes) render accurately without `ResourceNotFoundException`.
    4. Editing a file in the external project triggers a sub-2-second hot reload in the browser.

---

## 3. Post-Execution Checklist
1. Commit changes: `feat: implement shippable Compositor Gradle plugin`.
2. Update [`brain/STATE.md`](../STATE.md) marking Phase 3 complete.
