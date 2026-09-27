package io.compositor.plugin.resolution

import com.android.build.gradle.BaseExtension
import io.compositor.plugin.CompositorExtension
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import java.io.File

/**
 * Resolves Android compilation outputs, dependency classpaths, Android SDK platforms,
 * and merged Android resources for a target build variant.
 */
object AndroidClasspathResolver {

    private const val DEFAULT_SDK_VERSION = "android-35"
    private const val ARTIFACT_TYPE_ANDROID_CLASSES_JAR = "android-classes-jar"

    /**
     * Resolves an immutable [CompositorProjectContext] from the project and extension configuration.
     */
    fun resolve(project: Project, extension: CompositorExtension): CompositorProjectContext {
        val variantName = extension.variantName.getOrElse("debug")
        val capVariant = variantName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

        val baseExtension = project.extensions.findByType(BaseExtension::class.java)
        val (androidJar, layoutLibDataDir) = resolveAndroidSdk(baseExtension)

        val packageName = resolvePackageName(project, baseExtension)
        val compileSdkVersion = resolveCompileSdkVersion(baseExtension)
        val compiledClassesDirs = resolveCompiledClasses(project, variantName, capVariant)
        val mergedResourceDirs = resolveMergedResources(project, baseExtension, variantName, capVariant)
        val libraryResourceDirs = resolveLibraryResources(project, variantName)
        val rJar = resolveRJar(project, variantName, capVariant)
        val dependencyClasspath = resolveDependencies(project, variantName)
        val compileClasspath = resolveCompileDependencies(project, variantName)
        val watchRoots = resolveWatchRoots(project, extension, variantName)

        return CompositorProjectContext(
            projectRoot = project.projectDir,
            variantName = variantName,
            packageName = packageName,
            compileSdkVersion = compileSdkVersion,
            compiledClassesDirs = compiledClassesDirs,
            dependencyClasspathFiles = dependencyClasspath,
            compileClasspathFiles = compileClasspath,
            mergedResourceDirs = mergedResourceDirs,
            libraryResourceDirs = libraryResourceDirs,
            rJar = rJar,
            androidJar = androidJar,
            layoutLibDataDir = layoutLibDataDir,
            watchRoots = watchRoots
        )
    }

    private fun resolveAndroidSdk(baseExtension: BaseExtension?): Pair<File?, File?> {
        val sdkDir = baseExtension?.sdkDirectory
            ?: System.getenv("ANDROID_HOME")?.let { File(it) }
            ?: System.getenv("ANDROID_SDK_ROOT")?.let { File(it) }
            ?: File(System.getProperty("user.home"), "AppData/Local/Android/Sdk")

        val rawCompileSdk = baseExtension?.compileSdkVersion ?: DEFAULT_SDK_VERSION
        val platformDirName = if (rawCompileSdk.startsWith("android-")) {
            rawCompileSdk
        } else {
            "android-$rawCompileSdk"
        }

        val platformDir = File(sdkDir, "platforms/$platformDirName").takeIf { it.exists() }
            ?: File(sdkDir, "platforms").listFiles()?.firstOrNull { it.isDirectory && File(it, "android.jar").exists() }

        val androidJar = platformDir?.let { File(it, "android.jar").takeIf { f -> f.exists() } }
        val layoutLibDataDir = platformDir?.let { File(it, "data").takeIf { f -> f.exists() } }

        return Pair(androidJar, layoutLibDataDir)
    }

    private fun resolveCompiledClasses(
        project: Project,
        variantName: String,
        capVariant: String
    ): List<File> {
        val classes = mutableListOf<File>()
        val candidates = listOf(
            project.layout.buildDirectory.dir("tmp/kotlin-classes/$variantName").get().asFile,
            project.layout.buildDirectory.dir(
                "intermediates/built_in_kotlinc/$variantName/compile${capVariant}Kotlin/classes"
            ).get().asFile,
            project.layout.buildDirectory.dir(
                "intermediates/javac/$variantName/compile${capVariant}JavaWithJavac/classes"
            ).get().asFile,
            project.layout.buildDirectory.dir("intermediates/javac/$variantName/classes").get().asFile
        )

        for (candidate in candidates) {
            if (candidate.exists() && !classes.contains(candidate)) {
                classes.add(candidate)
            }
        }

        return classes
    }

