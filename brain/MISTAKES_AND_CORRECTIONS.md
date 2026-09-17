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


