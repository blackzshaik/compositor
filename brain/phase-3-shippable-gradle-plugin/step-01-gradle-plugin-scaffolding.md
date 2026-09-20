# Step 01: Gradle Plugin Scaffolding & Configuration

*Phase*: 3 — Shippable Compositor Gradle Plugin  
*Status*: Complete 🟢  
*Target Module*: `plugin`

---

## 1. Objective
Scaffold the `plugin/` Gradle subproject using `java-gradle-plugin` and `kotlin("jvm")`, establishing the plugin identifier `io.compositor`, plugin descriptor, and a user-facing extension block.

---

## 2. Architectural Design & Responsibilities
* **Plugin Project Setup**:
  * Create `plugin/build.gradle.kts` with `java-gradle-plugin` and `maven-publish`.
  * Define the plugin ID: `io.compositor`.
  * Specify implementation class: `io.compositor.plugin.CompositorPlugin`.
* **Compositor Extension**:
  * Define an extension DSL block (`compositor { ... }`) allowing developers to optionally customize:
    * `port`: Default `3001`.
    * `autoOpenBrowser`: Default `true`.
    * `preferredTheme`: Default `Theme.SYSTEM`.
* **Subproject Integration**:
  * Include `:plugin` in `settings.gradle.kts` and link dependency on `:core-renderer`.

---

## 3. High-Level Integration Guidance
* Use Gradle's Lazy Property APIs (`Property<Int>`, `Property<Boolean>`) for configuration cache compatibility.
* Ensure the plugin applies cleanly to both `com.android.application` and `com.android.library` modules.

---

## 4. Verification & Quality Gates
* **Automated Test**: Write a Gradle Functional Test using `GradleRunner` that applies `id("io.compositor")` to a test build; assert the plugin applies without errors and registers the `compositor` extension.
