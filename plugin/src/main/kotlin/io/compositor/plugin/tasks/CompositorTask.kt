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
 * Gradle task that starts the interactive Compositor preview daemon,
 * launches the web viewer in the default browser, and streams live updates.
 */
abstract class CompositorTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val autoOpenBrowser: Property<Boolean>

    @get:Input
    abstract val variantName: Property<String>

    init {
        group = TASK_GROUP
        description = "Starts the interactive Compositor preview daemon and launches the browser viewer."
    }

    @TaskAction
    fun run() {
        val extension = project.extensions.getByType(CompositorExtension::class.java)
        val context = AndroidClasspathResolver.resolve(project, extension)

        val compositorDir = File(context.projectRoot, ".compositor").apply { mkdirs() }
        val previewsDir = File(compositorDir, "previews").apply { mkdirs() }
        val configFile = File(compositorDir, "daemon-config.json")

        val rootEscaped = escape(context.projectRoot.absolutePath)
        val watchEscaped = context.watchRoots.joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val classesEscaped = context.allClasspathFiles().joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val compileCp = mutableListOf<File>()
        compileCp.addAll(context.compiledClassesDirs.filter { it.exists() })
        compileCp.addAll(context.compileClasspathFiles.filter { it.exists() })
        compileCp.addAll(context.dependencyClasspathFiles.filter { it.exists() })
        context.rJar?.let { if (it.exists()) compileCp.add(it) }
        context.androidJar?.let { if (it.exists()) compileCp.add(it) }
        val compileEscaped = compileCp.joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val resEscaped = context.mergedResourceDirs.joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val rJarEscaped = context.rJar?.let { "\"${escape(it.absolutePath)}\"" } ?: "null"
        val layoutLibEscaped = context.layoutLibDataDir?.let { "\"${escape(it.absolutePath)}\"" } ?: "null"
        val libraryResEscaped = context.libraryResourceDirs.joinToString(",") { "\"${escape(it.absolutePath)}\"" }
        val pkgEscaped = context.packageName?.let { "\"${escape(it)}\"" } ?: "null"
        val outEscaped = escape(previewsDir.absolutePath)

        configFile.writeText(
            """
            {
              "mode": "daemon",
              "projectRoot": "$rootEscaped",
              "port": ${port.get()},
              "packageName": $pkgEscaped,
              "compileSdkVersion": ${context.compileSdkVersion},
              "watchRoots": [$watchEscaped],
              "classesDirs": [$classesEscaped],
              "compileClasspath": [$compileEscaped],
              "resourceDirs": [$resEscaped],
              "libraryResourceDirs": [$libraryResEscaped],
              "rJar": $rJarEscaped,
              "layoutLibDataDir": $layoutLibEscaped,
              "outputDir": "$outEscaped",
              "autoOpenBrowser": ${autoOpenBrowser.get()}
            }
            """.trimIndent()
        )

        val runtimeClasspath = project.configurations.getByName(CompositorPlugin.COMPOSITOR_RUNTIME_CONFIG)
        val fullClasspath = runtimeClasspath + project.files(context.allClasspathFiles())

        execOperations.javaexec { spec ->
            spec.mainClass.set("io.compositor.pipeline.CompositorCli")
            spec.classpath = fullClasspath
            spec.args = listOf(configFile.absolutePath)
            spec.jvmArgs = listOf("-Xmx2048m")
            spec.standardInput = System.`in`
            context.layoutLibDataDir?.let {
                spec.systemProperty("paparazzi.layoutlib.resources.root", it.absolutePath)
            }
        }
    }

    private fun escape(path: String): String = path.replace("\\", "\\\\")

    companion object {
        const val TASK_NAME = "compositor"
        const val TASK_GROUP = "compositor"
    }
}
