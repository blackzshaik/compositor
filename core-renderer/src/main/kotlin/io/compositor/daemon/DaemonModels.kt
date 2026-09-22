package io.compositor.daemon

import io.compositor.parser.PreviewItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * Health and operational status of the Compositor preview daemon.
 */
@Serializable
data class DaemonStatus(
    val status: String = "running",
    val projectRoot: String,
    val port: Int,
    val watcherActive: Boolean = false,
    val previewsCount: Int = 0,
    val uptimeMs: Long = 0L
)

/**
 * WebSocket event message envelope broadcast to connected clients.
 */
@Serializable
data class DaemonWsMessage(
    val event: String,
    val type: String = event,
    val payload: JsonObject
) {
    companion object {
        private val json = Json { encodeDefaults = true }

        fun previewRegistered(preview: PreviewItem): DaemonWsMessage =
            DaemonWsMessage(
                event = "PREVIEW_REGISTERED",
                payload = buildJsonObject {
                    put("preview", json.encodeToJsonElement(preview))
                }
            )

        fun previewRenderStarted(previewId: String): DaemonWsMessage =
            DaemonWsMessage(
                event = "PREVIEW_RENDER_STARTED",
                payload = buildJsonObject {
                    put("previewId", previewId)
                }
            )

        fun previewUpdated(
            previewId: String,
            imageUrl: String,
            timestamp: Long = System.currentTimeMillis(),
            preview: PreviewItem? = null
        ): DaemonWsMessage =
            DaemonWsMessage(
                event = "PREVIEW_UPDATED",
                payload = buildJsonObject {
                    put("previewId", previewId)
                    put("url", imageUrl)
                    put("timestamp", timestamp)
                    put("status", "Rendered")
                    preview?.let { put("preview", json.encodeToJsonElement(it)) }
                }
            )

        fun renderError(
            previewId: String,
            error: String
        ): DaemonWsMessage =
            DaemonWsMessage(
                event = "RENDER_ERROR",
                payload = buildJsonObject {
                    put("previewId", previewId)
                    put("error", error)
                }
            )
    }
}

/**
 * Response payload returned after triggering an on-demand preview render.
 */
@Serializable
data class RenderResponse(
    val success: Boolean,
    val previewId: String,
    val preview: PreviewItem? = null,
    val error: String? = null
)
