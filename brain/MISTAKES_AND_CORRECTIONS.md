# Mistakes, Pitfalls & Corrections

This document serves as an institutional memory log of engineering pitfalls, platform quirks, and invalid assumptions encountered during Compositor development. **Review this file before starting any implementation task to avoid repeating these mistakes.**

---

### Entry 001: Native Android SDK Cannot Directly Compile to WebAssembly (Wasm)
* **Date**: September 2026
* **Category**: Architecture / Multiplatform
* **Initial Assumption**:
  A standard Android Jetpack Compose app can simply be built with the Android SDK and presented in a web browser using Kotlin Multiplatform.
* **Root Cause**:
  Standard Android applications compile to Dalvik Executable (`.dex`) bytecode and heavily link against the Android OS C++/Java framework (`android.content.Context`, `android.os.*`, `R.string`, Android Drawables). Browsers execute JavaScript and WebAssembly; they have no Dalvik runtime or Android framework services. While Compose Multiplatform (CMP) has a Wasm target, it only compiles pure `commonMain` code that has zero Android SDK imports.
* **Correction & Prevention**:
  Never attempt to run Android `.dex` or Android-specific `@Composable` code in a browser engine. Instead, execute the `@Preview` headlessly on the host desktop's JVM using Android's **LayoutLib** engine (`compose-preview-renderer`), rasterize the view into an in-memory bitmap, and stream the resulting image over WebSocket to a browser canvas.

---

### Entry 002: Java Version Incompatibility (Java 26 vs Java 21 LTS)
* **Date**: September 2026
* **Category**: Build System / Tooling
* **Symptom**:
  System PATH default Java is JDK 26 (`java version "26.0.1"`). Running Gradle or Android build tools directly with Java 26 can result in `Unsupported class file major version` or Gradle JVM validation errors.
* **Root Cause**:
  The Android Gradle Plugin (AGP) and Gradle ecosystem strictly require Java 17 or Java 21 LTS. Java 26 is too new for current AGP tooling.
* **Correction & Prevention**:
  Always anchor Gradle tasks and JVM daemon executions to JDK 21 at `C:\Program Files\Android\openjdk\jdk-21.0.8` or Android Studio's bundled JBR. In Gradle build scripts, enforce the toolchain:
  ```kotlin
  java {
      toolchain {
          languageVersion.set(JavaLanguageVersion.of(21))
      }
  }
  ```

---

### Entry 003: Agent Tooling - Artifact Metadata vs Workspace Source Files
* **Date**: September 2026
* **Category**: AI Assistant Tooling
* **Symptom**:
  `write_to_file` returned `invalid tool call error: ... is not a valid artifact path; artifacts must be in brain/`.
* **Root Cause**:
  Providing `ArtifactMetadata` instructs the tool runner that the target is a conversational artifact. Workspace project files are not artifacts.
