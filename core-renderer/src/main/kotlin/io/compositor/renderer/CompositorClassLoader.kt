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

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // System and framework classes MUST delegate to parent
        if (isParentDelegated(name)) {
            return super.loadClass(name, resolve)
        }

        // For project classes, attempt loading from local URLs first
        synchronized(getClassLoadingLock(name)) {
            var c = findLoadedClass(name)
            if (c == null) {
                try {
                    c = findClass(name)
                } catch (_: ClassNotFoundException) {
                    c = super.loadClass(name, resolve)
                }
            }
            if (resolve) {
                resolveClass(c)
            }
            return c
        }
    }

    private fun isParentDelegated(name: String): Boolean {
        for (prefix in PARENT_DELEGATED_PREFIXES) {
            if (name.startsWith(prefix)) return true
        }
        return false
    }

    companion object {
        private val PARENT_DELEGATED_PREFIXES = listOf(
            "java.",
            "javax.",
            "kotlin.",
            "android.",
            "androidx.",
            "com.android.tools.",
            "com.android.ide.",
            "com.android.layoutlib.",
            "com.android.internal.",
            "io.compositor.renderer.",
            "io.compositor.daemon.",
            "io.compositor.parser.",
            "io.compositor.watcher.",
            "io.compositor.pipeline.",
            "io.compositor.compiler.",
            "app.cash.paparazzi"
        )

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
