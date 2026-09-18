package io.compositor.watcher

import java.io.File

/**
 * Type of filesystem modification detected by the watcher.
 */
enum class ChangeKind {
    CREATED,
    MODIFIED,
    DELETED
}

/**
 * Event payload describing a detected filesystem change for a tracked file.
 */
data class FileChangeEvent(
    val file: File,
    val kind: ChangeKind,
    val timestamp: Long = System.currentTimeMillis()
)
