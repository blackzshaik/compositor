package io.compositor.watcher

import io.compositor.parser.KotlinPsiPreviewScanner
import io.compositor.parser.PreviewItem
import io.compositor.parser.PreviewRegistry
import io.compositor.parser.PreviewRenderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates debounced file change events with AST parsing,
 * catalog indexing, and incremental preview rendering dispatch.
 */
class PreviewWatchDispatcher(
    val watcher: SourceDirectoryWatcher,
    val previewRegistry: PreviewRegistry,
    val scanner: KotlinPsiPreviewScanner = KotlinPsiPreviewScanner(),
    val moduleName: String = DEFAULT_MODULE_NAME,
    val renderHandler: (suspend (String) -> PreviewItem?)? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : AutoCloseable {

    private val isRunning = AtomicBoolean(false)
    private var dispatchJob: Job? = null

    /**
     * Optional listener invoked when preview rendering is initiated.
     */
    var onRenderStarted: (suspend (String) -> Unit)? = null

    /**
     * Optional listener invoked when a source file changes before parsing/rendering.
     */
    var onSourceChanged: ((File) -> Unit)? = null

    /**
     * Optional listener invoked when an affected preview has been parsed or re-rendered.
     */
    var onPreviewUpdated: (suspend (PreviewItem) -> Unit)? = null

    /**
     * Starts listening to debounced file system events.
     */
    @Synchronized
    fun start(): Job {
        if (isRunning.compareAndSet(false, true)) {
            watcher.start()
            dispatchJob = coroutineScope.launch {
                watcher.debouncedEvents.collect { event ->
                    try {
                        println("[Compositor Watcher] Dispatching event: ${event.kind} for ${event.file.name}")
                        handleFileChange(event)
                    } catch (t: Throwable) {
                        System.err.println("[Compositor Watcher] Error handling file change for ${event.file.name}: ${t.message}")
                        t.printStackTrace()
                    }
                }
            }
        }
        return dispatchJob ?: Job().apply { complete() }
    }

    /**
     * Processes a single file change event: re-scans Kotlin source or removes deleted files.
     */
    suspend fun handleFileChange(event: FileChangeEvent): List<PreviewItem> {
        val extension = event.file.extension.lowercase()
        return when {
            extension == "kt" && event.kind == ChangeKind.DELETED -> {
                println("[Compositor Watcher] Removing deleted file previews: ${event.file.name}")
                previewRegistry.removeFile(event.file.invariantSeparatorsPath)
                emptyList()
            }
            extension == "kt" -> {
                processKotlinFileChange(event)
            }
            extension == "class" -> {
                processClassFileChange(event)
            }
            else -> emptyList()
        }
    }

    private suspend fun processClassFileChange(event: FileChangeEvent): List<PreviewItem> {
        val allItems = previewRegistry.getCatalog().previews.values.toList()
        for (item in allItems) {
            onRenderStarted?.invoke(item.id)
            val renderedItem = try {
                if (renderHandler != null) {
                    renderHandler.invoke(item.id) ?: item
                } else {
                    item
                }
            } catch (t: Throwable) {
                System.err.println("[Compositor Watcher] Error rendering ${item.id}: ${t.message}")
                item.copy(
                    status = PreviewRenderStatus.ERROR,
                    errorDetails = t.message ?: "Render failure"
                )
            }
            onPreviewUpdated?.invoke(renderedItem)
        }
        return allItems
    }

    private suspend fun processKotlinFileChange(event: FileChangeEvent): List<PreviewItem> {
        println("[Compositor Watcher] Processing Kotlin source change: ${event.file.name}")
        val content = FileReadHelper.readTextWithRetry(event.file)
        if (content == null) {
            System.err.println("[Compositor Watcher] Warning: Failed to read file ${event.file.name} after retries")
            return emptyList()
        }

        onSourceChanged?.invoke(event.file)
        val definitions = scanner.parseSource(content, event.file.invariantSeparatorsPath)
        println("[Compositor Scanner] Found ${definitions.size} preview(s) in ${event.file.name}")

        val updatedItems = previewRegistry.updateFilePreviews(
            filePath = event.file.invariantSeparatorsPath,
            definitions = definitions,
            moduleName = moduleName
        )

        for (item in updatedItems) {
            println("[Compositor Watcher] Rendering preview: ${item.id}")
            onRenderStarted?.invoke(item.id)

            val renderedItem = try {
                if (renderHandler != null) {
                    renderHandler.invoke(item.id) ?: item
                } else {
                    item
                }
            } catch (t: Throwable) {
                System.err.println("[Compositor Watcher] Error rendering preview ${item.id}: ${t.message}")
                item.copy(
                    status = PreviewRenderStatus.ERROR,
                    errorDetails = t.message ?: "Render failure"
                )
            }

            println("[Compositor Watcher] Finished preview: ${item.id} (status: ${renderedItem.status})")
            onPreviewUpdated?.invoke(renderedItem)
        }

        return updatedItems
    }

    /**
     * Stops the dispatcher and the underlying directory watcher.
     */
    @Synchronized
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            dispatchJob?.cancel()
            dispatchJob = null
            watcher.stop()
        }
    }

    override fun close() = stop()

    companion object {
        const val DEFAULT_MODULE_NAME = "app"
    }
}
