# Step 01: Compose Multiplatform (Kotlin/Wasm) Scaffolding & Preview Sidebar

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Ready for Implementation  
*Target Module*: `web-viewer` (Kotlin Multiplatform Subproject)

---

## 1. Objective
Scaffold the `web-viewer` subproject as a pure **Compose Multiplatform (Kotlin/Wasm - `wasmJs`)** application built exclusively with Gradle. Build the first user-facing component: a collapsible, searchable navigation sidebar in Compose Material 3 that queries the Ktor daemon catalog (`/api/previews`).

---

## 2. Architectural Design & Responsibilities
* **Gradle Subproject Setup**:
  * Include `:web-viewer` in `settings.gradle.kts`.
  * In `web-viewer/build.gradle.kts`, apply:
    * `kotlin("multiplatform")` targeting `wasmJs { browser(); binaries.executable() }`.
    * Compose Multiplatform plugin (`org.jetbrains.compose`) with Compose Material 3, Runtime, Foundation, and UI dependencies.
    * Ktor Client (Wasm compatible) with `ktor-client-core`, `ktor-client-websockets`, and `kotlinx-serialization-json`.
* **Wasm Entrypoint**:
  * In `wasmJsMain/kotlin/main.kt`, initialize the Compose window using `CanvasBasedWindow(canvasElementId = "ComposeTarget") { App() }`.
* **Searchable Preview Sidebar (`@Composable`)**:
  * Implement using Material 3 `NavigationDrawer` or a responsive collapsible side panel.
  * Fetch preview items from Ktor daemon (`/api/previews`) via Ktor HTTP Client.
  * Provide instant search filtering with an `OutlinedTextField`.
  * Render the filtered preview catalog in a `LazyColumn`, grouping composables by module and source file.
  * Emit selection events when a user clicks a preview card.

---

## 3. High-Level Integration Guidance
* Use standard Jetpack Compose state management (`remember`, `mutableStateOf`, `derivedStateOf`).
* Model catalog loading states cleanly (Loading indicator, Empty state, Populated list, Network error).
* Ensure zero npm or Node.js commands are required. Everything builds via Gradle.

---

## 4. Verification & Quality Gates
* **Build Verification**: Run `./gradlew :web-viewer:wasmJsBrowserDistribution`; assert clean compilation and generation of Wasm executable artifacts in `build/dist/wasmJs/productionExecutable/`.
* **Quality Gate**: Code must pass `./gradlew detekt`.
