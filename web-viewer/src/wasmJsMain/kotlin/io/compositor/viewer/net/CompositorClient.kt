package io.compositor.viewer.net

import io.compositor.viewer.models.DaemonWsMessage
import io.compositor.viewer.models.PreviewCatalog
import io.compositor.viewer.models.RenderResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

private fun jsDateNow(): Double = js("Date.now()")

/**
 * Pure Kotlin Ktor client communicating with the Compositor preview daemon
 * over HTTP REST and WebSocket channels.
 */
class CompositorClient(
    val baseUrl: String = resolveDefaultBaseUrl(),
    val wsUrl: String = resolveDefaultWsUrl()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
    }

    private var cacheBuster: Long = 0L

    /**
     * Fetches the current catalog of discovered composable previews.
     */
    suspend fun fetchCatalog(): PreviewCatalog =
        httpClient.get("$baseUrl/api/previews").body()

    /**
     * Requests the daemon to render or re-render a specific composable preview.
     */
    suspend fun triggerRender(previewId: String): RenderResponse =
        httpClient.post("$baseUrl/api/previews/$previewId/render").body()

    /**
     * Constructs the cache-busted URL for a preview image.
     */
    fun getPreviewImageUrl(previewId: String): String {
        cacheBuster++
        val timestamp = jsDateNow().toLong()
        return "$baseUrl/api/previews/$previewId/image?t=${timestamp}_$cacheBuster"
    }

    /**
     * Connects to the daemon WebSocket stream and dispatches incoming events.
     */
    suspend fun listenWebSocket(
        onEvent: (DaemonWsMessage) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        while (coroutineContext.isActive) {
            try {
                httpClient.webSocket(urlString = wsUrl) {
                    onStatusChange(true)
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            if (text.isNotBlank()) {
                                val message = json.decodeFromString<DaemonWsMessage>(text)
                                onEvent(message)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                onStatusChange(false)
                throw e
            } catch (_: Exception) {
                onStatusChange(false)
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    companion object {
        private const val RECONNECT_DELAY_MS = 2500L
        private const val DEFAULT_PORT = 3001

        private fun resolveDefaultBaseUrl(): String {
            val location = runCatching { window.location }.getOrNull()
            val host = location?.host
            return if (!host.isNullOrBlank() && !host.contains("localhost:0")) {
                val protocol = location.protocol.ifBlank { "http:" }
                "$protocol//$host"
            } else {
                "http://localhost:$DEFAULT_PORT"
            }
        }

        private fun resolveDefaultWsUrl(): String {
            val location = runCatching { window.location }.getOrNull()
            val host = location?.host
            return if (!host.isNullOrBlank() && !host.contains("localhost:0")) {
                val proto = if (location.protocol == "https:") "wss:" else "ws:"
                "$proto//$host/ws"
            } else {
                "ws://localhost:$DEFAULT_PORT/ws"
            }
        }
    }
}
