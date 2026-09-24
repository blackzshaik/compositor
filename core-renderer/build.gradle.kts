plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "io.compositor"
version = "0.1.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
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
    implementation(libs.kotlin.compose.compiler.plugin.embeddable)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.paparazzi)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.sse)
    implementation(libs.kotlin.mcp.sdk)
    runtimeOnly("com.android.tools.layoutlib:layoutlib-runtime:14.0.11:$osClassifier")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.ktor.client.content.negotiation)

    detektPlugins(libs.detekt.formatting)
}

val webViewerMode = providers.gradleProperty("compositor.webViewer")
    .getOrElse(
        if (providers.gradleProperty("compositor.experimental.cmp").map { it.toBoolean() }.getOrElse(false)) "cmp" else "react"
    )

val buildWebViewerReact by tasks.registering(Exec::class) {
    workingDir = rootProject.file("web-viewer-react")
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    if (isWindows) {
        commandLine("cmd", "/c", "npm", "run", "build")
    } else {
        commandLine("npm", "run", "build")
    }
    inputs.dir(rootProject.file("web-viewer-react/src"))
    inputs.file(rootProject.file("web-viewer-react/package.json"))
    outputs.dir(rootProject.file("web-viewer-react/dist"))
}

val copyWebViewer by tasks.registering(Copy::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    when (webViewerMode) {
        "cmp" -> {
            dependsOn(":web-viewer:wasmJsBrowserDistribution")
            from(rootProject.file("web-viewer/build/dist/wasmJs/productionExecutable"))
            into(layout.buildDirectory.dir("generated/resources/web"))
        }
        "both" -> {
            dependsOn(buildWebViewerReact)
            dependsOn(":web-viewer:wasmJsBrowserDistribution")
            from(rootProject.file("web-viewer-react/dist"))
            into(layout.buildDirectory.dir("generated/resources/web"))
            from(rootProject.file("web-viewer/build/dist/wasmJs/productionExecutable")) {
                into("cmp")
            }
        }
        else -> {
            // Default: "react" (ultra-fast, native web experience)
            dependsOn(buildWebViewerReact)
            from(rootProject.file("web-viewer-react/dist"))
            into(layout.buildDirectory.dir("generated/resources/web"))
        }
    }
}

sourceSets["main"].resources.srcDir(layout.buildDirectory.dir("generated/resources"))

tasks.processResources {
    dependsOn(copyWebViewer)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
        "compositor.sample.compile.dir",
        rootProject.file("samples/sample-app/build/compositor/compile-jars").absolutePath
    )
    systemProperty(
        "compositor.sample.android.jar",
        file("$androidSdkDir/platforms/android-35/android.jar").absolutePath
    )
    systemProperty(
        "paparazzi.layoutlib.resources.root",
        file("$androidSdkDir/platforms/android-35/data").absolutePath
    )
}
