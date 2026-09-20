package io.compositor.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CompositorPluginFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin applies successfully and registers extension and tasks`() {
        val settingsFile = File(testProjectDir, "settings.gradle.kts")
        settingsFile.writeText("rootProject.name = \"test-app\"\n")

        val buildFile = File(testProjectDir, "build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                id("io.compositor")
            }

            compositor {
                port.set(4000)
                autoOpenBrowser.set(false)
                preferredTheme.set("dark")
                variantName.set("debug")
            }

            tasks.register("verifyCompositorConfig") {
                val ext = project.extensions.getByType(io.compositor.plugin.CompositorExtension::class.java)
                doLast {
                    println("COMPOSITOR_PORT=" + ext.port.get())
                    println("COMPOSITOR_AUTO_BROWSER=" + ext.autoOpenBrowser.get())
                    println("COMPOSITOR_THEME=" + ext.preferredTheme.get())
                    println("COMPOSITOR_VARIANT=" + ext.variantName.get())
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("verifyCompositorConfig", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":verifyCompositorConfig")?.outcome)
        assertTrue(result.output.contains("COMPOSITOR_PORT=4000"))
        assertTrue(result.output.contains("COMPOSITOR_AUTO_BROWSER=false"))
        assertTrue(result.output.contains("COMPOSITOR_THEME=dark"))
        assertTrue(result.output.contains("COMPOSITOR_VARIANT=debug"))
    }

    @Test
    fun `plugin registers compositor and compositorRender tasks`() {
        val settingsFile = File(testProjectDir, "settings.gradle.kts")
        settingsFile.writeText("rootProject.name = \"test-tasks\"\n")

        val buildFile = File(testProjectDir, "build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                id("io.compositor")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--group=compositor")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("compositor - Starts the interactive Compositor preview daemon"))
        assertTrue(result.output.contains("compositorRender - Discovers all Compose previews in the module"))
    }
}
