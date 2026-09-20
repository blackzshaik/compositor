package io.compositor.pipeline

import io.compositor.parser.PreviewRenderStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

/**
 * Configuration DTO for [CompositorCli].
 */
@Serializable
data class CompositorCliConfig(
    val mode: String = "daemon",
    val projectRoot: String = ".",
    val port: Int = 3001,
    val watchRoots: List<String> = emptyList(),
    val classesDirs: List<String> = emptyList(),
    val resourceDirs: List<String> = emptyList(),
    val rJar: String? = null,
    val layoutLibDataDir: String? = null,
    val outputDir: String? = null,
    val autoOpenBrowser: Boolean = false
) {
    fun toPipelineConfig(): PipelineConfig {
        val root = File(projectRoot)
        return PipelineConfig(
            projectRoot = root,
            port = port,
            watchRoots = watchRoots.map { File(it) },
            classesDirs = classesDirs.map { File(it) },
            resourceDirs = resourceDirs.map { File(it) },
            rJar = rJar?.let { File(it) },
            outputDir = if (outputDir != null) File(outputDir) else File(root, ".compositor/previews")
        )
    }
}

/**
 * Standalone CLI entry point for executing the Compositor engine in a dedicated,
 * uninstrumented JVM process.
 */
object CompositorCli {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            System.err.println("Usage: CompositorCli <path-to-config.json>")
            exitProcess(1)
        }

        val configFile = File(args[0])
        if (!configFile.exists()) {
            System.err.println("Config file not found: ${configFile.absolutePath}")
            exitProcess(1)
        }

        val config = json.decodeFromString<CompositorCliConfig>(configFile.readText())
        config.layoutLibDataDir?.let { dataDir ->
            System.setProperty("paparazzi.layoutlib.resources.root", dataDir)
        }

        val pipelineConfig = config.toPipelineConfig()

        if (config.mode.equals("render", ignoreCase = true)) {
            runBatchRender(pipelineConfig)
        } else {
            runDaemon(pipelineConfig, config.autoOpenBrowser)
        }
    }

    private fun runBatchRender(config: PipelineConfig) {
        println("Compositor: Starting headless batch render for: ${config.projectRoot.path}")
        val pipeline = CompositorPipeline(config)
        try {
            val items = pipeline.initialScan()
            if (items.isEmpty()) {
                println("Compositor: No @Preview composables discovered.")
                return
            }

            println("Compositor: Discovered ${items.size} preview(s). Executing batch render...")
            var successCount = 0
            var failureCount = 0

            for (item in items) {
                val rendered = pipeline.renderPreview(item.id)
                if (rendered?.status == PreviewRenderStatus.RENDERED) {
                    successCount++
                    println("  ✓ Rendered [${item.id}] in ${rendered.durationMs}ms -> ${rendered.imagePath}")
                } else {
                    failureCount++
                    val errorMsg = rendered?.errorDetails ?: "Unknown failure"
                    System.err.println("  ✗ Failed [${item.id}]: $errorMsg")
                }
            }

            println("Compositor: Batch render complete: $successCount succeeded, $failureCount failed.")
            if (failureCount > 0 && successCount == 0) {
                exitProcess(1)
            }
        } finally {
            pipeline.stop()
        }
    }

    private fun runDaemon(config: PipelineConfig, autoOpenBrowser: Boolean) {
        println("Compositor: Starting preview daemon for: ${config.projectRoot.path} on port ${config.port}")
        val pipeline = CompositorPipeline(config)
        val latch = CountDownLatch(1)

        Runtime.getRuntime().addShutdownHook(Thread {
            println("\nCompositor: Shutting down daemon...")
            pipeline.stop()
            latch.countDown()
        })

        pipeline.start(autoRender = true)
        val catalog = pipeline.previewRegistry.getCatalog()
        println("Compositor: Discovered ${catalog.totalCount} preview(s).")
        println("Compositor daemon running at: http://localhost:${config.port}")

        if (autoOpenBrowser) {
            openBrowser("http://localhost:${config.port}")
        }

        println("Compositor: Press Ctrl+C in this terminal to stop.")
        latch.await()
    }

    private fun openBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return
            }
        } catch (_: java.io.IOException) {
            // Fall back to OS process
        } catch (_: SecurityException) {
            // Fall back to OS process
        } catch (_: UnsupportedOperationException) {
            // Fall back to OS process
        }

        try {
            val os = System.getProperty("os.name", "").lowercase()
            when {
                os.contains("win") -> ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start()
                os.contains("mac") -> ProcessBuilder("open", url).start()
                else -> ProcessBuilder("xdg-open", url).start()
            }
        } catch (e: java.io.IOException) {
            System.err.println("Compositor: Unable to automatically open browser: ${e.message}")
        } catch (e: SecurityException) {
            System.err.println("Compositor: Security restriction opening browser: ${e.message}")
        }
    }
}
