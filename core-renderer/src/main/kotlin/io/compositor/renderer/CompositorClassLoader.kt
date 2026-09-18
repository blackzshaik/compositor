package io.compositor.renderer

import java.io.File
import java.net.URLClassLoader

/**
 * Isolated classloader for loading user application classes and dependencies.
 */
class CompositorClassLoader(
    classpath: List<File>,
    parent: ClassLoader = Thread.currentThread().contextClassLoader ?: CompositorClassLoader::class.java.classLoader
) : URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), parent) {

    companion object {
        /**
         * Factory function to create a new isolated classloader.
         */
        fun create(
            classpath: List<File>,
            parent: ClassLoader = Thread.currentThread().contextClassLoader
                ?: CompositorClassLoader::class.java.classLoader
        ): CompositorClassLoader = CompositorClassLoader(classpath, parent)
    }
}
