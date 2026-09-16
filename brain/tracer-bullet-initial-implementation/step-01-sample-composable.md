# Step 01: Scaffold Minimal Android Compose App with GreetingPreview

*Status*: **Complete**  
*Assigned Agent*: Any AI Agent invoking "Build from brain"  
*Estimated Duration*: 3 minutes

---

## 1. Objective

Create a clean, minimal reference Android Jetpack Compose application under `samples/sample-app` containing a standard `@Preview` Composable (`GreetingPreview`). This app serves as the input artifact for our headless JVM renderer.

---

## 2. Target Files & Structure

```
samples/sample-app/
├── src/
│   └── main/
│       ├── AndroidManifest.xml
│       └── java/com/compositor/sample/
│           └── Greeting.kt
└── build.gradle.kts
```

---

## 3. Agent Action Prompt (Step-by-Step Instructions)

### 3.1. Configure Project Settings & SDK Path
1. Ensure `local.properties` exists in the repository root with the correct Android SDK path:
   ```properties
   sdk.dir=C:/Users/jahab/AppData/Local/Android/Sdk
   ```
2. Update [`settings.gradle.kts`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/settings.gradle.kts) to include the new sample module:
   ```kotlin
   include(":samples:sample-app")
   ```

### 3.2. Add Android & Compose Plugins to `gradle/libs.versions.toml`
Add AGP and Compose dependencies to [`gradle/libs.versions.toml`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/gradle/libs.versions.toml):
```toml
[versions]
agp = "8.8.2"
composeBom = "2025.02.00"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-material3 = { module = "androidx.compose.material3:material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### 3.3. Create `samples/sample-app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.jvm) apply false
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.compositor.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.compositor.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    debugImplementation(libs.compose.ui.tooling)
}
```

### 3.4. Create `samples/sample-app/src/main/AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="Compositor Sample"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar" />
</manifest>
```

### 3.5. Create `samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt`
```kotlin
package com.compositor.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF6200EE))
            .padding(24.dp)
    ) {
        Text(
            text = "Hello $name from Compositor!",
            color = Color.White,
            fontSize = 20.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting("Android Developer")
}
```

---

## 4. Automated Verification Gate

Execute the compile check from the repository root:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
.\gradlew.bat :samples:sample-app:compileDebugKotlin
```

### Acceptance Criteria:
* The command completes with `BUILD SUCCESSFUL`.
* Exit code is `0`.
* `samples/sample-app/build/intermediates/javac/debug/` or Kotlin classes are compiled without errors.

---

## 5. Post-Execution Checklist
1. Commit changes: `feat: scaffold sample Compose app with GreetingPreview`.
2. Update [`brain/STATE.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/STATE.md): mark Step 01 complete, advance active step to Step 02.
3. Record outputs in the log below.

---

## 6. Execution Log (To be completed by executing agent)
* **Execution Date**: 2026-09-16
* **Executing Agent**: Gemini 3.8 Flash (High) / Antigravity
* **Verification Command Output**:
  ```
  > Task :samples:sample-app:compileDebugKotlin
  BUILD SUCCESSFUL in 2m 2s
  14 actionable tasks: 14 executed
  ```
* **Artifacts Created**:
  - `local.properties`
  - `gradle.properties`
  - `samples/sample-app/build.gradle.kts`
  - `samples/sample-app/src/main/AndroidManifest.xml`
  - `samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt`
  - `gradle/libs.versions.toml` (updated)
  - `settings.gradle.kts` (updated)
  - `build.gradle.kts` (updated)
