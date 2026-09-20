# Step 03: Compositor Gradle Tasks Registration

*Phase*: 3 — Shippable Compositor Gradle Plugin  
*Status*: Complete 🟢  
*Target Module*: `plugin`

---

## 1. Objective
Register the primary user-facing Gradle tasks on target Android modules: `./gradlew compositor` (starts the interactive daemon, opens the web browser, and begins live watching) and `./gradlew compositorRender` (headless batch render for CLI scripts or CI).

---

## 2. Architectural Design & Responsibilities
* **Task: `compositor` (Interactive Daemon Mode)**:
  * Inherits from `DefaultTask` and depends on `compileDebugKotlin` and `processDebugResources`.
  * Instantiates and starts the embedded Ktor preview server.
  * Launches the default system web browser to `http://localhost:3001` (if `autoOpenBrowser` is enabled).
  * Starts the Coroutines source watcher and remains running until interrupted (`Ctrl + C`).
* **Task: `compositorRender` (Headless Batch Mode)**:
  * Discovers all `@Preview` composables via the Kotlin PSI parser.
  * Renders each preview via the LayoutLib engine to `.compositor/previews/`.
  * Generates an index summary and exits with code 0 upon completion.

---

## 3. High-Level Integration Guidance
* Use Gradle's Worker API or task action execution to isolate daemon execution cleanly.
* Ensure graceful shutdown: attach a JVM shutdown hook so the Ktor server and file watcher terminate properly when the user presses `Ctrl + C` in their terminal.

---

## 4. Verification & Quality Gates
* **Functional Test**: Run `./gradlew compositorRender` on a test Android module; assert that all preview images are generated and the task finishes with `BUILD SUCCESSFUL`.
