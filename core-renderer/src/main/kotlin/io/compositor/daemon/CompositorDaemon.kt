package io.compositor.daemon

import io.compositor.mcp.CompositorMcpServer
import io.compositor.parser.PreviewCatalog
import io.compositor.parser.PreviewItem
import io.compositor.parser.PreviewRegistry
import io.compositor.parser.PreviewRenderStatus
import io.compositor.watcher.PreviewWatchDispatcher
import io.compositor.watcher.SourceDirectoryWatcher
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Pure Kotlin Ktor daemon hosting the Compose preview canvas, REST endpoints,
 * and live-sync WebSocket channels.
 */
class CompositorDaemon(
    val port: Int = DEFAULT_PORT,
    val projectRoot: File = File("."),
    val previewRegistry: PreviewRegistry = PreviewRegistry(),
    val renderHandler: (suspend (String) -> PreviewItem?)? = null,
    var watcherActive: Boolean = false
) {
    private val startTimeMs: Long = System.currentTimeMillis()
    private val activeWsSessions = ConcurrentHashMap.newKeySet<WebSocketSession>()
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var watcherDispatcher: PreviewWatchDispatcher? = null

    val mcpServer: CompositorMcpServer = CompositorMcpServer(
        previewRegistry = previewRegistry,
        projectRoot = projectRoot,
        renderHandler = { id, _, _ -> renderHandler?.invoke(id) }
    )

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Configures all plugins and route endpoints on the target Ktor application.
     */
    fun configureApplication(app: Application) {
        installPlugins(app)
        installRoutes(app)
    }

    private fun installPlugins(app: Application) {
        app.install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.CacheControl)
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
        }

        app.install(ContentNegotiation) {
            json(json)
        }

        app.install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 15.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        app.install(SSE)
    }

    private fun installRoutes(app: Application) {
        app.routing {
            get("/api/status") {
                val status = DaemonStatus(
                    status = "running",
                    projectRoot = projectRoot.canonicalPath.replace('\\', '/'),
                    port = port,
                    watcherActive = watcherActive,
                    previewsCount = previewRegistry.getCatalog().totalCount,
                    uptimeMs = System.currentTimeMillis() - startTimeMs
                )
                call.respond(status)
            }

            get("/api/previews") {
                val catalog: PreviewCatalog = previewRegistry.getCatalog()
                call.respond(catalog)
            }

            get("/api/previews/{id}/image") {
                val previewId = resolvePreviewId(
                    rawParam = call.parameters["id"],
                    queryParam = call.request.queryParameters["id"]
                )
                servePreviewImage(call, previewId)
            }

            get("/api/previews/image") {
                val previewId = call.request.queryParameters["id"]
                servePreviewImage(call, previewId)
            }

            post("/api/previews/{id}/render") {
                val previewId = resolvePreviewId(
                    rawParam = call.parameters["id"],
                    queryParam = call.request.queryParameters["id"]
                )
                handleRenderRequest(call, previewId)
            }

            post("/api/previews/render") {
                val previewId = call.request.queryParameters["id"]
                handleRenderRequest(call, previewId)
            }

            webSocket("/ws") {
                activeWsSessions.add(this)
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            handleClientWsMessage(frame.readText())
                        }
                    }
                } catch (_: ClosedReceiveChannelException) {
                    // Normal WebSocket channel closure
                } finally {
                    activeWsSessions.remove(this)
                }
            }

            // Model Context Protocol (MCP) Server-Sent Events endpoints
            mcp("/mcp") { mcpServer.server }
            mcp("/sse") { mcpServer.server }

            // Embedded Web Viewer static bundle
            staticResources("/", "web", index = "index.html")
        }
    }

    private suspend fun servePreviewImage(
        call: io.ktor.server.application.ApplicationCall,
        previewId: String?
    ) {
        if (previewId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing preview id"))
            return
        }

        val item = previewRegistry.getCatalog().previews[previewId]
        val candidateFile = resolveImageFile(item?.imagePath, previewId)

        if (candidateFile != null && candidateFile.exists() && candidateFile.isFile) {
            call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
            call.response.header(HttpHeaders.ContentType, "image/png")
            call.respondFile(candidateFile)
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Preview image not found for id: $previewId")
            )
        }
    }

    private fun resolveImageFile(imagePath: String?, previewId: String): File? {
        if (!imagePath.isNullOrBlank()) {
            val direct = File(imagePath)
            if (direct.exists()) return direct
            val relative = File(projectRoot, imagePath)
            if (relative.exists()) return relative
        }

        val sanitized = previewId.replace(':', '_').replace('#', '_')
        val fallback = File(projectRoot, ".compositor/previews/$sanitized.png")
        if (fallback.exists()) return fallback

        return null
    }

    private suspend fun handleRenderRequest(
        call: io.ktor.server.application.ApplicationCall,
        previewId: String?
    ) {
        if (previewId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing preview id"))
            return
        }

        broadcast(DaemonWsMessage.previewRenderStarted(previewId))

        val updatedItem = if (renderHandler != null) {
            renderHandler.invoke(previewId)
        } else {
            previewRegistry.updateRenderStatus(
                previewId = previewId,
                status = PreviewRenderStatus.RENDERED,
                durationMs = DEFAULT_RENDER_DURATION_MS
            )
        }

        if (updatedItem != null && updatedItem.status == PreviewRenderStatus.RENDERED) {
            val imageUrl = updatedItem.imageUrl ?: "/api/previews/$previewId/image"
            broadcast(DaemonWsMessage.previewUpdated(previewId, imageUrl, preview = updatedItem))
            call.respond(
                HttpStatusCode.OK,
                RenderResponse(success = true, previewId = previewId, preview = updatedItem)
            )
        } else if (updatedItem != null && updatedItem.status == PreviewRenderStatus.ERROR) {
            val errorMsg = updatedItem.errorDetails ?: "Render failed"
            broadcast(DaemonWsMessage.renderError(previewId, errorMsg))
            call.respond(
                HttpStatusCode.InternalServerError,
                RenderResponse(success = false, previewId = previewId, preview = updatedItem, error = errorMsg)
            )
        } else {
            call.respond(
                HttpStatusCode.OK,
                RenderResponse(success = true, previewId = previewId, preview = updatedItem)
            )
        }
    }

    private fun handleClientWsMessage(text: String) {
        // Reserved for interactive inspector messages or ping frames
        if (text.isBlank()) return
    }

    private fun resolvePreviewId(rawParam: String?, queryParam: String?): String? =
        if (!rawParam.isNullOrBlank()) rawParam else queryParam

    /**
     * Broadcasts a WebSocket message envelope to all actively connected clients.
     */
    suspend fun broadcast(message: DaemonWsMessage) {
        val jsonPayload = json.encodeToString(message)
        val frame = Frame.Text(jsonPayload)
        val staleSessions = mutableListOf<WebSocketSession>()

        for (session in activeWsSessions) {
            try {
                session.send(frame)
            } catch (_: ClosedSendChannelException) {
                staleSessions.add(session)
            } catch (_: IOException) {
                staleSessions.add(session)
            }
        }

        if (staleSessions.isNotEmpty()) {
            activeWsSessions.removeAll(staleSessions.toSet())
        }
    }

    /**
     * Starts the embedded Ktor server.
     */
    fun start(wait: Boolean = false): CompositorDaemon {
        val engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            configureApplication(this)
        }
        server = engine
        engine.start(wait = wait)
        return this
    }

    /**
     * Attaches and activates a source directory watcher and dispatcher.
     */
    fun attachWatcher(
        watchRoots: List<File>,
        moduleName: String = "app"
    ): PreviewWatchDispatcher {
        val watcher = SourceDirectoryWatcher(rootDirectories = watchRoots)
        val dispatcher = PreviewWatchDispatcher(
            watcher = watcher,
            previewRegistry = previewRegistry,
            moduleName = moduleName,
            renderHandler = renderHandler
        )
        dispatcher.onPreviewUpdated = { item ->
            val imageUrl = item.imageUrl ?: "/api/previews/${item.id}/image"
            broadcast(DaemonWsMessage.previewUpdated(item.id, imageUrl, preview = item))
        }
        dispatcher.start()
        this.watcherDispatcher = dispatcher
        this.watcherActive = true
        return dispatcher
    }

    /**
     * Gracefully terminates the daemon server and releases network sockets.
     */
    fun stop(gracePeriodMillis: Long = 1000L, timeoutMillis: Long = 3000L) {
        watcherDispatcher?.stop()
        watcherDispatcher = null
        watcherActive = false
        server?.stop(gracePeriodMillis, timeoutMillis)
        server = null
    }

    companion object {
        const val DEFAULT_PORT = 3001
        private const val DEFAULT_RENDER_DURATION_MS = 100L

        /**
         * Factory function providing daemon startup with registered shutdown hook.
         */
        fun startDaemon(
            port: Int = DEFAULT_PORT,
            projectRoot: File = File("."),
            previewRegistry: PreviewRegistry = PreviewRegistry(File(projectRoot, ".compositor/previews.json")),
            renderHandler: (suspend (String) -> PreviewItem?)? = null
        ): CompositorDaemon {
            val daemon = CompositorDaemon(
                port = port,
                projectRoot = projectRoot,
                previewRegistry = previewRegistry,
                renderHandler = renderHandler
            )
            daemon.start(wait = false)
            Runtime.getRuntime().addShutdownHook(Thread {
                daemon.stop()
            })
            return daemon
        }
    }
}
