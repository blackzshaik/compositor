package io.compositor.daemon

import io.compositor.parser.PreviewDefinition
import io.compositor.parser.PreviewItem
import io.compositor.parser.PreviewParameters
import io.compositor.parser.PreviewRegistry
import io.compositor.parser.PreviewRenderStatus
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CompositorDaemonTest {

    @TempDir
    lateinit var tempDir: File

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun createSamplePreview(
        id: String = "app:com.compositor.sample.GreetingPreview#Default",
        imagePath: String? = null
    ): PreviewItem = PreviewItem(
        id = id,
        module = "app",
        definition = PreviewDefinition(
            functionName = "GreetingPreview",
            enclosingClass = null,
            packageName = "com.compositor.sample",
            parameters = PreviewParameters(name = "Default"),
            line = 42,
            filePath = "Greeting.kt"
        ),
        status = if (imagePath != null) PreviewRenderStatus.RENDERED else PreviewRenderStatus.PENDING,
        imagePath = imagePath
    )

    @Test
    fun testStatusEndpointReturnsRunning() = testApplication {
        val registry = PreviewRegistry()
        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir,
            previewRegistry = registry
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        val response = client.get("/api/status")
        assertEquals(HttpStatusCode.OK, response.status)

        val status = response.body<DaemonStatus>()
        assertEquals("running", status.status)
        assertEquals(3001, status.port)
        assertEquals(0, status.previewsCount)
        assertTrue(status.uptimeMs >= 0)
    }

    @Test
    fun testPreviewsEndpointReturnsCatalog() = testApplication {
        val registry = PreviewRegistry()
        val item = createSamplePreview()
        registry.updateFilePreviews(
            filePath = "Greeting.kt",
            definitions = listOf(item.definition),
            moduleName = "app"
        )

        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir,
            previewRegistry = registry
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        val response = client.get("/api/previews")
        assertEquals(HttpStatusCode.OK, response.status)

        val catalog = response.body<io.compositor.parser.PreviewCatalog>()
        assertEquals(1, catalog.totalCount)
        assertTrue(catalog.previews.containsKey(item.id))
    }

    @Test
    fun testPreviewImageNotFoundReturns404() = testApplication {
        val registry = PreviewRegistry()
        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir,
            previewRegistry = registry
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {}
        val response = client.get("/api/previews/nonexistent/image")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testPreviewImageFoundStreamsPng() = testApplication {
        val registry = PreviewRegistry()
        val previewPng = File(tempDir, "preview_test.png").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        }

        val item = createSamplePreview(imagePath = previewPng.absolutePath)
        registry.updateFilePreviews(
            filePath = "Greeting.kt",
            definitions = listOf(item.definition),
            moduleName = "app"
        )
        registry.updateRenderStatus(
            previewId = item.id,
            status = PreviewRenderStatus.RENDERED,
            imagePath = previewPng.absolutePath
        )

        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir,
            previewRegistry = registry
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {}
        val encodedId = java.net.URLEncoder.encode(item.id, "UTF-8")
        val response = client.get("/api/previews/$encodedId/image")

        assertEquals(HttpStatusCode.OK, response.status)
        val cacheControl = response.headers[HttpHeaders.CacheControl]
        assertNotNull(cacheControl)
        assertTrue(cacheControl?.contains("no-cache") == true)
        val contentType = response.headers[HttpHeaders.ContentType]
        assertTrue(contentType?.contains("image/png") == true)
    }

    @Test
    fun testDirectPreviewGetReturnsImage() = testApplication {
        val registry = PreviewRegistry()
        val previewPng = File(tempDir, "direct_preview.png").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        }

        val item = createSamplePreview(imagePath = previewPng.absolutePath)
        registry.updateFilePreviews(
            filePath = "Greeting.kt",
            definitions = listOf(item.definition),
            moduleName = "app"
        )
        registry.updateRenderStatus(
            previewId = item.id,
            status = PreviewRenderStatus.RENDERED,
            imagePath = previewPng.absolutePath
        )

        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir,
            previewRegistry = registry
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {}
        val encodedId = java.net.URLEncoder.encode(item.id, "UTF-8")
        val response = client.get("/api/previews/$encodedId")

        assertEquals(HttpStatusCode.OK, response.status)
        val contentType = response.headers[HttpHeaders.ContentType]
        assertTrue(contentType?.contains("image/png") == true)
    }

    @Test
    fun testDirectPreviewGetWithSubmoduleOrTagFuzzyMatch() = testApplication {
        val registry = PreviewRegistry()
        val appPreviewsDir = File(tempDir, "app/.compositor/previews").apply { mkdirs() }
        val fuzzyPng = File(
            appPreviewsDir,
            "app_com_compositor_sample_GreetingKt_GreetingPreview_Default.png"
        ).apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        }

        val item = createSamplePreview(imagePath = fuzzyPng.absolutePath)
        registry.updateFilePreviews(
            filePath = "Greeting.kt",
            definitions = listOf(item.definition),
            moduleName = "app"
        )

        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir,
            previewRegistry = registry
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {}
        // Omit #Default tag in request, simulating browser truncation
        val requestedId = "app:com.compositor.sample.GreetingPreview"
        val response = client.get("/api/previews/$requestedId")

        assertEquals(HttpStatusCode.OK, response.status)
        val contentType = response.headers[HttpHeaders.ContentType]
        assertTrue(contentType?.contains("image/png") == true)
    }

    @Test
    fun testDirectPreviewGetWithJsonAcceptReturnsMetadata() = testApplication {
        val registry = PreviewRegistry()
        val item = createSamplePreview()
        registry.updateFilePreviews(
            filePath = "Greeting.kt",
            definitions = listOf(item.definition),
            moduleName = "app"
        )

        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir,
            previewRegistry = registry
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        val encodedId = java.net.URLEncoder.encode(item.id, "UTF-8")
        val response = client.get("/api/previews/$encodedId") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<PreviewItem>()
        assertEquals(item.id, body.id)
    }

    @Test
    fun testTriggerRenderEndpoint() = testApplication {
        val registry = PreviewRegistry()
        val item = createSamplePreview()
        registry.updateFilePreviews(
            filePath = "Greeting.kt",
            definitions = listOf(item.definition),
            moduleName = "app"
        )

        var renderHandlerCalled = false
        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir,
            previewRegistry = registry,
            renderHandler = { id ->
                renderHandlerCalled = true
                registry.updateRenderStatus(
                    previewId = id,
                    status = PreviewRenderStatus.RENDERED,
                    durationMs = 85L
                )
            }
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        val encodedId = java.net.URLEncoder.encode(item.id, "UTF-8")
        val response = client.post("/api/previews/$encodedId/render")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(renderHandlerCalled)

        val renderResp = response.body<RenderResponse>()
        assertTrue(renderResp.success)
        assertEquals(PreviewRenderStatus.RENDERED, renderResp.preview?.status)
    }

    @Test
    fun testStaticAssetsIndexHtml() = testApplication {
        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir
        )

        application {
            daemon.configureApplication(this)
        }

        val client = createClient {}
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        val htmlContent = response.bodyAsText()
        assertTrue(htmlContent.contains("<html") || htmlContent.contains("<!DOCTYPE html>"))
    }

    @Test
    fun testWebSocketBroadcasting() = testApplication {
        val daemon = CompositorDaemon(
            port = 3001,
            projectRoot = tempDir
        )

        application {
            daemon.configureApplication(this)
        }

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/ws") {
            launch {
                delay(100)
                daemon.broadcast(
                    DaemonWsMessage.previewUpdated(
                        previewId = "app:Greeting",
                        imageUrl = "/api/previews/app:Greeting/image"
                    )
                )
            }

            val received = incoming.receive() as Frame.Text
            val text = received.readText()
            val parsed = json.decodeFromString<DaemonWsMessage>(text)
            assertEquals("PREVIEW_UPDATED", parsed.event)
            assertEquals("app:Greeting", parsed.payload["previewId"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun testDaemonLifecycleStartStop() {
        val daemon = CompositorDaemon(
            port = 39123,
            projectRoot = tempDir
        )
        daemon.start(wait = false)
        daemon.stop(gracePeriodMillis = 100, timeoutMillis = 500)
    }
}
