package io.compositor.plugin.tasks

import io.compositor.plugin.CompositorExtension
import io.compositor.plugin.CompositorPlugin
import io.compositor.plugin.resolution.AndroidClasspathResolver
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Gradle task that performs headless batch rendering of all discovered
 * `@Preview` composables into `.compositor/previews/` in an isolated JVM process.
 */
abstract class CompositorRenderTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val variantName: Property<String>

    init {
        group = TASK_GROUP
        description = "Discovers all Compose previews in the module and renders them in headless batch mode."
    }

    @TaskAction
    fun run() {
        val extension = project.extensions.getByType(CompositorExtension::class.java)
        val context = AndroidClasspathResolver.resolve(project, extension)

        val compositorDir = File(context.projectRoot, ".compositor").apply { mkdirs() }
        val previewsDir = File(compositorDir, "previews").apply { mkdirs() }
        val configFile = File(compositorDir, "render-config.json")

        val rootEscaped = escape(context.projectRoot.absolutePath)
        val watchEscaped = context.watchRoots.joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val classesEscaped = context.allClasspathFiles().joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val resEscaped = context.mergedResourceDirs.joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val rJarEscaped = context.rJar?.let { "\"${escape(it.absolutePath)}\"" } ?: "null"
        val layoutLibEscaped = context.layoutLibDataDir?.let { "\"${escape(it.absolutePath)}\"" } ?: "null"
        val libraryResEscaped = context.libraryResourceDirs.joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val pkgEscaped = context.packageName?.let { "\"${escape(it)}\"" } ?: "null"
        val outEscaped = escape(previewsDir.absolutePath)

        configFile.writeText(
            """
            {
              "mode": "render",
              "projectRoot": "$rootEscaped",
              "port": ${CompositorExtension.DEFAULT_PORT},
              "packageName": $pkgEscaped,
              "compileSdkVersion": ${context.compileSdkVersion},
              "watchRoots": [$watchEscaped],
              "classesDirs": [$classesEscaped],
              "resourceDirs": [$resEscaped],
              "libraryResourceDirs": [$libraryResEscaped],
              "rJar": $rJarEscaped,
              "layoutLibDataDir": $layoutLibEscaped,
              "outputDir": "$outEscaped",
              "autoOpenBrowser": false
            }
            """.trimIndent()
        )

        val runtimeClasspath = project.configurations.getByName(CompositorPlugin.COMPOSITOR_RUNTIME_CONFIG)
        val fullClasspath = runtimeClasspath + project.files(context.allClasspathFiles())

        val result = execOperations.javaexec { spec ->
            spec.mainClass.set("io.compositor.pipeline.CompositorCli")
            spec.classpath = fullClasspath
            spec.args = listOf(configFile.absolutePath)
            spec.jvmArgs = listOf("-Xmx2048m")
            context.layoutLibDataDir?.let {
                spec.systemProperty("paparazzi.layoutlib.resources.root", it.absolutePath)
            }
        }

        result.assertNormalExitValue()
    }

    private fun escape(path: String): String = path.replace("\\", "\\\\")

    companion object {
        const val TASK_NAME = "compositorRender"
        const val TASK_GROUP = "compositor"
    }
}
