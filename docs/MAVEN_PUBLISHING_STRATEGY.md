# Maven & Plugin Publishing Strategy

This document outlines the strategy and configuration details for distributing **Compositor** via public Maven repositories and the Gradle Plugin Portal to provide a **zero-friction, single-line setup** for Android developers.

---

## 1. The Goal: Zero-Friction Developer Experience

Without public artifact distribution, an external developer must clone this repository, run `./gradlew publishToMavenLocal`, and configure local repositories.

Once published to a public repository, an Android developer only needs to add **one line** to their project:

```kotlin
// In app/build.gradle.kts
plugins {
    id("io.compositor") version "0.1.0-alpha01"
}
```

Gradle automatically handles fetching the plugin, headless LayoutLib engine, Ktor daemon, bundled React web viewer, and Kotlin MCP server directly over HTTPS.

---

## 2. Consumption Models

### A. Gradle Plugin (Primary / Recommended)
```kotlin
plugins {
    id("io.compositor") version "0.1.0-alpha01"
}
```
* **Host JVM Execution Only**: Runs strictly on the host desktop JVM during build and preview tasks.
* **Zero APK Impact**: Never enters the Android APK or `.aab` package; does not increase app download size.
* **Automated Environment**: Automatically resolves Android SDK paths, compile classpaths, merged Android resources (`R.string`, `R.drawable`), and variants.

### B. Test Library Dependency (For Screenshot & CI Tests)
```kotlin
dependencies {
    testImplementation("io.compositor:core-renderer:0.1.0-alpha01")
}
```
* **Direct JUnit Integration**: Allows developers to programmatically invoke `LayoutLibRenderer` or `KotlinPsiPreviewScanner` inside host-side JVM unit tests (`testDebugUnitTest`) for automated screenshot diffing without launching the Ktor daemon.

---

## 3. Public Distribution Targets

```
                               ┌──────────────────────────────────────────┐
                               │   Compositor Monorepo (v0.1.0-alpha01)   │
                               └────────────────────┬─────────────────────┘
                                                    │
                 ┌──────────────────────────────────┼──────────────────────────────────┐
                 ▼                                  ▼                                  ▼
      ┌─────────────────────┐            ┌─────────────────────┐            ┌─────────────────────┐
      │     1. JitPack      │            │  2. Plugin Portal   │            │  3. Maven Central   │
      │   (Instant / Zero   │            │ (Native to Gradle,  │            │  (Global standard,  │
      │    Bureaucracy)     │            │  plugins.gradle.org)│            │  Sonatype Central)  │
      └─────────────────────┘            └─────────────────────┘            └─────────────────────┘
```

### Option 1: JitPack (`jitpack.io`) — Fastest (5 Minutes)
JitPack builds artifacts on demand directly from public GitHub tags with zero manual approval.
* **Developer Configuration**:
  ```kotlin
  // settings.gradle.kts
  pluginManagement {
      repositories {
          maven { url = uri("https://jitpack.io") }
          gradlePluginPortal()
          google()
      }
  }
  ```
* **Dependency Coordinates**:
  ```kotlin
  plugins {
      id("com.github.blackzshaik.compositor") version "v0.1.0-alpha01"
  }
  ```

### Option 2: Gradle Plugin Portal (`plugins.gradle.org`) — Standard
The official repository for Gradle plugins. Built into Gradle by default.
* **Developer Configuration**:
  ```kotlin
  // No repository configuration needed
  plugins {
      id("io.compositor") version "0.1.0-alpha01"
  }
  ```
* **Publisher Setup**:
  1. Register an account at [plugins.gradle.org](https://plugins.gradle.org).
  2. Generate an API Key and Secret under your profile.
  3. Add `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` to repository GitHub Secrets.
  4. Run `./gradlew publishPlugins`.

### Option 3: Maven Central via Sonatype Central Portal (`central.sonatype.com`)
The global standard for open-source Java and Kotlin libraries.
* **Artifacts Published**:
  * `io.compositor:core-renderer:0.1.0-alpha01`
  * `io.compositor:plugin:0.1.0-alpha01`
* **Publisher Setup**:
  1. Claim the `io.compositor` or `io.github.blackzshaik` namespace on [central.sonatype.com](https://central.sonatype.com).
  2. Create a User Token and GPG signing key.
  3. Configure the `signing` and publishing plugins in Gradle.

---

## 4. Self-Contained Shaded / Fat-JAR Architecture

To prevent transitive dependency resolution issues where an external project cannot resolve `core-renderer`:
* **Approach**: Bundle `core-renderer` classes and its bundled web resources directly inside the `io.compositor:plugin` artifact.
* **Benefit**: The Android project downloads a single self-contained JAR that has no external repository dependencies beyond standard Gradle and Maven Central repositories.

---

## 5. Publishing Playbook (When Ready)

1. **Local Test Verification**:
   ```bash
   ./gradlew publishToMavenLocal
   ```
2. **Tag Release**:
   ```bash
   git tag -a v0.1.0-alpha01 -m "Compositor v0.1.0-alpha01"
   git push origin v0.1.0-alpha01
   ```
3. **CI Pipeline Automation**:
   The automated GitHub Actions workflow (`.github/workflows/release.yml`) handles assembling, testing, and uploading artifacts.
