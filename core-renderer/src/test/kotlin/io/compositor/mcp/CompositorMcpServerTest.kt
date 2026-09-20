package io.compositor.mcp

import io.compositor.mcp.diff.BaselineManager
import io.compositor.parser.PreviewDefinition
import io.compositor.parser.PreviewParameters
import io.compositor.parser.PreviewRegistry
import io.compositor.parser.PreviewRenderStatus
import io.compositor.renderer.ElementBounds
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.ImageContent
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class CompositorMcpServerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var registry: PreviewRegistry
    private lateinit var baselineManager: BaselineManager
    private lateinit var mcpServer: CompositorMcpServer

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        val registryFile = File(tempDir, ".compositor/previews.json")
        registry = PreviewRegistry(registryFile)
        baselineManager = BaselineManager(tempDir)

        val def1 = PreviewDefinition(
            functionName = "GreetingPreview",
            packageName = "com.compositor.sample",
            parameters = PreviewParameters(name = "Greeting"),
            line = 42,
            filePath = "src/main/Greeting.kt"
        )

        val def2 = PreviewDefinition(
            functionName = "HeaderPreview",
            packageName = "com.compositor.sample",
            parameters = PreviewParameters(name = "Header"),
            line = 88,
            filePath = "src/main/Header.kt"
        )

        registry.updateFilePreviews("src/main/Greeting.kt", listOf(def1), "app")
        registry.updateFilePreviews("src/main/Header.kt", listOf(def2), "app")

        mcpServer = CompositorMcpServer(
            previewRegistry = registry,
            projectRoot = tempDir,
            baselineManager = baselineManager,
            renderHandler = { previewId, _, _ ->
                registry.updateRenderStatus(
                    previewId = previewId,
                    status = PreviewRenderStatus.RENDERED,
                    durationMs = 150L
                )
            },
            layoutBoundsProvider = { _ ->
                ElementBounds(
                    className = "androidx.compose.foundation.layout.Column",
                    left = 0,
                    top = 0,
                    width = 1080,
                    height = 2340,
                    children = listOf(
                        ElementBounds("androidx.compose.material3.Text", 48, 96, 400, 80)
                    )
                )
            }
        )
    }

    @Test
    fun `list_previews tool returns all discovered previews without filter`() = runBlocking {
        val request = CallToolRequest(
            name = "list_previews",
            arguments = buildJsonObject {}
        )

        val result = mcpServer.executeTool(request)
        assertFalse(result.isError ?: false)

        val text = (result.content.first() as TextContent).text
        val payload = json.decodeFromString<JsonObject>(text.orEmpty())

        assertEquals(2, payload["totalCount"]?.jsonPrimitive?.intOrNull)
        val previews = payload["previews"]?.jsonArray
        assertNotNull(previews)
        assertEquals(2, previews?.size)
    }

    @Test
    fun `list_previews tool filters by query`() = runBlocking {
        val request = CallToolRequest(
            name = "list_previews",
            arguments = buildJsonObject {
                put("filter", "Header")
            }
        )

        val result = mcpServer.executeTool(request)
        val text = (result.content.first() as TextContent).text
        val payload = json.decodeFromString<JsonObject>(text.orEmpty())

        assertEquals(1, payload["totalCount"]?.jsonPrimitive?.intOrNull)
        val preview = payload["previews"]?.jsonArray?.first()?.jsonObject
        assertEquals(
            "app:com.compositor.sample.HeaderPreview#Header",
            preview?.get("id")?.jsonPrimitive?.content
        )
    }

    @Test
    fun `render_preview tool triggers render and returns status`() = runBlocking {
        val targetId = "app:com.compositor.sample.GreetingPreview#Greeting"
        val request = CallToolRequest(
            name = "render_preview",
            arguments = buildJsonObject {
                put("previewId", targetId)
            }
        )

        val result = mcpServer.executeTool(request)
        assertFalse(result.isError ?: false)

        val text = (result.content.first() as TextContent).text
        val payload = json.decodeFromString<JsonObject>(text.orEmpty())

        assertTrue(payload["success"]?.jsonPrimitive?.booleanOrNull ?: false)
        assertEquals("RENDERED", payload["status"]?.jsonPrimitive?.content)
        assertEquals(150L, payload["durationMs"]?.jsonPrimitive?.doubleOrNull?.toLong())
    }

    @Test
    fun `get_preview_image returns ImageContent for existing preview bitmap`() = runBlocking {
        val previewId = "app:com.compositor.sample.GreetingPreview#Greeting"
        val previewsDir = File(tempDir, ".compositor/previews").apply { mkdirs() }
        val sanitized = BaselineManager.sanitizeId(previewId)
        val imageFile = File(previewsDir, "$sanitized.png")
        createTestImage(imageFile, Color.GREEN)

        val request = CallToolRequest(
            name = "get_preview_image",
            arguments = buildJsonObject {
                put("previewId", previewId)
            }
        )

        val result = mcpServer.executeTool(request)
        assertFalse(result.isError ?: false)

        val imageContent = result.content.filterIsInstance<ImageContent>().firstOrNull()
        assertNotNull(imageContent)
        assertEquals("image/png", imageContent?.mimeType)
        assertTrue(imageContent?.data?.isNotBlank() ?: false)
    }

    @Test
    fun `inspect_layout_tree returns layout hierarchy and bounds`() = runBlocking {
        val targetId = "app:com.compositor.sample.GreetingPreview#Greeting"
        val request = CallToolRequest(
            name = "inspect_layout_tree",
            arguments = buildJsonObject {
                put("previewId", targetId)
            }
        )

        val result = mcpServer.executeTool(request)
        val text = (result.content.first() as TextContent).text
        val payload = json.decodeFromString<JsonObject>(text.orEmpty())

        assertEquals("androidx.compose.foundation.layout.Column", payload["className"]?.jsonPrimitive?.content)
        assertEquals(1080, payload["width"]?.jsonPrimitive?.intOrNull)
        val children = payload["children"]?.jsonArray
        assertNotNull(children)
        assertEquals(1, children?.size)
    }

    @Test
    fun `compare_previews establishes baseline on first call and detects diffs on subsequent call`() = runBlocking {
        val previewId = "app:com.compositor.sample.GreetingPreview#Greeting"
        val previewsDir = File(tempDir, ".compositor/previews").apply { mkdirs() }
        val sanitized = BaselineManager.sanitizeId(previewId)
        val imageFile = File(previewsDir, "$sanitized.png")

        // First render: solid white image
        createTestImage(imageFile, Color.WHITE)

        val firstCall = CallToolRequest(
            name = "compare_previews",
            arguments = buildJsonObject {
                put("previewId", previewId)
            }
        )

        val firstResult = mcpServer.executeTool(firstCall)
        val firstText = (firstResult.content.first() as TextContent).text
        val firstPayload = json.decodeFromString<JsonObject>(firstText.orEmpty())

        assertTrue(firstPayload["baselineEstablished"]?.jsonPrimitive?.booleanOrNull ?: false)
        assertFalse(firstPayload["hasVisualDifferences"]?.jsonPrimitive?.booleanOrNull ?: true)

        // Second render: modify image with red patch
        val img = ImageIO.read(imageFile)
        val g = img.createGraphics()
        try {
            g.color = Color.RED
            g.fillRect(10, 10, 20, 20)
        } finally {
            g.dispose()
        }
        ImageIO.write(img, "PNG", imageFile)

        val secondCall = CallToolRequest(
            name = "compare_previews",
            arguments = buildJsonObject {
                put("previewId", previewId)
            }
        )

        val secondResult = mcpServer.executeTool(secondCall)
        val secondText = (secondResult.content.first() as TextContent).text
        val secondPayload = json.decodeFromString<JsonObject>(secondText.orEmpty())

        assertTrue(secondPayload["hasVisualDifferences"]?.jsonPrimitive?.booleanOrNull ?: false)
        assertTrue((secondPayload["differencePercentage"]?.jsonPrimitive?.doubleOrNull ?: 0.0) > 0.0)

        // Diff image content is attached
        val diffImageContent = secondResult.content.filterIsInstance<ImageContent>().firstOrNull()
        assertNotNull(diffImageContent)
        assertEquals("image/png", diffImageContent?.mimeType)
    }

    private fun createTestImage(file: File, color: Color) {
        val img = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        try {
            g.color = color
            g.fillRect(0, 0, 100, 100)
        } finally {
            g.dispose()
        }
        ImageIO.write(img, "PNG", file)
    }
}
