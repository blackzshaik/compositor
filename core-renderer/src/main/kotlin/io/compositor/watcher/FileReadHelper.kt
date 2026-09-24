package io.compositor.watcher

import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Resilient file operations designed to withstand momentary Windows filesystem locks
 * during IDE file save cycles.
 */
object FileReadHelper {
    private const val DEFAULT_MAX_RETRIES = 5
    private const val DEFAULT_RETRY_DELAY_MS = 50L

    /**
     * Reads text from a file with retry logic to avoid sharing violations during atomic IDE writes.
     */
    suspend fun readTextWithRetry(
        file: File,
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS
    ): String? {
        var attempts = 0
        while (attempts < maxRetries) {
            try {
                if (file.exists() && file.isFile && Files.isReadable(file.toPath())) {
                    val text = file.readText()
                    if (text.isNotEmpty() || (file.length() == 0L && attempts >= 2)) {
                        return text
                    }
                }
            } catch (_: IOException) {
                // Momentary file lock or sharing violation on Windows
            } catch (_: SecurityException) {
                // Temporary file permission lock
            }
            attempts++
            if (attempts < maxRetries) {
                delay(retryDelayMs)
            }
        }
        return null
    }
}
