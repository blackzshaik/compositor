package io.compositor.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File
import javax.inject.Inject

/**
 * User configuration extension for the Compositor Gradle Plugin.
 *
 * Example usage in build.gradle.kts:
 * ```kotlin
 * compositor {
 *     port.set(3001)
 *     autoOpenBrowser.set(true)
 *     preferredTheme.set("system")
 *     variantName.set("debug")
 * }
 * ```
 */
abstract class CompositorExtension @Inject constructor(objects: ObjectFactory) {

    /**
     * Local port for the embedded Ktor preview daemon and web server. Defaults to 3001.
     */
    val port: Property<Int> = objects.property(Int::class.java).convention(DEFAULT_PORT)

    /**
     * Whether to automatically open the default web browser upon launching `./gradlew compositor`.
     */
    val autoOpenBrowser: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * Default UI theme ("light", "dark", or "system").
     */
    val preferredTheme: Property<String> = objects.property(String::class.java).convention("system")

    /**
     * Target build variant to extract classpath and merged resources from (e.g. "debug").
     */
    val variantName: Property<String> = objects.property(String::class.java).convention("debug")

    /**
     * Optional custom directories to watch for source code changes. Defaults to variant source directories.
     */
    val watchRoots: ListProperty<File> = objects.listProperty(File::class.java)

    companion object {
        const val NAME = "compositor"
        const val DEFAULT_PORT = 3001
    }
}
