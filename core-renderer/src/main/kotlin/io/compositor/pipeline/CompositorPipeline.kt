package io.compositor.pipeline

import io.compositor.compiler.CompilationResult
import io.compositor.compiler.KotlinSourceCompiler
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Configuration for the end-to-end Compositor reactive preview pipeline.
 */
data class PipelineConfig(
    val projectRoot: File = File("."),
    val port: Int = CompositorDaemon.DEFAULT_PORT,
    val watchRoots: List<File> = listOf(File(projectRoot, "src")),
    val classesDirs: List<File> = emptyList(),
    val compileClasspath: List<File> = emptyList(),
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

    private data class CompileCacheEntry(
        val lastModified: Long,
        val length: Long,
        val result: CompilationResult
    )

    private val compileCache = ConcurrentHashMap<String, CompileCacheEntry>()

    private fun isSourceEligible(file: File): Boolean =
        file.exists() && file.isFile && file.extension == "kt"

    private fun isClassUpToDate(def: PreviewDefinition, outDir: File, lastMod: Long): Boolean {
        val className = resolveClassName(def)
        val classRelativePath = className.replace('.', File.separatorChar) + ".class"
        val classFile = File(outDir, classRelativePath)
        return classFile.exists() && classFile.lastModified() >= lastMod
    }

    private fun buildCompileClasspath(): List<File> {
        val cp = mutableListOf<File>()
        cp.addAll(config.classesDirs)
        for (f in config.compileClasspath) {
            if (!cp.contains(f)) {
                cp.add(f)
            }
        }
        val compileJarsDir = File(config.projectRoot, "build/compositor/compile-jars")
        if (compileJarsDir.exists() && compileJarsDir.isDirectory) {
            compileJarsDir.listFiles()?.filter { it.extension == "jar" }?.forEach { f ->
                if (!cp.contains(f)) cp.add(f)
            }
        }
        config.rJar?.let { if (it.exists() && !cp.contains(it)) cp.add(it) }
        return cp
    }

    private fun compileSourceIfNeeded(
        sourceFile: File,
        def: PreviewDefinition? = null
    ): CompilationResult? {
        if (!isSourceEligible(sourceFile)) return null
        val lastMod = sourceFile.lastModified()
        val fileLength = sourceFile.length()
        val cached = compileCache[sourceFile.canonicalPath]
        if (cached != null && cached.lastModified == lastMod && cached.length == fileLength) {
            return cached.result
        }

        val outDir = config.classesDirs.firstOrNull { it.isDirectory }
            ?: File(config.projectRoot, "build/tmp/kotlin-classes/debug")

        if (def != null && isClassUpToDate(def, outDir, lastMod)) {
            val result = CompilationResult(isSuccess = true)
            compileCache[sourceFile.canonicalPath] = CompileCacheEntry(lastMod, fileLength, result)
            return result
        }

        val cp = buildCompileClasspath()
        val result = KotlinSourceCompiler.compile(
            sourceFile = sourceFile,
            outputDir = outDir,
            classpath = cp
        )
        compileCache[sourceFile.canonicalPath] = CompileCacheEntry(lastMod, fileLength, result)
        return result
    }

    /**
     * Renders a specific preview in-memory using LayoutLib and updates the catalog.
     */
    @Synchronized
    fun renderPreview(previewId: String): PreviewItem? {
        val item = previewRegistry.getCatalog().previews[previewId] ?: return null
        println("[Compositor Pipeline] Starting renderPreview for: $previewId")
        previewRegistry.updateRenderStatus(
            previewId = previewId,
            status = PreviewRenderStatus.RENDERING
        )
        val def = item.definition
        val sourceFile = File(def.filePath)
        val compileResult = compileSourceIfNeeded(sourceFile, def)
        if (compileResult != null && !compileResult.isSuccess) {
            val errorMsg = compileResult.errorMessages.joinToString("\n")
            System.err.println("[Compositor Pipeline] Compilation error for $previewId: $errorMsg")
            return previewRegistry.updateRenderStatus(
                previewId = previewId,
                update = RenderStateUpdate(
                    status = PreviewRenderStatus.ERROR,
                    errorDetails = errorMsg
                )
            )
        }

        val request = buildRenderRequest(previewId, def)

        return try {
            when (val result = renderer.render(request)) {
                is RenderResult.Success -> {
                    previewRegistry.updateRenderStatus(
                        previewId = previewId,
                        update = RenderStateUpdate(
                            status = PreviewRenderStatus.RENDERED,
                            durationMs = result.durationMs,
                            imagePath = result.imageFile.absolutePath,
                            imageUrl = "/api/previews/$previewId/image",
                            rootBounds = result.rootBounds
                        )
                    )
                }
                is RenderResult.Failure -> {
                    val details = if (result.stackTrace.isNotBlank()) {
                        "${result.errorMessage}\n${result.stackTrace}"
                    } else {
                        result.errorMessage
                    }
                    System.err.println("[Compositor Pipeline] Render failure for $previewId: ${result.errorMessage}")
                    previewRegistry.updateRenderStatus(
                        previewId = previewId,
                        update = RenderStateUpdate(
                            status = PreviewRenderStatus.ERROR,
                            errorDetails = details
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            System.err.println("[Compositor Pipeline] Unexpected exception during render for $previewId: ${t.message}")
            previewRegistry.updateRenderStatus(
                previewId = previewId,
                update = RenderStateUpdate(
                    status = PreviewRenderStatus.ERROR,
                    errorDetails = t.message ?: "Unexpected rendering error"
                )
            )
        }
    }

    private fun buildRenderRequest(previewId: String, def: PreviewDefinition): RenderRequest {
        val className = resolveClassName(def)
        val classpathList = mutableListOf<File>()
        classpathList.addAll(config.classesDirs)
        config.rJar?.let { if (it.exists()) classpathList.add(it) }

        val sanitizedId = previewId.replace(':', '_').replace('#', '_').replace('.', '_')
        val outputFile = File(config.outputDir, "$sanitizedId.png")

        return RenderRequest(
            composableId = previewId,
            className = className,
            methodName = def.functionName,
            classpath = classpathList,
            resourceDirs = config.resourceDirs,
            deviceConfig = CompositorDeviceConfig.PIXEL_5,
            outputFile = outputFile
        )
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
    @Suppress("TooGenericExceptionCaught")
    @Synchronized
    fun start(autoRender: Boolean = true): CompositorPipeline {
        if (isRunning.compareAndSet(false, true)) {
            val items = initialScan()

            daemon.start(wait = false)

            if (autoRender) {
                for (item in items) {
                    try {
                        renderPreview(item.id)
                    } catch (e: Exception) {
                        System.err.println("Compositor: Initial render failed for ${item.id}: ${e.message}")
                    }
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
                disp.onRenderStarted = { previewId ->
                    println("[Compositor Pipeline] Broadcasting PREVIEW_RENDER_STARTED for: $previewId")
                    daemon.broadcast(DaemonWsMessage.previewRenderStarted(previewId))
                }
                disp.onSourceChanged = { file ->
                    println("[Compositor Pipeline] Source changed: ${file.name}, checking compilation...")
                    compileSourceIfNeeded(file)
                }
                disp.onPreviewUpdated = { updatedItem ->
                    if (updatedItem.status == PreviewRenderStatus.ERROR) {
                        val errorMsg = updatedItem.errorDetails ?: "Render failed"
                        println("[Compositor Pipeline] Broadcasting RENDER_ERROR for: ${updatedItem.id}")
                        daemon.broadcast(DaemonWsMessage.renderError(updatedItem.id, errorMsg))
                    } else {
                        val url = updatedItem.imageUrl ?: "/api/previews/${updatedItem.id}/image"
                        println("[Compositor Pipeline] Broadcasting PREVIEW_UPDATED for: ${updatedItem.id} (image: $url)")
                        daemon.broadcast(
                            DaemonWsMessage.previewUpdated(updatedItem.id, url, preview = updatedItem)
                        )
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
