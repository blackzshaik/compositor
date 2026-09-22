package io.compositor.watcher

import io.compositor.parser.PreviewDefinition
import io.compositor.parser.PreviewItem
import io.compositor.parser.PreviewRegistry
import io.compositor.parser.PreviewRenderStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PreviewWatchDispatcherTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `file change updates registry and triggers render callback`() = runBlocking {
        val srcDir = File(tempDir, "src").apply { mkdirs() }
        val watcher = SourceDirectoryWatcher(rootDirectories = listOf(srcDir))
        val registry = PreviewRegistry()

        val renderedIds = mutableListOf<String>()
        val startedIds = mutableListOf<String>()
        val notifiedItems = mutableListOf<PreviewItem>()

        val dispatcher = PreviewWatchDispatcher(
            watcher = watcher,
            previewRegistry = registry,
            renderHandler = { previewId ->
                renderedIds.add(previewId)
                registry.updateRenderStatus(
                    previewId = previewId,
                    status = PreviewRenderStatus.RENDERED,
                    durationMs = 42L
                )
            }
        )
        dispatcher.onRenderStarted = { previewId ->
            startedIds.add(previewId)
        }
        dispatcher.onPreviewUpdated = { item ->
            notifiedItems.add(item)
        }

        val ktFile = File(srcDir, "MyGreeting.kt")
        ktFile.writeText(
            """
            package com.example.ui

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            @Preview(name = "Light Mode")
            @Composable
            fun MyGreetingPreview() {
            }
            """.trimIndent()
        )

        val updatedItems = dispatcher.handleFileChange(
            FileChangeEvent(file = ktFile, kind = ChangeKind.MODIFIED)
        )

        assertEquals(1, updatedItems.size)
        val item = updatedItems.first()
        assertEquals("app:com.example.ui.MyGreetingPreview#Light Mode", item.id)

        // Verify registry contains the preview
        val catalog = registry.getCatalog()
        assertNotNull(catalog.previews[item.id])

        // Verify renderHandler was triggered
        assertTrue(renderedIds.contains(item.id), "renderHandler should be invoked for discovered preview")
        assertTrue(startedIds.contains(item.id), "onRenderStarted should be invoked for discovered preview")

        // Verify listener was notified
        assertEquals(1, notifiedItems.size)
        assertEquals(PreviewRenderStatus.RENDERED, notifiedItems.first().status)
    }

    @Test
    fun `file deletion cleans up registry entries`() = runBlocking {
        val srcDir = File(tempDir, "src").apply { mkdirs() }
        val watcher = SourceDirectoryWatcher(rootDirectories = listOf(srcDir))
        val registry = PreviewRegistry()

        val ktFile = File(srcDir, "Card.kt")
        val def = PreviewDefinition(
            functionName = "CardPreview",
            packageName = "com.example.ui",
            line = 10,
            filePath = ktFile.invariantSeparatorsPath
        )
        registry.updateFilePreviews(ktFile.invariantSeparatorsPath, listOf(def))
        assertEquals(1, registry.getCatalog().totalCount)

        val dispatcher = PreviewWatchDispatcher(
            watcher = watcher,
            previewRegistry = registry
        )

        dispatcher.handleFileChange(
            FileChangeEvent(file = ktFile, kind = ChangeKind.DELETED)
        )

        assertEquals(0, registry.getCatalog().totalCount, "Deleted file previews should be pruned from registry")
    }
}
