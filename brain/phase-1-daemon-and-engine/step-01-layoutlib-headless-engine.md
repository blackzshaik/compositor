# Step 01: Native LayoutLib In-Memory Headless Engine

*Phase*: 1 — Native LayoutLib Engine & Pure-Kotlin Ktor Daemon  
*Status*: Ready for Implementation  
*Target Module*: `core-renderer`

---

## 1. Objective
Build an in-memory, headless Compose rendering engine inside `core-renderer` that interfaces directly with Android's LayoutLib. This replaces the Paparazzi testing harness, allowing Compositor to render any `@Preview` composable directly from application source code without requiring JUnit test files or test dependencies.

---

## 2. Architectural Design & Responsibilities
* **LayoutLib Native Bootstrap**:
  * Locate Android SDK platforms (e.g. `platforms/android-35/data`) and load the native LayoutLib bridge.
  * Initialize Android framework stubs and system resource tables.
* **Isolated Project Classloader**:
  * Construct a URLClassLoader with the target Android module's compiled `.class` files, Compose runtime JARs, and dependencies.
* **In-Memory Render Session**:
  * Configure session parameters: device screen resolution, pixel density, orientation, and Android theme.
  * Instantiate the target Composable class and invoke the `@Preview` method via reflection within the Compose view hierarchy.
* **Rasterization & Bounds Extraction**:
  * Render the resulting view hierarchy into a Java `BufferedImage`.
  * Traverse view node coordinates to capture bounding boxes for element inspection.
  * Encode and write the bitmap to `.compositor/previews/<preview-id>.png`.

---

## 3. High-Level Integration Guidance
* Implement as a clean Kotlin service class (e.g. `LayoutLibPreviewRenderer`) in `core-renderer`.
* Take execution inputs: fully qualified Composable function name, classpath list, resource directory path, and device configuration.
* Return a structured `RenderResult` (Success with image path, dimensions, and duration, or Failure with captured exception and stack trace).
* Ensure zero dependency on `junit` or testing frameworks in the application module.

---

## 4. Verification & Quality Gates
* **Unit/Integration Test**: Write a verification test in `core-renderer` that passes `com.compositor.sample.GreetingKt.GreetingPreview` to the renderer; assert a valid, non-empty PNG file is generated on disk in under 1.5 seconds.
* **Quality Gate**: Code must pass `./gradlew detekt` and Kover coverage verification.
