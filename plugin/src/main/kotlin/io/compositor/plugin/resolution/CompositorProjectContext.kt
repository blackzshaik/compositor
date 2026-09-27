package io.compositor.plugin.resolution

import java.io.File

/**
 * Immutable context containing resolved Android project paths, classpaths,
 * SDK directories, and resources required for LayoutLib preview rendering.
 */
data class CompositorProjectContext(
    val projectRoot: File,
    val variantName: String,
    val packageName: String? = null,
    val compileSdkVersion: Int = 35,
    val compiledClassesDirs: List<File> = emptyList(),
    val dependencyClasspathFiles: List<File> = emptyList(),
    val compileClasspathFiles: List<File> = emptyList(),
    val mergedResourceDirs: List<File> = emptyList(),
    val libraryResourceDirs: List<File> = emptyList(),
    val rJar: File? = null,
    val androidJar: File? = null,
    val layoutLibDataDir: File? = null,
    val watchRoots: List<File> = emptyList()
) {
    /**
     * Resolves the combined classpath required for Compositor ClassLoader.
     */
    fun allClasspathFiles(): List<File> {
        val result = mutableListOf<File>()
        result.addAll(compiledClassesDirs.filter { it.exists() })
        result.addAll(dependencyClasspathFiles.filter { it.exists() })
        rJar?.let { if (it.exists()) result.add(it) }
        androidJar?.let { if (it.exists()) result.add(it) }
        return result
    }
}
