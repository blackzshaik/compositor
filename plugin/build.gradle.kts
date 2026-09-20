plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    `maven-publish`
}

group = "io.compositor"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    plugins {
        create("compositor") {
            id = "io.compositor"
            displayName = "Compositor Gradle Plugin"
            description = "Headless Jetpack Compose preview engine and local web viewer Gradle plugin"
            implementationClass = "io.compositor.plugin.CompositorPlugin"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.android.gradle.plugin)
    implementation(project(":core-renderer"))

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    detektPlugins(libs.detekt.formatting)
}

tasks.test {
    useJUnitPlatform()
}
