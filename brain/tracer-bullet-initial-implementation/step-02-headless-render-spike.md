# Step 02: Headless LayoutLib JVM Render Spike

*Status*: **Ready for Execution** (Blocked by Step 01)  
*Assigned Agent*: Any AI Agent invoking "Build from brain"  
*Estimated Duration*: 4 minutes

---

## 1. Objective

Execute `@Preview fun GreetingPreview()` on the host machine's Java Virtual Machine (JVM) without launching an Android emulator or Android Studio. Generate a pixel-accurate PNG snapshot (`latest_preview.png`) using the battle-tested **LayoutLib engine via Paparazzi**.

---

## 2. Target Files & Structure

```
samples/sample-app/
├── src/test/java/com/compositor/sample/
│   └── GreetingPreviewSnapshotTest.kt
└── build.gradle.kts (update to apply Paparazzi plugin & preview export task)
```

---

## 3. Agent Action Prompt (Step-by-Step Instructions)

### 3.1. Add Paparazzi Plugin to Version Catalog & Sample App
1. In [`gradle/libs.versions.toml`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/gradle/libs.versions.toml), add the Paparazzi plugin:
   ```toml
   [versions]
   paparazzi = "1.3.5"

   [plugins]
   paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
   ```
2. In `samples/sample-app/build.gradle.kts`, apply the plugin:
   ```kotlin
   plugins {
       alias(libs.plugins.android.application)
       id("org.jetbrains.kotlin.android")
       alias(libs.plugins.compose.compiler)
       alias(libs.plugins.paparazzi)
   }
   ```
3. In `samples/sample-app/build.gradle.kts`, add an export task to copy the generated snapshot to `.compositor/latest_preview.png`:
   ```kotlin
   tasks.register<Copy>("exportLatestPreview") {
       dependsOn("recordPaparazziDebug")
       from("src/test/snapshots/images")
       include("**/*.png")
       into(rootProject.layout.projectDirectory.dir(".compositor"))
       rename { "latest_preview.png" }
   }
   ```

### 3.2. Create Snapshot Test Harness
Create `samples/sample-app/src/test/java/com/compositor/sample/GreetingPreviewSnapshotTest.kt`:
```kotlin
package com.compositor.sample

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class GreetingPreviewSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun renderGreetingPreview() {
        paparazzi.snapshot {
            GreetingPreview()
        }
    }
}
```

---

## 4. Automated Verification Gate

Execute the headless preview render from the repository root:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
.\gradlew.bat :samples:sample-app:exportLatestPreview
```

### Acceptance Criteria:
* The command completes with `BUILD SUCCESSFUL`.
* The file `.compositor/latest_preview.png` exists in the repository root.
* The image file size is greater than 5,000 bytes (verifying non-empty image rasterization).
* The command runs completely on the host JVM without requiring `adb`, an emulator, or Android Studio.

---

## 5. Post-Execution Checklist
1. Commit changes: `feat: implement headless LayoutLib preview rendering spike`.
2. Update [`brain/STATE.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/STATE.md): mark Step 02 complete, advance active step to Step 03.
3. Record outputs in the log below.

---

## 6. Execution Log (To be completed by executing agent)
* **Execution Date**: _[Pending Execution]_
* **Executing Agent**: _[Agent Name/Model]_
* **Render Duration**: _[Time taken in seconds]_
* **Output Image Size**: _[File size in bytes]_
* **Artifact Path**: `.compositor/latest_preview.png`
