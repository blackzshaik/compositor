package io.compositor.watcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

class SourceDirectoryWatcherTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var srcDir: File
    private lateinit var watcher: SourceDirectoryWatcher
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @BeforeEach
    fun setUp() {
        srcDir = File(tempDir, "src/main/java").apply { mkdirs() }
        watcher = SourceDirectoryWatcher(
            rootDirectories = listOf(srcDir),
            debounceDuration = 100.milliseconds,
            coroutineScope = scope
        )
        watcher.start()
    }

    @AfterEach
    fun tearDown() {
        watcher.stop()
        scope.cancel()
    }

    @Test
    fun `debouncer collapses rapid bursts into single event`() = runBlocking {
        val testFile = File(srcDir, "RapidSaveComponent.kt")
        testFile.writeText("initial content")

        val debouncedList = CopyOnWriteArrayList<FileChangeEvent>()
        val collectJob = scope.launch {
            watcher.debouncedEvents.collect { debouncedList.add(it) }
        }

        delay(150)
        debouncedList.clear()

        // Rapid write burst (5 saves within 60ms, well below 100ms debounce window)
        for (i in 1..5) {
            testFile.writeText("// Save iteration $i\nclass RapidSaveComponent")
            delay(12)
        }

        // Wait for debounce window plus safety margin
        delay(250)

        collectJob.cancel()

        assertEquals(1, debouncedList.size, "Expected exactly 1 debounced event after rapid burst")
        assertEquals(testFile.canonicalPath, debouncedList.first().file.canonicalPath)
    }

    @Test
    fun `ignores non target file extensions`() = runBlocking {
        val txtFile = File(srcDir, "notes.txt")
        val mdFile = File(srcDir, "README.md")

        val receivedEvents = CopyOnWriteArrayList<FileChangeEvent>()
        val collectJob = scope.launch {
            watcher.debouncedEvents.collect { receivedEvents.add(it) }
        }

        delay(150)
        txtFile.writeText("notes content")
        mdFile.writeText("# Documentation")
        delay(250)

        collectJob.cancel()
        assertTrue(receivedEvents.isEmpty(), "Non-monitored file extensions should be filtered out")
    }

    @Test
    fun `ignores excluded directories`() = runBlocking {
        val buildDir = File(srcDir, "build").apply { mkdirs() }
        val excludedFile = File(buildDir, "Generated.kt")

        val receivedEvents = CopyOnWriteArrayList<FileChangeEvent>()
        val collectJob = scope.launch {
            watcher.debouncedEvents.collect { receivedEvents.add(it) }
        }

        delay(150)
        excludedFile.writeText("class Generated")
        delay(250)

        collectJob.cancel()
        val buildEvents = receivedEvents.filter { it.file.canonicalPath.contains("build") }
        assertTrue(buildEvents.isEmpty(), "Changes in excluded 'build' directory should be ignored")
    }

    @Test
    fun `emits delete event when file is removed`() = runBlocking {
        val fileToDelete = File(srcDir, "TemporaryComposable.kt")
        fileToDelete.writeText("class TemporaryComposable")

        val receivedEvents = CopyOnWriteArrayList<FileChangeEvent>()
        val collectJob = scope.launch {
            watcher.debouncedEvents.collect { receivedEvents.add(it) }
        }

        delay(250)
        receivedEvents.clear()

        fileToDelete.delete()
        delay(250)

        collectJob.cancel()
        val deleteEvent = receivedEvents.firstOrNull { it.kind == ChangeKind.DELETED }
        assertTrue(deleteEvent != null, "A DELETED event should be emitted when file is removed")
        assertEquals(fileToDelete.name, deleteEvent?.file?.name)
    }
}