* **Correction & Prevention**:
  When creating or editing workspace project files (`c:\Users\...\`), omit `ArtifactMetadata`. Only include `ArtifactMetadata` when writing to the conversation artifact directory (`C:\Users\jahab\.gemini\antigravity\brain\...`).

---

### Entry 004: Comment and Expression Line-Length Creep ("AI Slop" Vulnerability)
* **Date**: September 2026
* **Category**: Code Quality / Linting
* **Symptom**:
  AI assistants occasionally generate comments or chained method calls that stretch past 150+ columns.
* **Root Cause**:
  Markdown guidelines alone are insufficient without automated static analysis enforcement.
* **Correction & Prevention**:
  Configure **Detekt** (`MaxLineLength: 120` with `excludeCommentStatements: false`) for Kotlin and **ESLint** (`max-len: 120`) for TypeScript. Run `./gradlew detekt` and `npm run lint` to enforce formatting automatically.

---

### Entry 005: AndroidX Flag & Subproject Plugin Management
* **Date**: September 2026
* **Category**: Android Build System / AGP
* **Symptom**:
  `checkDebugAarMetadata` failed with: `Configuration contains AndroidX dependencies, but the android.useAndroidX property is not enabled`.
  Subproject build scripts applying `id("org.jetbrains.kotlin.android")` directly failed with `ClassNotFoundException: com/android/build/gradle/api/BaseVariant`.
* **Root Cause**:
  1. AGP strictly requires `android.useAndroidX=true` in `gradle.properties`.
  2. Kotlin Android Gradle Plugin must be managed via version catalog and applied with `apply false` in root `build.gradle.kts` to guarantee subprojects resolve the matching version.
* **Correction & Prevention**:
  Ensure `gradle.properties` contains `android.useAndroidX=true`. Always define `kotlin-android` in `gradle/libs.versions.toml` and declare `alias(libs.plugins.kotlin.android) apply false` in root `build.gradle.kts`.

---

### Entry 006: Gradle 9.x vs AGP 8.8 TestResultsProvider Incompatibility
* **Date**: September 2026
* **Category**: Gradle Tooling / AGP Compatibility
* **Symptom**:
  Running `:samples:sample-app:testDebugUnitTest` under Gradle 9.5 failed with:
  `NoSuchMethodError: 'boolean org.gradle.api.internal.tasks.testing.junit.result.TestResultsProvider.hasOutput(long, ...)'`.
* **Root Cause**:
  AGP 8.8 is designed for Gradle 8.10.2 - 8.12. Gradle 9.x removed internal testing methods that AGP 8.8's test reporting task invokes.
* **Correction & Prevention**:
  Anchor Gradle wrapper strictly to Gradle 8.12 LTS in `gradle/wrapper/gradle-wrapper.properties`:
  `distributionUrl=https\://services.gradle.org/distributions/gradle-8.12-bin.zip`.

---

### Entry 007: Child Process Launcher JVM Inherits Host JDK 26 Instead of JDK 21
* **Date**: September 2026
* **Category**: Process Environment / Windows / JVM Tooling
* **Symptom**:
  Render dispatcher executions failed with `What went wrong: 26.0.1` inside `errorDetails`.
* **Root Cause**:
  `render-dispatcher.ts` used `process.env.JAVA_HOME || 'C:\\Program Files\\Android\\openjdk\\jdk-21.0.8'`. Because the user's system environment had `JAVA_HOME=C:\Program Files\Java\jdk-26.0.1`, the fallback was never evaluated. The Gradle client launcher ran with JDK 26, failing AGP's Java compatibility check before the Gradle daemon could take over.
* **Correction & Prevention**:
  1. Add `org.gradle.java.home=C:/Program Files/Android/openjdk/jdk-21.0.8` to `gradle.properties`.
  2. In `render-dispatcher.ts`, explicitly resolve and prioritize JDK 21 over system environment, and prepend JDK 21's `bin` directory to `PATH` in the spawned child process environment.

---

### Entry 008: Tailwind CSS v4 Requires `@tailwindcss/vite` Plugin & Daemon URL Proxying
* **Date**: September 2026
* **Category**: Frontend Tooling / Vite / Tailwind CSS v4
* **Symptom**:
  The web viewer dashboard appeared completely unstyled with broken layouts and missing preview images (showing fallback "Rendering preview..." SVG).
* **Root Cause**:
  1. Tailwind CSS v4 uses a dedicated Vite plugin (`@tailwindcss/vite`). Without this plugin configured in `vite.config.ts`, `@import "tailwindcss";` leaves utility layers uncompiled in Vite builds, generating zero CSS utility rules.
  2. The daemon serves relative image endpoints (`/api/previews/...`). When the web client ran on port 3000, unproxied relative requests targeted Vite's dev server rather than daemon port 3001, resulting in 404 image errors.
* **Correction & Prevention**:
  1. Always install and configure `@tailwindcss/vite` in `vite.config.ts`.
  2. Configure `/api` proxy in `vite.config.ts` targeting `http://localhost:3001` and use `resolvePreviewUrl()` to guarantee preview bitmaps always resolve to the active daemon host.

---

### Entry 009: IDE Background Gradle Sync Fails on Missing `androidTest` Annotation Processors
* **Date**: September 2026
* **Category**: Android Gradle Plugin (AGP) / IDE Tooling
* **Symptom**:
  When launching the project in VS Code or Cursor, Gradle sync or build fails with:
  `java.io.FileNotFoundException: ...\build\intermediates\annotation_processor_list\debugAndroidTest\javaPreCompileDebugAndroidTest\annotationProcessors.json`.
* **Root Cause**:
  VS Code's Gradle/Android/Kotlin language extensions automatically query and build all project test variants including `androidTest` (device instrumented tests). Because Compositor is a headless host-JVM preview engine that only uses unit tests (`debugUnitTest`), the sample app contains no `androidTest/` sources. AGP's `javaPreCompileDebugAndroidTest` task is never triggered by default build tasks, causing AGP to crash when looking for the missing intermediate JSON file.
* **Correction & Prevention**:
  Explicitly disable the `androidTest` variant component in `samples/sample-app/build.gradle.kts`:
  ```kotlin
  androidComponents {
      beforeVariants { variantBuilder ->
          variantBuilder.androidTest.enable = false
      }
  }
  ```
  And run `./gradlew assembleDebug assembleDebugUnitTest` to pre-generate all required compiler intermediates.

---

### Entry 010: PaparazziSdk Static State Poisoning and Teardown Timing
* **Date**: September 2026
* **Category**: Headless LayoutLib / Paparazzi Integration
* **Symptom**:
  1. `UninitializedPropertyAccessException: lateinit property sessionParamsBuilder has not been initialized` when
     subsequent renders occurred after an earlier failure or render.
  2. `AssertionFailedError: Root bounds should be captured ==> expected: not <null>` when extracting ViewInfo
     from `bridgeRenderSession` after `sdk.teardown()`.
* **Root Cause**:
  1. `PaparazziSdk` stores `renderer` and `sessionParamsBuilder` in companion object static fields. If preparation
     fails mid-way or is invoked repeatedly across requests, `PaparazziSdk` assumes it was already initialized
     while `sessionParamsBuilder` remains uninitialized.
  2. Calling `sdk.teardown()` releases and disposes `bridgeRenderSession` (`session.dispose()`), clearing
     all root views before `extractRootBounds()` can inspect them.
* **Correction & Prevention**:
  1. Always invoke `resetSdkState()` via reflection to reset `renderer` and `sessionParamsBuilder` to null
     before and after rendering sessions.
  2. Extract `rootBounds` from `sdk` inside the `try` block immediately after `snapshotMethod.invoke(...)`
     and strictly before `sdk.teardown()` is executed in the `finally` block.

---

### Entry 011: Standalone JVM Engine Requires Android SDK Platform JAR and Compose Dependencies
* **Date**: September 2026
* **Category**: Headless Rendering / ClassLoading
* **Symptom**:
  1. `ClassNotFoundException: android.os.Build$VERSION` during `ComposeView` composition in pure JVM test tasks.
  2. `NoClassDefFoundError: androidx.compose.ui.platform.ComposeView` when `PaparazziSdk.snapshot(...)` is invoked.
* **Root Cause**:
  `core-renderer` is a pure Kotlin JVM library without AGP dependencies. While `layoutlib-runtime` provides native
  binaries and resources, Android framework classes (like `android.os.Build`) reside in `android.jar`, and Compose
  classes reside inside AAR bundles compiled by Android application modules.
* **Correction & Prevention**:
  1. Include `platforms/android-35/android.jar` on the runtime classpath of tests or the daemon engine.
  2. Export and extract AAR classes from the target Android application (via `exportDebugClasspath` task) and
     wire the resulting JARs into the classpath so that `ComposeView` and its dispatcher dependencies resolve.

---

### Entry 012: Kotlin Compiler Embeddable Relocates IntelliJ OpenAPI Packages
* **Date**: September 2026
* **Category**: Embedded Compiler Tooling / Classpath
* **Symptom**:
  `compileKotlin` failed with:
  `Unresolved reference 'Disposer'` when importing `com.intellij.openapi.util.Disposer`.
* **Root Cause**:
  `org.jetbrains.kotlin:kotlin-compiler-embeddable` relocates all bundled IntelliJ IDEA OpenAPI classes
  under `org.jetbrains.kotlin.com.intellij.*` to avoid binary collisions with IDE runtime plugins.
* **Correction & Prevention**:
  Always import IntelliJ utility and AST classes from `org.jetbrains.kotlin.com.intellij.*` (e.g.
  `org.jetbrains.kotlin.com.intellij.openapi.util.Disposer`) when working in modules using
  `kotlin-compiler-embeddable`.
