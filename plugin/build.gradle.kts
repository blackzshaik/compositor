plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    `maven-publish`
}

group = "io.compositor"
version = "0.1.0-alpha01"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    website.set("https://github.com/compositor-org/compositor")
    vcsUrl.set("https://github.com/compositor-org/compositor.git")
    plugins {
        create("compositor") {
            id = "io.compositor"
            displayName = "Compositor Gradle Plugin"
            description = "Headless Jetpack Compose preview engine and local web viewer Gradle plugin"
            tags.set(listOf("compose", "jetpack-compose", "preview", "layoutlib", "mcp"))
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
