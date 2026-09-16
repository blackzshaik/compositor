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
