package io.compositor.plugin

import io.compositor.plugin.tasks.CompositorRenderTask
import io.compositor.plugin.tasks.CompositorTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Main entry point for the Compositor Gradle Plugin (`io.compositor`).
 *
 * Integrates headless LayoutLib preview rendering and the local web viewer into
 * Android application and library modules.
 */
class CompositorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            CompositorExtension.NAME,
            CompositorExtension::class.java
        )

        project.configurations.maybeCreate(COMPOSITOR_RUNTIME_CONFIG).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        if (project.rootProject.findProject(":core-renderer") != null) {
            project.dependencies.add(
                COMPOSITOR_RUNTIME_CONFIG,
                project.dependencies.project(mapOf("path" to ":core-renderer"))
            )
        } else {
            project.dependencies.add(
                COMPOSITOR_RUNTIME_CONFIG,
                "io.compositor:core-renderer:0.1.0-SNAPSHOT"
            )
        }

        val compositorTask = project.tasks.register(
            CompositorTask.TASK_NAME,
            CompositorTask::class.java
        ) { task ->
            task.port.set(extension.port)
            task.autoOpenBrowser.set(extension.autoOpenBrowser)
            task.variantName.set(extension.variantName)
        }

        val renderTask = project.tasks.register(
            CompositorRenderTask.TASK_NAME,
            CompositorRenderTask::class.java
        ) { task ->
            task.variantName.set(extension.variantName)
        }

        project.afterEvaluate {
            val variantName = extension.variantName.getOrElse("debug")
            val capVariant = variantName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            val compileKotlin = project.tasks.findByName("compile${capVariant}Kotlin")
            val processRes = project.tasks.findByName("process${capVariant}Resources")
            val mergeRes = project.tasks.findByName("merge${capVariant}Resources")

            compositorTask.configure { task ->
                compileKotlin?.let { task.dependsOn(it) }
                processRes?.let { task.dependsOn(it) }
                mergeRes?.let { task.dependsOn(it) }
            }

            renderTask.configure { task ->
                compileKotlin?.let { task.dependsOn(it) }
                processRes?.let { task.dependsOn(it) }
                mergeRes?.let { task.dependsOn(it) }
            }
        }
    }

    companion object {
        const val COMPOSITOR_RUNTIME_CONFIG = "compositorRuntime"
    }
}
