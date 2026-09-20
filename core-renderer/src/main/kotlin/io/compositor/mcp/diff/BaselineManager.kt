package io.compositor.mcp.diff

import java.io.File

/**
 * Manages baseline snapshot images stored under `.compositor/baselines/` for visual regression checks.
 */
class BaselineManager(
    val projectRoot: File = File(".")
) {
    val baselinesDir: File = File(projectRoot, ".compositor/baselines").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    /**
     * Resolves the target baseline snapshot file.
     */
    fun getBaselineFile(previewId: String, timestamp: Long? = null): File {
        val sanitized = sanitizeId(previewId)
        return if (timestamp != null) {
            File(baselinesDir, "${sanitized}_$timestamp.png")
        } else {
            File(baselinesDir, "$sanitized.png")
        }
    }

    /**
     * Checks if a baseline snapshot exists for the given preview.
     */
    fun hasBaseline(previewId: String, timestamp: Long? = null): Boolean =
        getBaselineFile(previewId, timestamp).let { it.exists() && it.isFile }

    /**
     * Saves an image byte array as a baseline snapshot.
     */
    fun saveBaseline(previewId: String, imageBytes: ByteArray, timestamp: Long? = null): File {
        if (!baselinesDir.exists()) {
            baselinesDir.mkdirs()
        }
        val target = getBaselineFile(previewId, timestamp)
        target.writeBytes(imageBytes)

        // Also update default baseline if a timestamped baseline was saved
        if (timestamp != null) {
            val defaultTarget = getBaselineFile(previewId, null)
            defaultTarget.writeBytes(imageBytes)
        }

        return target
    }

    /**
     * Loads a baseline snapshot as bytes, or null if no baseline exists.
     */
    fun loadBaseline(previewId: String, timestamp: Long? = null): ByteArray? {
        val file = getBaselineFile(previewId, timestamp)
        return if (file.exists() && file.isFile) file.readBytes() else null
    }

    /**
     * Lists all historical baseline files associated with the specified preview.
     */
    fun listBaselines(previewId: String): List<File> {
        val sanitized = sanitizeId(previewId)
        return baselinesDir.listFiles { file ->
            file.isFile && file.name.startsWith(sanitized) && file.name.endsWith(".png")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    companion object {
        /**
         * Sanitizes arbitrary preview identifiers into filesystem-safe filenames.
         */
        fun sanitizeId(previewId: String): String =
            previewId.replace(Regex("[:#./\\\\ ]"), "_")
    }
}
