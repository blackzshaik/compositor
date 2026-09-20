package io.compositor.pipeline

import io.compositor.daemon.CompositorDaemon
import io.compositor.daemon.DaemonWsMessage
import io.compositor.parser.KotlinPsiPreviewScanner
import io.compositor.parser.PreviewDefinition
import io.compositor.parser.PreviewItem
import io.compositor.parser.PreviewRegistry
import io.compositor.parser.PreviewRenderStatus
import io.compositor.parser.RenderStateUpdate
import io.compositor.renderer.CompositorDeviceConfig
import io.compositor.renderer.LayoutLibPreviewRenderer
import io.compositor.renderer.RenderRequest
import io.compositor.renderer.RenderResult
import io.compositor.watcher.PreviewWatchDispatcher
import io.compositor.watcher.SourceDirectoryWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Configuration for the end-to-end Compositor reactive preview pipeline.
 */
data class PipelineConfig(
    val projectRoot: File = File("."),
    val port: Int = CompositorDaemon.DEFAULT_PORT,
    val watchRoots: List<File> = listOf(File(projectRoot, "src")),
    val classesDirs: List<File> = emptyList(),
    val resourceDirs: List<File> = emptyList(),
    val rJar: File? = null,
    val outputDir: File = File(projectRoot, ".compositor/previews")
)

/**
 * Unified reactive pipeline orchestrating source discovery, coroutine file watching,
 * native LayoutLib in-memory rendering, and WebSocket broadcast streaming.
 */
class CompositorPipeline(
    val config: PipelineConfig,
    val previewRegistry: PreviewRegistry = PreviewRegistry(
        File(config.projectRoot, ".compositor/previews.json")
    ),
    val scanner: KotlinPsiPreviewScanner = KotlinPsiPreviewScanner(),
    val renderer: LayoutLibPreviewRenderer = LayoutLibPreviewRenderer()
) : AutoCloseable {

    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var dispatcher: PreviewWatchDispatcher? = null

    val daemon: CompositorDaemon = CompositorDaemon(
        port = config.port,
        projectRoot = config.projectRoot,
        previewRegistry = previewRegistry,
        renderHandler = { previewId -> renderPreview(previewId) }
    )

    /**
     * Resolves the fully-qualified JVM class name containing the @Composable preview function.
     */
    fun resolveClassName(def: PreviewDefinition): String {
        val pkg = if (def.packageName.isNotBlank()) "${def.packageName}." else ""
        if (!def.enclosingClass.isNullOrBlank()) {
            return "$pkg${def.enclosingClass}"
        }
        val file = File(def.filePath)
        val baseName = file.nameWithoutExtension
        return "$pkg${baseName}Kt"
    }

    /**
     * Renders a specific preview in-memory using LayoutLib and updates the catalog.
     */
    fun renderPreview(previewId: String): PreviewItem? {
        val item = previewRegistry.getCatalog().previews[previewId] ?: return null
        val def = item.definition
        val className = resolveClassName(def)

        val classpathList = mutableListOf<File>()
        classpathList.addAll(config.classesDirs)
        config.rJar?.let { if (it.exists()) classpathList.add(it) }

        val sanitizedId = previewId.replace(':', '_').replace('#', '_').replace('.', '_')
        val outputFile = File(config.outputDir, "$sanitizedId.png")

        val request = RenderRequest(
            composableId = previewId,
            className = className,
            methodName = def.functionName,
            classpath = classpathList,
            resourceDirs = config.resourceDirs,
            deviceConfig = CompositorDeviceConfig.PIXEL_5,
            outputFile = outputFile
        )

        return when (val result = renderer.render(request)) {
            is RenderResult.Success -> {
                previewRegistry.updateRenderStatus(
                    previewId = previewId,
                    update = RenderStateUpdate(
                        status = PreviewRenderStatus.RENDERED,
                        durationMs = result.durationMs,
                        imagePath = result.imageFile.absolutePath,
                        imageUrl = "/api/previews/$previewId/image"
                    )
                )
            }
            is RenderResult.Failure -> {
                val details = if (result.stackTrace.isNotBlank()) {
                    "${result.errorMessage}\n${result.stackTrace}"
                } else {
                    result.errorMessage
                }
                previewRegistry.updateRenderStatus(
                    previewId = previewId,
                    update = RenderStateUpdate(
                        status = PreviewRenderStatus.ERROR,
                        errorDetails = details
                    )
                )
            }
        }
    }

    /**
     * Discovers all previews across target watch roots and registers them.
     */
    fun initialScan(): List<PreviewItem> {
        val allDiscovered = mutableListOf<PreviewDefinition>()
        for (root in config.watchRoots) {
            if (root.exists() && root.isDirectory) {
                allDiscovered.addAll(scanner.scanDirectory(root))
            }
        }

        val registeredItems = mutableListOf<PreviewItem>()
        val byFile = allDiscovered.groupBy { it.filePath }
        for ((filePath, defs) in byFile) {
            val items = previewRegistry.updateFilePreviews(
                filePath = filePath,
                definitions = defs
            )
            registeredItems.addAll(items)
        }
        return registeredItems
    }

    /**
     * Starts the pipeline, registers previews, launches the Ktor daemon,
     * and activates the coroutine source file watcher.
     */
    @Synchronized
    fun start(autoRender: Boolean = true): CompositorPipeline {
        if (isRunning.compareAndSet(false, true)) {
            val items = initialScan()

            daemon.start(wait = false)

            if (autoRender) {
                for (item in items) {
                    renderPreview(item.id)
                }
            }

            val validRoots = config.watchRoots.filter { it.exists() && it.isDirectory }
            if (validRoots.isNotEmpty()) {
                val watcher = SourceDirectoryWatcher(rootDirectories = validRoots)
                val disp = PreviewWatchDispatcher(
                    watcher = watcher,
                    previewRegistry = previewRegistry,
                    scanner = scanner,
                    renderHandler = { previewId -> renderPreview(previewId) }
                )
                disp.onPreviewUpdated = { updatedItem ->
                    val url = updatedItem.imageUrl ?: "/api/previews/${updatedItem.id}/image"
                    scope.launch {
                        daemon.broadcast(DaemonWsMessage.previewUpdated(updatedItem.id, url))
                    }
                }
                disp.start()
                dispatcher = disp
                daemon.watcherActive = true
            }
        }
        return this
    }

    /**
     * Terminates the watcher and stops the daemon.
     */
    @Synchronized
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            dispatcher?.stop()
            dispatcher = null
            daemon.stop()
        }
    }

    override fun close() = stop()

    companion object {
        /**
         * Main CLI entry point for standalone execution of the pure-Kotlin Compositor daemon.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val rootPath = if (args.isNotEmpty()) args[0] else "."
            val port = if (args.size > 1) args[1].toIntOrNull() ?: CompositorDaemon.DEFAULT_PORT else 3001

            val projectRoot = File(rootPath).canonicalFile
            println("Starting Compositor Native Engine for: ${projectRoot.path} on port $port")

            val config = PipelineConfig(
                projectRoot = projectRoot,
                port = port,
                watchRoots = listOf(
                    File(projectRoot, "src/main/java"),
                    File(projectRoot, "src/main/kotlin"),
                    File(projectRoot, "src")
                ).filter { it.exists() }
            )

            val pipeline = CompositorPipeline(config)
            pipeline.start(autoRender = false)

            Runtime.getRuntime().addShutdownHook(Thread {
                println("Shutting down Compositor engine...")
                pipeline.stop()
            })

            println("Compositor daemon running at http://localhost:$port")
        }
    }
}
