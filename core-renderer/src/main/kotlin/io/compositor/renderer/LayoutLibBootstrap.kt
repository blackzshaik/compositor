package io.compositor.renderer

import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile

/**
 * Bootstraps the Android LayoutLib native environment and platform runtime dependencies.
 */
object LayoutLibBootstrap {

    private const val RUNTIME_VERSION = "14.0.11"
    private const val PROPERTY_RUNTIME_ROOT = "paparazzi.layoutlib.runtime.root"
    private const val PROPERTY_RESOURCES_ROOT = "paparazzi.layoutlib.resources.root"

    @Volatile
    private var isInitialized: Boolean = false

    /**
     * Resolves the current operating system identifier for native library loading.
     */
    fun resolvePlatformName(): String {
        val osName = System.getProperty("os.name", "").lowercase()
        return when {
            osName.contains("win") -> "win"
            osName.contains("mac") -> {
                val arch = System.getProperty("os.arch", "").lowercase()
                if (arch == "aarch64" || arch == "arm64") "mac-arm" else "mac"
            }
            else -> "linux"
        }
    }

    /**
     * Returns the target directory where the native LayoutLib runtime is cached.
     */
    fun getRuntimeCacheDir(): File {
        val userHome = System.getProperty("user.home") ?: "."
        return File(userHome, ".compositor/layoutlib/$RUNTIME_VERSION")
    }

    /**
     * Ensures LayoutLib runtime files are unpacked and system properties configured.
     */
    @Synchronized
    fun ensureInitialized(customRuntimeRoot: File? = null) {
        if (isInitialized) return

        val targetDir = customRuntimeRoot ?: resolveRuntimeRoot()
        require(targetDir.exists()) {
            "LayoutLib runtime directory does not exist: ${targetDir.absolutePath}"
        }

        val dataDir = File(targetDir, "data")
        require(dataDir.exists()) {
            "LayoutLib data directory missing at: ${dataDir.absolutePath}"
        }

        System.setProperty(PROPERTY_RUNTIME_ROOT, targetDir.absolutePath)

        val resourcesRoot = resolveResourcesRoot()
        if (resourcesRoot != null) {
            System.setProperty(PROPERTY_RESOURCES_ROOT, resourcesRoot.absolutePath)
        }

        isInitialized = true
    }

    fun resolveResourcesRoot(): File? {
        return resolveExistingResourceDir()
            ?: resolveResourceFromSdk()
            ?: resolveResourceFromCache()
    }

    private fun resolveExistingResourceDir(): File? {
        val existingProperty = System.getProperty(PROPERTY_RESOURCES_ROOT)
        if (!existingProperty.isNullOrBlank()) {
            val dir = File(existingProperty)
            if (isValidResourcesDir(dir)) return dir
        }
        return null
    }

    private fun resolveResourceFromSdk(): File? {
        val sdkDataDir = findAndroidSdkPlatformData()
        if (sdkDataDir != null && isValidResourcesDir(sdkDataDir)) {
            return sdkDataDir
        }
        return null
    }

    private fun resolveResourceFromCache(): File? {
        val cacheDir = getRuntimeCacheDir()
        if (isValidResourcesDir(cacheDir)) return cacheDir
        val cacheData = File(cacheDir, "data")
        if (isValidResourcesDir(cacheData)) return cacheData
        return null
    }

    private fun isValidResourcesDir(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val resDir = File(dir, "res")
        val attrsXml = File(resDir, "values/attrs.xml")
        return resDir.exists() && attrsXml.exists()
    }

    /**
     * Locates the runtime root, unpacking from classpath JAR if necessary.
     */
    private fun resolveRuntimeRoot(): File {
        return resolveExistingPropertyDir()
            ?: resolveCachedDir()
            ?: resolveFromClasspathJar()
            ?: resolveFromAndroidSdk()
            ?: getRuntimeCacheDir()
    }

    private fun resolveExistingPropertyDir(): File? {
        val existingProperty = System.getProperty(PROPERTY_RUNTIME_ROOT)
        if (!existingProperty.isNullOrBlank()) {
            val existingDir = File(existingProperty)
            if (isValidRuntimeDir(existingDir)) {
                return existingDir
            }
        }
        return null
    }

    private fun resolveCachedDir(): File? {
        val cacheDir = getRuntimeCacheDir()
        return if (isValidRuntimeDir(cacheDir)) cacheDir else null
    }

    private fun resolveFromClasspathJar(): File? {
        val runtimeJar = findRuntimeJarOnClasspath()
        if (runtimeJar != null && runtimeJar.exists()) {
            val cacheDir = getRuntimeCacheDir()
            unpackJar(runtimeJar, cacheDir)
            if (isValidRuntimeDir(cacheDir)) {
                return cacheDir
            }
        }
        return null
    }

    private fun resolveFromAndroidSdk(): File? {
        val sdkDataDir = findAndroidSdkPlatformData()
        if (sdkDataDir != null && sdkDataDir.exists()) {
            val sdkPlatformDir = sdkDataDir.parentFile
            if (sdkPlatformDir != null && isValidRuntimeDir(sdkPlatformDir)) {
                return sdkPlatformDir
            }
        }
        return null
    }

    private fun isValidRuntimeDir(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val dataDir = File(dir, "data")
        val buildProp = File(dir, "build.prop")
        val dataBuildProp = File(dataDir, "build.prop")
        return dataDir.exists() && (buildProp.exists() || dataBuildProp.exists())
    }

    /**
     * Searches classpaths for the layoutlib-runtime JAR.
     */
    fun findRuntimeJarOnClasspath(): File? {
        val classPath = System.getProperty("java.class.path", "")
        val entries = classPath.split(File.pathSeparator)
        val candidate = entries.firstOrNull { entry ->
            entry.contains("layoutlib-runtime") && entry.endsWith(".jar")
        }
        if (candidate != null) {
            val file = File(candidate)
            if (file.exists()) return file
        }

        // Search Gradle cache if not directly in java.class.path
        val userHome = System.getProperty("user.home") ?: return null
        val gradleCacheDir = File(userHome, ".gradle/caches/modules-2/files-2.1/com.android.tools.layoutlib")
        if (gradleCacheDir.exists()) {
            val platform = resolvePlatformName()
            val matching = gradleCacheDir.walkTopDown()
                .filter { it.isFile && it.name.contains("layoutlib-runtime") && it.name.contains(platform) }
                .firstOrNull()
            if (matching != null) return matching
        }
        return null
    }

    /**
     * Attempts to locate the Android SDK platform data directory.
     */
    private fun findAndroidSdkPlatformData(): File? {
        val candidates = listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            System.getProperty("android.home"),
            "${System.getProperty("user.home")}/AppData/Local/Android/Sdk"
        )
        for (candidatePath in candidates) {
            val sdkDir = File(candidatePath)
            if (sdkDir.exists()) {
                val platformsDir = File(sdkDir, "platforms")
                if (platformsDir.exists()) {
                    val latestPlatform = platformsDir.listFiles()
                        ?.filter { it.isDirectory && File(it, "data").exists() }
                        ?.maxByOrNull { it.name }
                    if (latestPlatform != null) {
                        return File(latestPlatform, "data")
                    }
                }
            }
        }
        return null
    }

    /**
     * Unpacks a JAR file to a target destination directory.
     */
    fun unpackJar(jarFile: File, destinationDir: File) {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        JarFile(jarFile).use { jar ->
            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val outputFile = File(destinationDir, entry.name)
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    val parent = outputFile.parentFile
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs()
                    }
                    jar.getInputStream(entry).use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}
