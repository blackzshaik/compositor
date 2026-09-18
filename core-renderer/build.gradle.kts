plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val osName = System.getProperty("os.name").lowercase()
val osClassifier = when {
    osName.contains("win") -> "win"
    osName.contains("mac") -> if (System.getProperty("os.arch") == "aarch64") "mac-arm" else "mac"
    else -> "linux"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.paparazzi)
    runtimeOnly("com.android.tools.layoutlib:layoutlib-runtime:14.0.11:$osClassifier")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)

    detektPlugins(libs.detekt.formatting)
}

tasks.test {
    useJUnitPlatform()
    dependsOn(
        ":samples:sample-app:compileDebugKotlin",
        ":samples:sample-app:processDebugResources",
        ":samples:sample-app:exportDebugClasspath"
    )
    val androidSdkDir = System.getenv("ANDROID_HOME")
        ?: "${System.getProperty("user.home")}/AppData/Local/Android/Sdk"
    classpath += files("$androidSdkDir/platforms/android-35/android.jar")
    classpath += fileTree(rootProject.file("samples/sample-app/build/compositor/extracted-jars"))
    classpath += files(rootProject.file("samples/sample-app/build/tmp/kotlin-classes/debug"))
    systemProperty(
        "compositor.sample.classes.dir",
        rootProject.file("samples/sample-app/build/tmp/kotlin-classes/debug").absolutePath
    )
    systemProperty(
        "compositor.sample.resources.dir",
        rootProject.file(
            "samples/sample-app/build/intermediates/merged_res/debug/mergeDebugResources/merged.dir"
        ).absolutePath
    )
    systemProperty(
        "compositor.sample.rjar",
        rootProject.file(
            "samples/sample-app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"
        ).absolutePath
    )
    systemProperty(
        "paparazzi.layoutlib.resources.root",
        file("$androidSdkDir/platforms/android-35/data").absolutePath
    )
}
