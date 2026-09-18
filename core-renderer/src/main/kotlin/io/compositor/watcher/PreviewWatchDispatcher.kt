package io.compositor.watcher

import io.compositor.parser.KotlinPsiPreviewScanner
import io.compositor.parser.PreviewItem
import io.compositor.parser.PreviewRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
                    handleFileChange(event)
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
                previewRegistry.removeFile(event.file.invariantSeparatorsPath)
                emptyList()
            }
            extension == "kt" -> {
                processKotlinFileChange(event)
            }
            else -> emptyList()
        }
    }

    private suspend fun processKotlinFileChange(event: FileChangeEvent): List<PreviewItem> {
        val content = FileReadHelper.readTextWithRetry(event.file) ?: return emptyList()
        val definitions = scanner.parseSource(content, event.file.invariantSeparatorsPath)
        val updatedItems = previewRegistry.updateFilePreviews(
            filePath = event.file.invariantSeparatorsPath,
            definitions = definitions,
            moduleName = moduleName
        )

        for (item in updatedItems) {
            val renderedItem = if (renderHandler != null) {
                renderHandler.invoke(item.id) ?: item
            } else {
                item
            }
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
