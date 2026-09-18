# Step 02: Multi-Device Bezel Mockups in Compose Multiplatform

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 01  
*Target Module*: `web-viewer`

---

## 1. Objective
Build reusable, hardware-accurate device bezel components in pure Jetpack Compose (`@Composable`) for Google Pixel 8, Samsung Galaxy S24 Ultra, and Android Tablet. Implement canvas zoom, pan, and centering controls using Compose graphics modifiers.

---

## 2. Architectural Design & Responsibilities
* **Device Shell Composables**:
  * Implement `@Composable fun DeviceFrame(...)` using Compose layout primitives (`Box`, `Column`).
  * Style bezels using Compose `Modifier.clip(RoundedCornerShape(...))`, `Modifier.border`, and inner shadow effects.
  * Render hardware cutouts: camera punchhole, speaker bar, and home indicator using Compose shapes.
  * Display the live preview image inside the device screen viewport using Compose Skia image rendering.
* **Device Profiles Enum**:
  * Define `DeviceProfile` enum in Kotlin:
    * `PIXEL_8`: Compact viewport, rounded 36dp corners, centered camera cutout.
    * `GALAXY_S24_ULTRA`: Boxy 12dp corners, edge display styling.
    * `TABLET_10`: 16:10 aspect ratio landscape/portrait tablet bezel.
    * `FRAMELESS`: Minimalist container displaying only the preview image.
* **Canvas Zoom & Pan (`Modifier.graphicsLayer`)**:
  * Apply interactive scaling (`scaleX`, `scaleY`) and translation (`translationX`, `translationY`) using `pointerInput` gestures or keyboard shortcuts (`Ctrl + / -`).
  * Provide standard zoom presets: 50%, 75%, 100%, 125%, and Fit-to-Viewport.

---

## 3. High-Level Integration Guidance
* Render hardware bezels entirely using vector Compose graphics (`Canvas`, `Path`, and `Shape`) so they remain razor-sharp at any zoom level without raster assets.
* Support switching active profiles dynamically from state hoisted at the `App` level.

---

## 4. Verification & Quality Gates
* **Build Verification**: Run `./gradlew :web-viewer:wasmJsBrowserDistribution`; assert clean compilation of device frame composables into the Wasm target.
* **Quality Gate**: Code must pass `./gradlew detekt`.
