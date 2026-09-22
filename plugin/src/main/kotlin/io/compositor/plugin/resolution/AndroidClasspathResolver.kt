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

        val compiledClassesDirs = resolveCompiledClasses(project, variantName, capVariant)
        val mergedResourceDirs = resolveMergedResources(project, variantName, capVariant)
        val rJar = resolveRJar(project, variantName, capVariant)
        val dependencyClasspath = resolveDependencies(project, variantName)
        val compileClasspath = resolveCompileDependencies(project, variantName)
        val watchRoots = resolveWatchRoots(project, extension, variantName)

        return CompositorProjectContext(
            projectRoot = project.projectDir,
            variantName = variantName,
            compiledClassesDirs = compiledClassesDirs,
            dependencyClasspathFiles = dependencyClasspath,
            compileClasspathFiles = compileClasspath,
            mergedResourceDirs = mergedResourceDirs,
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

        val platformDir = File(sdkDir, "platforms/$platformDirName")
        val androidJar = File(platformDir, "android.jar").takeIf { it.exists() }
        val layoutLibDataDir = File(platformDir, "data").takeIf { it.exists() }

        return Pair(androidJar, layoutLibDataDir)
    }

    private fun resolveCompiledClasses(
        project: Project,
        variantName: String,
        capVariant: String
    ): List<File> {
        val classes = mutableListOf<File>()

        val kotlinDir = project.layout.buildDirectory.dir("tmp/kotlin-classes/$variantName").get().asFile
        if (kotlinDir.exists()) classes.add(kotlinDir)

        val javaDir = project.layout.buildDirectory.dir(
            "intermediates/javac/$variantName/compile${capVariant}JavaWithJavac/classes"
        ).get().asFile
        if (javaDir.exists()) classes.add(javaDir)

        return classes
    }

    private fun resolveMergedResources(
        project: Project,
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
                "intermediates/incremental/$variantName/package${capVariant}Resources/merged.dir"
            ).get().asFile,
            File(project.projectDir, "src/main/res")
        )

        for (candidate in candidates) {
            if (candidate.exists() && !resDirs.contains(candidate)) {
                resDirs.add(candidate)
            }
        }

        return resDirs
    }

    private fun resolveRJar(
        project: Project,
        variantName: String,
        capVariant: String
    ): File? {
        val rJarFile = project.layout.buildDirectory.file(
            "intermediates/compile_and_runtime_not_namespaced_r_class_jar/" +
                "$variantName/process${capVariant}Resources/R.jar"
        ).get().asFile

        return if (rJarFile.exists()) rJarFile else null
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
