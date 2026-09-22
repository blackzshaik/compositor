plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "web-viewer"
        browser {
            commonWebpackConfig {
                outputFileName = "web-viewer.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-wasm-js:1.10.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-wasm-js:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-wasm-js:1.7.3")
                implementation("io.ktor:ktor-client-core-wasm-js:3.0.3")
                implementation("io.ktor:ktor-client-websockets-wasm-js:3.0.3")
                implementation("io.ktor:ktor-client-content-negotiation-wasm-js:3.0.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json-wasm-js:3.0.3")
            }
        }
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

detekt {
    source.setFrom("src/wasmJsMain/kotlin")
}

configurations.configureEach {
    if (!name.startsWith("detekt")) {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name == "kotlinx-serialization-core-jvm") {
                useTarget("org.jetbrains.kotlinx:kotlinx-serialization-core-wasm-js:${requested.version}")
            }
        }
    }
}