    private fun resolvePackageName(project: Project, baseExtension: BaseExtension?): String? {
        val namespace = try {
            baseExtension?.namespace
        } catch (_: Throwable) {
            null
        }
        if (!namespace.isNullOrBlank()) return namespace

        val appId = try {
            baseExtension?.defaultConfig?.applicationId
        } catch (_: Throwable) {
            null
        }
        if (!appId.isNullOrBlank()) return appId

        val manifestFile = File(project.projectDir, "src/main/AndroidManifest.xml")
        if (manifestFile.exists()) {
            val content = manifestFile.readText()
            val match = Regex("""package\s*=\s*["']([^"']+)["']""").find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    private fun resolveCompileSdkVersion(baseExtension: BaseExtension?): Int {
        val raw = baseExtension?.compileSdkVersion ?: return 35
        val digits = raw.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 35
    }

    private fun resolveLibraryResources(project: Project, variantName: String): List<File> {
        val libResources = mutableListOf<File>()
        val configName = "${variantName}RuntimeClasspath"
        val runtimeConfig = project.configurations.findByName(configName)

        if (runtimeConfig != null && runtimeConfig.isCanBeResolved) {
            try {
                val artifactView = runtimeConfig.incoming.artifactView { viewConfig ->
                    viewConfig.lenient(true)
                    viewConfig.attributes { attrs ->
                        attrs.attribute(
                            Attribute.of("artifactType", String::class.java),
                            "android-res"
                        )
                    }
                }
                for (file in artifactView.artifacts.artifactFiles.files) {
                    if (file.exists() && !libResources.contains(file)) {
                        libResources.add(file)
                    }
                }
            } catch (_: Throwable) {
                // Lenient resolution failure ignored
            }
        }

        return libResources
    }

    private fun resolveMergedResources(
        project: Project,
        baseExtension: BaseExtension?,
        variantName: String,
        capVariant: String
    ): List<File> {
        val resDirs = mutableListOf<File>()
        val candidates = listOf(
            project.layout.buildDirectory.dir(
                "intermediates/incremental/$variantName/merge${capVariant}Resources/merged.dir"
            ).get().asFile,
            project.layout.buildDirectory.dir(
                "intermediates/merged_res/$variantName/merge${capVariant}Resources/merged.dir"
            ).get().asFile,
            project.layout.buildDirectory.dir(
                "intermediates/merged_res/$variantName/merge${capVariant}Resources"
            ).get().asFile,
            project.layout.buildDirectory.dir(
                "intermediates/incremental/$variantName/package${capVariant}Resources/merged.dir"
            ).get().asFile,
            project.layout.buildDirectory.dir(
                "intermediates/packaged_res/$variantName/package${capVariant}Resources"
            ).get().asFile,
            File(project.projectDir, "src/main/res"),
            File(project.projectDir, "src/$variantName/res")
        )

        for (candidate in candidates) {
            if (candidate.exists() && !resDirs.contains(candidate)) {
                resDirs.add(candidate)
            }
        }

        try {
            baseExtension?.sourceSets?.forEach { sourceSet ->
                sourceSet.res.srcDirs.forEach { dir ->
                    if (dir.exists() && !resDirs.contains(dir)) {
                        resDirs.add(dir)
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore if sourceSets cannot be queried
        }

        return resDirs
    }

    private fun resolveRJar(
        project: Project,
        variantName: String,
        capVariant: String
    ): File? {
        val candidates = listOf(
            project.layout.buildDirectory.file(
                "intermediates/compile_and_runtime_r_class_jar/$variantName/process${capVariant}Resources/R.jar"
            ).get().asFile,
            project.layout.buildDirectory.file(
                "intermediates/compile_and_runtime_not_namespaced_r_class_jar/" +
                    "$variantName/process${capVariant}Resources/R.jar"
            ).get().asFile
        )

        return candidates.firstOrNull { it.exists() }
    }

    private fun resolveDependencies(project: Project, variantName: String): List<File> {
        val dependencies = mutableListOf<File>()
        val configName = "${variantName}RuntimeClasspath"
        val runtimeConfig = project.configurations.findByName(configName)

        if (runtimeConfig != null && runtimeConfig.isCanBeResolved) {
            val artifactView = runtimeConfig.incoming.artifactView { viewConfig ->
                viewConfig.attributes { attrs ->
                    attrs.attribute(
                        Attribute.of("artifactType", String::class.java),
                        ARTIFACT_TYPE_ANDROID_CLASSES_JAR
                    )
                }
            }
            dependencies.addAll(artifactView.artifacts.artifactFiles.files)

            // Include regular JAR dependencies directly present in the configuration
            val jarFiles = runtimeConfig.files.filter { file ->
                file.extension == "jar" && !dependencies.contains(file)
            }
            dependencies.addAll(jarFiles)
        }

        return dependencies
    }

    private fun resolveCompileDependencies(project: Project, variantName: String): List<File> {
        val dependencies = mutableListOf<File>()
        val configName = "${variantName}CompileClasspath"
        val compileConfig = project.configurations.findByName(configName)

        if (compileConfig != null && compileConfig.isCanBeResolved) {
            val artifactView = compileConfig.incoming.artifactView { viewConfig ->
                viewConfig.attributes { attrs ->
                    attrs.attribute(
                        Attribute.of("artifactType", String::class.java),
                        ARTIFACT_TYPE_ANDROID_CLASSES_JAR
                    )
                }
            }
            dependencies.addAll(artifactView.artifacts.artifactFiles.files)

            val jarFiles = compileConfig.files.filter { file ->
                file.extension == "jar" && !dependencies.contains(file)
            }
            dependencies.addAll(jarFiles)
        }

        return dependencies
    }

    private fun resolveWatchRoots(
        project: Project,
        extension: CompositorExtension,
        variantName: String
    ): List<File> {
        val configuredRoots = extension.watchRoots.getOrElse(emptyList())
        if (configuredRoots.isNotEmpty()) {
            return configuredRoots.filter { it.exists() }
        }

        val candidates = listOf(
            File(project.projectDir, "src/main/java"),
            File(project.projectDir, "src/main/kotlin"),
            File(project.projectDir, "src/$variantName/java"),
            File(project.projectDir, "src/$variantName/kotlin"),
            File(project.projectDir, "src/main/res"),
            File(project.projectDir, "src")
        )

        return candidates.filter { it.exists() }
    }
}


