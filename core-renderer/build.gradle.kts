plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.paparazzi)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.websockets)
    runtimeOnly("com.android.tools.layoutlib:layoutlib-runtime:14.0.11:$osClassifier")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.ktor.client.content.negotiation)

    detektPlugins(libs.detekt.formatting)
}

val copyWebViewer by tasks.registering(Copy::class) {
    from(rootProject.file("web-viewer/dist"))
    into(layout.buildDirectory.dir("resources/main/web"))
}

tasks.processResources {
    dependsOn(copyWebViewer)
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
