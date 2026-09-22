plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compositor)
}

compositor {
    port.set(3001)
    autoOpenBrowser.set(false)
    preferredTheme.set("system")
    variantName.set("debug")
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

tasks.register("exportDebugClasspath") {
    dependsOn("compileDebugKotlin", "processDebugResources")
    val runtimeClasspath = configurations.named("debugRuntimeClasspath")
    val compileClasspath = configurations.named("debugCompileClasspath")
    val outputDir = layout.buildDirectory.dir("compositor/extracted-jars")
    val compileOutputDir = layout.buildDirectory.dir("compositor/compile-jars")
    outputs.dir(outputDir)
    outputs.dir(compileOutputDir)
    doLast {
        val runtimeArtifactView = runtimeClasspath.get().incoming.artifactView {
            attributes {
                attribute(
                    org.gradle.api.attributes.Attribute.of("artifactType", String::class.java),
                    "android-classes-jar"
                )
            }
        }
        val targetDir = outputDir.get().asFile
        targetDir.mkdirs()
        runtimeArtifactView.artifacts.artifactFiles.files.forEachIndexed { index, file ->
            val destName = "${index}_${file.name}"
            file.copyTo(File(targetDir, destName), overwrite = true)
        }
        val rJar = layout.buildDirectory.file(
            "intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"
        ).get().asFile
        if (rJar.exists()) {
            rJar.copyTo(File(targetDir, "R.jar"), overwrite = true)
        }

        val compileArtifactView = compileClasspath.get().incoming.artifactView {
            attributes {
                attribute(
                    org.gradle.api.attributes.Attribute.of("artifactType", String::class.java),
                    "android-classes-jar"
                )
            }
        }
        val compileTargetDir = compileOutputDir.get().asFile
        compileTargetDir.mkdirs()
        compileArtifactView.artifacts.artifactFiles.files.forEachIndexed { index, file ->
            val destName = "${index}_${file.name}"
            file.copyTo(File(compileTargetDir, destName), overwrite = true)
        }
        if (rJar.exists()) {
            rJar.copyTo(File(compileTargetDir, "R.jar"), overwrite = true)
        }
    }
}
