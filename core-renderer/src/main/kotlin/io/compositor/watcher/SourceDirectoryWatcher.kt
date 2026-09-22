package io.compositor.watcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * High-performance coroutine-native source directory watcher.
 * Recursively monitors directories for `.kt` and resource modifications,
 * debounces rapid editor bursts per file, and emits structured change events.
 */
class SourceDirectoryWatcher(
    val rootDirectories: List<File>,
    val debounceDuration: Duration = DEFAULT_DEBOUNCE_DURATION,
    val allowedExtensions: Set<String> = DEFAULT_EXTENSIONS,
    val excludedDirectories: Set<String> = DEFAULT_EXCLUDED_DIRS,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : AutoCloseable {

    private val isRunning = AtomicBoolean(false)
    private var watchService: WatchService? = null
    private val watchKeys = ConcurrentHashMap<WatchKey, Path>()
    private val pendingDebounceJobs = ConcurrentHashMap<String, Job>()

    private val _rawEvents = MutableSharedFlow<FileChangeEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    val rawEvents: SharedFlow<FileChangeEvent> = _rawEvents.asSharedFlow()

    private val _debouncedEvents = MutableSharedFlow<FileChangeEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    val debouncedEvents: SharedFlow<FileChangeEvent> = _debouncedEvents.asSharedFlow()

    private var pollJob: Job? = null

    /**
     * Starts watching all configured root directories.
     */
    @Synchronized
    fun start(): Job {
        if (isRunning.compareAndSet(false, true)) {
            val service = FileSystems.getDefault().newWatchService()
            watchService = service

            for (root in rootDirectories) {
                if (root.exists() && root.isDirectory) {
                    registerRecursively(root.toPath(), service)
                }
            }

            pollJob = coroutineScope.launch {
                runWatchLoop(service)
            }
        }
        return pollJob ?: Job().apply { complete() }
    }

    private suspend fun runWatchLoop(service: WatchService) {
        while (isRunning.get() && coroutineScope.isActive) {
            val key = try {
                service.poll(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                break
            } catch (_: ClosedWatchServiceException) {
                break
            }

            if (key == null) continue

            val dirPath = watchKeys[key]
            if (dirPath != null) {
                processKeyEvents(key, dirPath, service)
            }

            val isValid = key.reset()
            if (!isValid) {
                watchKeys.remove(key)
            }
        }
    }

    private suspend fun processKeyEvents(key: WatchKey, dirPath: Path, service: WatchService) {
        for (event in key.pollEvents()) {
            val kind = event.kind()
            if (kind == StandardWatchEventKinds.OVERFLOW) continue

            val relativePath = event.context() as? Path ?: continue
            val resolvedPath = dirPath.resolve(relativePath)

            handlePathEvent(kind, resolvedPath, service)
        }
    }

    private suspend fun handlePathEvent(
        kind: java.nio.file.WatchEvent.Kind<*>,
        resolvedPath: Path,
        service: WatchService
    ) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(resolvedPath)) {
            registerRecursively(resolvedPath, service)
            return
        }

        val changeKind = when (kind) {
            StandardWatchEventKinds.ENTRY_CREATE -> ChangeKind.CREATED
            StandardWatchEventKinds.ENTRY_MODIFY -> ChangeKind.MODIFIED
            StandardWatchEventKinds.ENTRY_DELETE -> ChangeKind.DELETED
            else -> null
        } ?: return

        val file = resolvedPath.toFile()
        if (isMonitoredFile(file)) {
            val changeEvent = FileChangeEvent(file = file, kind = changeKind)
            _rawEvents.emit(changeEvent)
            scheduleDebouncedDispatch(changeEvent)
        }
    }

    private fun scheduleDebouncedDispatch(event: FileChangeEvent) {
        val key = event.file.absolutePath
        pendingDebounceJobs[key]?.cancel()

        pendingDebounceJobs[key] = coroutineScope.launch {
            delay(debounceDuration)
            _debouncedEvents.emit(event)
            pendingDebounceJobs.remove(key)
        }
    }

    private fun isMonitoredFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in allowedExtensions
    }

    private fun isDirectoryExcluded(path: Path): Boolean {
        for (segment in path) {
            val name = segment.toString()
            if (name in excludedDirectories) {
                return true
            }
        }
        return false
    }

    private fun registerRecursively(root: Path, service: WatchService) {
        if (isDirectoryExcluded(root)) return

        try {
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (isDirectoryExcluded(dir)) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    registerDirectory(dir, service)
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (_: IOException) {
            // Directory might have been removed concurrently
        }
    }

    private fun registerDirectory(dir: Path, service: WatchService) {
        try {
            val key = dir.register(
                service,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )
            watchKeys[key] = dir
        } catch (_: IOException) {
            // Ignored if folder became inaccessible or was deleted
        }
    }

    /**
     * Halts directory monitoring and releases filesystem handles.
     */
    @Synchronized
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            pollJob?.cancel()
            pollJob = null

            for ((_, job) in pendingDebounceJobs) {
                job.cancel()
            }
            pendingDebounceJobs.clear()

            try {
                watchService?.close()
            } catch (_: IOException) {
                // Sockets or handles already closed
            }
            watchService = null
            watchKeys.clear()
        }
    }

    override fun close() = stop()

    companion object {
        val DEFAULT_DEBOUNCE_DURATION: Duration = 250.milliseconds
        val DEFAULT_EXTENSIONS: Set<String> = setOf("kt", "xml", "class")
        val DEFAULT_EXCLUDED_DIRS: Set<String> = setOf(
            "build",
            ".gradle",
            ".git",
            ".compositor",
            ".idea",
            "node_modules",
            "out",
            "bin"
        )
        private const val POLL_INTERVAL_MS = 100L
        private const val EVENT_BUFFER_CAPACITY = 128
    }
}
