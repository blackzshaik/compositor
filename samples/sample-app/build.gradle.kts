plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.paparazzi)
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

androidComponents {
    beforeVariants { variantBuilder ->
        variantBuilder.androidTest.enable = false
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

tasks.register<Copy>("exportLatestPreview") {
    dependsOn("recordPaparazziDebug")
    from("src/test/snapshots/images")
    include("**/*.png")
    into(rootProject.layout.projectDirectory.dir(".compositor"))
    rename { "latest_preview.png" }
}
