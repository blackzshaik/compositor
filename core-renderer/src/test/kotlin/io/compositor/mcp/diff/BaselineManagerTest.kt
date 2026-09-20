package io.compositor.mcp.diff

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BaselineManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `sanitizeId replaces colon, hash, slash, and whitespace with underscore`() {
        val sanitized = BaselineManager.sanitizeId("app:com.example.Preview#Dark Mode/Test")
        assertEquals("app_com_example_Preview_Dark_Mode_Test", sanitized)
    }

    @Test
    fun `saveBaseline and loadBaseline correctly persist and read snapshot bytes`() {
        val manager = BaselineManager(tempDir)
        val previewId = "app:com.compositor.sample.GreetingPreview"
        val sampleBytes = byteArrayOf(1, 2, 3, 4, 5)

        assertFalse(manager.hasBaseline(previewId))
        assertNull(manager.loadBaseline(previewId))

        val savedFile = manager.saveBaseline(previewId, sampleBytes)
        assertTrue(savedFile.exists())
        assertTrue(manager.hasBaseline(previewId))

        val loadedBytes = manager.loadBaseline(previewId)
        assertNotNull(loadedBytes)
        assertArrayEquals(sampleBytes, loadedBytes)
    }

    @Test
    fun `saveBaseline with timestamp creates timestamped and default copies`() {
        val manager = BaselineManager(tempDir)
        val previewId = "app:com.compositor.sample.HeaderPreview"
        val timestamp = 1700000000000L
        val bytes = byteArrayOf(9, 8, 7)

        manager.saveBaseline(previewId, bytes, timestamp)

        assertTrue(manager.hasBaseline(previewId, timestamp))
        assertTrue(manager.hasBaseline(previewId))

        val history = manager.listBaselines(previewId)
        assertTrue(history.size >= 2)
    }
}
