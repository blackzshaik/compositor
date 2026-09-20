package io.compositor.mcp

import io.compositor.mcp.diff.BaselineManager
import io.compositor.mcp.diff.VisualDiffEngine
import io.compositor.parser.PreviewCatalog
import io.compositor.parser.PreviewItem
import io.compositor.parser.PreviewRegistry
import io.compositor.parser.PreviewRenderStatus
import io.compositor.renderer.ElementBounds
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.ImageContent
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.PromptMessageContent
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.util.Base64

/**
 * Embedded Model Context Protocol (MCP) server exposing native visual tools to AI agents.
 */
class CompositorMcpServer(
    val previewRegistry: PreviewRegistry = PreviewRegistry(),
    val projectRoot: File = File("."),
    val baselineManager: BaselineManager = BaselineManager(projectRoot),
    val renderHandler: (suspend (previewId: String, theme: String?, fontScale: Float?) -> PreviewItem?)? = null,
    val layoutBoundsProvider: ((previewId: String) -> ElementBounds?)? = null
) {
    val server: Server

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        val serverInfo = Implementation(
            name = "compositor-mcp",
            version = "0.1.0"
        )
        val options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
        server = Server(serverInfo, options)
        registerTools()
    }

    private fun registerTools() {
        server.addTools(
            listOf(
                createListPreviewsTool(),
                createRenderPreviewTool(),
                createGetPreviewImageTool(),
                createInspectLayoutTreeTool(),
                createComparePreviewsTool()
            )
        )
    }

    /**
     * Dispatches a tool execution request to the appropriate tool handler.
     */
    suspend fun executeTool(request: CallToolRequest): CallToolResult = when (request.name) {
        "list_previews" -> handleListPreviews(request)
        "render_preview" -> handleRenderPreview(request)
        "get_preview_image" -> handleGetPreviewImage(request)
        "inspect_layout_tree" -> handleInspectLayoutTree(request)
        "compare_previews" -> handleComparePreviews(request)
        else -> CallToolResult(
            content = listOf(TextContent(text = "Unknown tool: ${request.name}")),
            isError = true
        )
    }

    private fun createListPreviewsTool(): RegisteredTool {
        val input = Tool.Input(
            properties = buildJsonObject {
                put("filter", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional search query to filter by composable name, package, or module.")
                })
            },
            required = emptyList()
        )
        val tool = Tool(
            name = "list_previews",
            description = "Discovers all @Preview composables in the Android project, returning their IDs, " +
                "file paths, and render statuses.",
            inputSchema = input
        )

        return RegisteredTool(tool) { request ->
            handleListPreviews(request)
        }
    }

    private fun handleListPreviews(request: CallToolRequest): CallToolResult {
        val filter = request.arguments?.get("filter")?.jsonPrimitive?.contentOrNull?.trim()
        val catalog: PreviewCatalog = previewRegistry.getCatalog()

        val filteredPreviews = if (filter.isNullOrBlank()) {
            catalog.previews.values.toList()
        } else {
            catalog.previews.values.filter { item ->
                item.id.contains(filter, ignoreCase = true) ||
                    item.module.contains(filter, ignoreCase = true) ||
                    item.definition.functionName.contains(filter, ignoreCase = true) ||
                    item.definition.packageName.contains(filter, ignoreCase = true)
            }
        }

        val summaries = filteredPreviews.map { item ->
            buildJsonObject {
                put("id", item.id)
                put("module", item.module)
                put("functionName", item.definition.functionName)
                put("packageName", item.definition.packageName)
                put("filePath", item.definition.filePath)
                put("line", item.definition.line)
                put("status", item.status.name)
                item.durationMs?.let { put("durationMs", it) }
                item.lastRenderedAt?.let { put("lastRenderedAt", it) }
                item.imagePath?.let { put("imagePath", it) }
            }
        }

        val resultPayload = buildJsonObject {
            put("totalCount", summaries.size)
            put("previews", JsonArray(summaries))
        }

        return CallToolResult(
            content = listOf(TextContent(text = json.encodeToString(resultPayload)))
        )
    }

    private fun createRenderPreviewTool(): RegisteredTool {
        val input = Tool.Input(
            properties = buildJsonObject {
                put("previewId", buildJsonObject {
                    put("type", "string")
                    put(
                        "description",
                        "The unique preview identifier (e.g. app:com.compositor.sample.GreetingPreview)."
                    )
                })
                put("theme", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional theme override ('light' or 'dark').")
                })
                put("fontScale", buildJsonObject {
                    put("type", "number")
                    put("description", "Optional font scale factor (e.g. 1.0, 1.25).")
                })
            },
            required = listOf("previewId")
        )
        val tool = Tool(
            name = "render_preview",
            description = "Triggers an on-demand render of a composable preview, returning status, duration, " +
                "and any compilation errors.",
            inputSchema = input
        )

        return RegisteredTool(tool) { request ->
            handleRenderPreview(request)
        }
    }

    private suspend fun handleRenderPreview(request: CallToolRequest): CallToolResult {
        val previewId = request.arguments?.get("previewId")?.jsonPrimitive?.contentOrNull
        if (previewId.isNullOrBlank()) {
            return CallToolResult(
                content = listOf(TextContent(text = "Error: 'previewId' argument is required.")),
                isError = true
            )
        }

        val theme = request.arguments?.get("theme")?.jsonPrimitive?.contentOrNull
        val fontScale = request.arguments?.get("fontScale")?.jsonPrimitive?.floatOrNull

        val updatedItem = if (renderHandler != null) {
            renderHandler.invoke(previewId, theme, fontScale)
        } else {
            previewRegistry.updateRenderStatus(
                previewId = previewId,
                status = PreviewRenderStatus.RENDERED,
                durationMs = 120L
            )
        }

        if (updatedItem == null) {
            return CallToolResult(
                content = listOf(TextContent(text = "Preview not found with ID: $previewId")),
                isError = true
            )
        }

        return if (updatedItem.status == PreviewRenderStatus.ERROR) {
            val errorMsg = updatedItem.errorDetails ?: "Unknown rendering failure"
            CallToolResult(
                content = listOf(TextContent(text = "Failed to render preview '$previewId':\n$errorMsg")),
                isError = true
            )
        } else {
            val payload = buildJsonObject {
                put("success", true)
                put("previewId", updatedItem.id)
                put("status", updatedItem.status.name)
                updatedItem.durationMs?.let { put("durationMs", it) }
                updatedItem.imagePath?.let { put("imagePath", it) }
                updatedItem.imageUrl?.let { put("imageUrl", it) }
            }
            CallToolResult(
                content = listOf(TextContent(text = json.encodeToString(payload)))
            )
        }
    }

    private fun createGetPreviewImageTool(): RegisteredTool {
        val input = Tool.Input(
            properties = buildJsonObject {
                put("previewId", buildJsonObject {
                    put("type", "string")
                    put("description", "The preview identifier whose rendered image is requested.")
                })
            },
            required = listOf("previewId")
        )
        val tool = Tool(
            name = "get_preview_image",
            description = "Retrieves the rendered PNG image for a composable preview as an image content block " +
                "for multimodal LLMs.",
            inputSchema = input
        )

        return RegisteredTool(tool) { request ->
            handleGetPreviewImage(request)
        }
    }

    private fun handleGetPreviewImage(request: CallToolRequest): CallToolResult {
        val previewId = request.arguments?.get("previewId")?.jsonPrimitive?.contentOrNull
        if (previewId.isNullOrBlank()) {
            return CallToolResult(
                content = listOf(TextContent(text = "Error: 'previewId' argument is required.")),
                isError = true
            )
        }

        val file = resolveImageFile(previewId)
        if (file == null || !file.exists() || !file.isFile) {
            return CallToolResult(
                content = listOf(
                    TextContent(
                        text = "Image artifact not found for preview '$previewId'. " +
                            "Please execute render_preview first."
                    )
                ),
                isError = true
            )
        }

        val bytes = file.readBytes()
        val base64Data = Base64.getEncoder().encodeToString(bytes)

        val contents = listOf<PromptMessageContent>(
            ImageContent(data = base64Data, mimeType = "image/png"),
            TextContent(text = "Rendered preview bitmap for '$previewId' (${bytes.size} bytes).")
        )
        return CallToolResult(content = contents)
    }

    private fun createInspectLayoutTreeTool(): RegisteredTool {
        val input = Tool.Input(
            properties = buildJsonObject {
                put("previewId", buildJsonObject {
                    put("type", "string")
                    put("description", "The preview identifier whose layout tree is requested.")
                })
            },
            required = listOf("previewId")
        )
        val tool = Tool(
            name = "inspect_layout_tree",
            description = "Retrieves the parsed layout hierarchy, coordinate bounds, padding, and semantics " +
                "for spatial inspection.",
            inputSchema = input
        )

        return RegisteredTool(tool) { request ->
            handleInspectLayoutTree(request)
        }
    }

    private fun handleInspectLayoutTree(request: CallToolRequest): CallToolResult {
        val previewId = request.arguments?.get("previewId")?.jsonPrimitive?.contentOrNull
        if (previewId.isNullOrBlank()) {
            return CallToolResult(
                content = listOf(TextContent(text = "Error: 'previewId' argument is required.")),
                isError = true
            )
        }

        val bounds = layoutBoundsProvider?.invoke(previewId)
        val treePayload = if (bounds != null) {
            elementBoundsToJson(bounds)
        } else {
            val item = previewRegistry.getCatalog().previews[previewId]
            buildJsonObject {
                put("previewId", previewId)
                put("composable", item?.definition?.functionName ?: "Unknown")
                put("note", "Synthesized bounds root. Precise LayoutLib bounds not yet cached.")
                put("left", 0)
                put("top", 0)
                put("width", item?.definition?.parameters?.widthDp ?: 1080)
                put("height", item?.definition?.parameters?.heightDp ?: 2340)
            }
        }

        return CallToolResult(
            content = listOf(TextContent(text = json.encodeToString(treePayload)))
        )
    }

    private fun createComparePreviewsTool(): RegisteredTool {
        val input = Tool.Input(
            properties = buildJsonObject {
                put("previewId", buildJsonObject {
                    put("type", "string")
                    put("description", "Primary preview identifier to compare.")
                })
                put("baselineTimestamp", buildJsonObject {
                    put("type", "number")
                    put("description", "Optional timestamp of a historical baseline snapshot to compare against.")
                })
                put("compareWithPreviewId", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional second preview ID to compare against instead of a baseline snapshot.")
                })
                put("saveCurrentAsBaseline", buildJsonObject {
                    put("type", "boolean")
                    put("description", "If true, updates the baseline snapshot with the current render.")
                })
            },
            required = listOf("previewId")
        )
        val tool = Tool(
            name = "compare_previews",
            description = "Compares a rendered preview against a baseline snapshot or another preview, " +
                "reporting visual diff percentage and overlay image.",
            inputSchema = input
        )

        return RegisteredTool(tool) { request ->
            handleComparePreviews(request)
        }
    }

    private fun handleComparePreviews(request: CallToolRequest): CallToolResult {
        val previewId = request.arguments?.get("previewId")?.jsonPrimitive?.contentOrNull
        if (previewId.isNullOrBlank()) {
            return CallToolResult(
                content = listOf(TextContent(text = "Error: 'previewId' argument is required.")),
                isError = true
            )
        }

        val currentFile = resolveImageFile(previewId)
        if (currentFile == null || !currentFile.exists() || !currentFile.isFile) {
            return CallToolResult(
                content = listOf(
                    TextContent(
                        text = "Current preview image not found for '$previewId'. " +
                            "Please execute render_preview first."
                    )
                ),
                isError = true
            )
        }

        val compareWithId = request.arguments?.get("compareWithPreviewId")?.jsonPrimitive?.contentOrNull
        val timestamp = request.arguments?.get("baselineTimestamp")?.jsonPrimitive?.longOrNull
        val saveCurrent = request.arguments?.get("saveCurrentAsBaseline")?.jsonPrimitive?.booleanOrNull ?: false

        val baseline = resolveBaseline(previewId, compareWithId, timestamp)
            ?: return CallToolResult(
                content = listOf(
                    TextContent(text = "Target comparison preview '$compareWithId' has no rendered image.")
                ),
                isError = true
            )

        return executeComparison(previewId, currentFile.readBytes(), baseline, saveCurrent)
    }

    private fun resolveBaseline(
        previewId: String,
        compareWithPreviewId: String?,
        baselineTimestamp: Long?
    ): Pair<ByteArray?, String>? {
        if (!compareWithPreviewId.isNullOrBlank()) {
            val otherFile = resolveImageFile(compareWithPreviewId)
            if (otherFile == null || !otherFile.exists()) return null
            return Pair(otherFile.readBytes(), "preview '$compareWithPreviewId'")
        }
        val bytes = baselineManager.loadBaseline(previewId, baselineTimestamp)
        val desc = if (baselineTimestamp != null) {
            "historical baseline snapshot ($baselineTimestamp)"
        } else {
            "default baseline snapshot"
        }
        return Pair(bytes, desc)
    }

    private fun executeComparison(
        previewId: String,
        currentBytes: ByteArray,
        baseline: Pair<ByteArray?, String>,
        saveCurrent: Boolean
    ): CallToolResult {
        val (baselineBytes, baselineSourceDesc) = baseline
        if (baselineBytes == null) {
            baselineManager.saveBaseline(previewId, currentBytes)
            val initialPayload = buildJsonObject {
                put("hasVisualDifferences", false)
                put("differencePercentage", 0.0)
                put(
                    "summary",
                    "No existing baseline found for preview '$previewId'. Current render saved as initial baseline."
                )
                put("baselineEstablished", true)
            }
            return CallToolResult(
                content = listOf(TextContent(text = json.encodeToString(initialPayload)))
            )
        }

        val diffResult = VisualDiffEngine.compareImages(currentBytes, baselineBytes)
        if (saveCurrent) {
            baselineManager.saveBaseline(previewId, currentBytes)
        }

        val resultPayload = buildJsonObject {
            put("previewId", previewId)
            put("comparedAgainst", baselineSourceDesc)
            put("hasVisualDifferences", diffResult.hasVisualDifferences)
            put("differencePercentage", diffResult.differencePercentage)
            put("pixelsDifferent", diffResult.pixelCountDifferent)
            put("totalPixels", diffResult.totalPixels)
            put("summary", diffResult.summary)
        }

        val contents = mutableListOf<PromptMessageContent>()
        contents.add(TextContent(text = json.encodeToString(resultPayload)))

        if (diffResult.hasVisualDifferences && diffResult.diffImageBase64 != null) {
            contents.add(ImageContent(data = diffResult.diffImageBase64, mimeType = "image/png"))
        }

        return CallToolResult(content = contents)
    }

    private fun resolveImageFile(previewId: String): File? {
        val item = previewRegistry.getCatalog().previews[previewId]
        val sanitized = previewId.replace(':', '_').replace('#', '_').replace('.', '_')
        val candidatePaths = listOfNotNull(
            item?.imagePath?.let { File(it) },
            item?.imagePath?.let { File(projectRoot, it) },
            File(projectRoot, ".compositor/previews/$sanitized.png"),
            File(projectRoot, ".compositor/previews/${BaselineManager.sanitizeId(previewId)}.png")
        )
        return candidatePaths.firstOrNull { it.exists() && it.isFile }
    }

    private fun elementBoundsToJson(bounds: ElementBounds): JsonObject = buildJsonObject {
        put("className", bounds.className)
        put("left", bounds.left)
        put("top", bounds.top)
        put("width", bounds.width)
        put("height", bounds.height)
        put("children", JsonArray(bounds.children.map { elementBoundsToJson(it) }))
    }
}
