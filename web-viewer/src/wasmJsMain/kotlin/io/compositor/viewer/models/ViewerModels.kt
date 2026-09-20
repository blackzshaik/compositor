package io.compositor.viewer.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Strong-typed parameters extracted from `@Preview` annotation attributes.
 */
@Serializable
data class PreviewParameters(
    val name: String? = null,
    val group: String? = null,
    val widthDp: Int? = null,
    val heightDp: Int? = null,
    val uiMode: String? = null,
    val fontScale: Float? = null,
    val showBackground: Boolean? = null,
    val backgroundColor: String? = null,
    val apiLevel: Int? = null,
    val locale: String? = null
)

/**
 * Definition of a discovered `@Preview` composable function.
 */
@Serializable
data class PreviewDefinition(
    val functionName: String,
    val enclosingClass: String? = null,
    val packageName: String,
    val parameters: PreviewParameters = PreviewParameters(),
    val line: Int,
    val filePath: String
)

/**
 * Lifecycle state for a preview.
 */
@Serializable
enum class PreviewRenderStatus {
    @SerialName("Pending")
    PENDING,
    @SerialName("Rendering")
    RENDERING,
    @SerialName("Rendered")
    RENDERED,
    @SerialName("Error")
    ERROR
}

/**
 * Catalog entry tracking rendering status, timing, and image artifacts.
 */
@Serializable
data class PreviewItem(
    val id: String,
    val module: String,
    val definition: PreviewDefinition,
    val status: PreviewRenderStatus = PreviewRenderStatus.PENDING,
    val durationMs: Long? = null,
    val lastRenderedAt: Long? = null,
    val errorDetails: String? = null,
    val imagePath: String? = null,
    val imageUrl: String? = null
)

/**
 * Aggregated catalog indexing all previews by identifier, module, file, and group.
 */
@Serializable
data class PreviewCatalog(
    val previews: Map<String, PreviewItem> = emptyMap(),
    val byModule: Map<String, List<String>> = emptyMap(),
    val byFile: Map<String, List<String>> = emptyMap(),
    val byGroup: Map<String, List<String>> = emptyMap(),
    val totalCount: Int = 0
)

/**
 * WebSocket event message envelope from the daemon.
 */
@Serializable
data class DaemonWsMessage(
    val event: String,
    val type: String = event,
    val payload: JsonObject? = null
)

/**
 * Response payload returned after triggering an on-demand render.
 */
@Serializable
data class RenderResponse(
    val success: Boolean,
    val previewId: String,
    val preview: PreviewItem? = null,
    val error: String? = null
)

/**
 * Element coordinate bounds for the LayoutLib element inspector.
 */
@Serializable
data class ElementBounds(
    val className: String,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val children: List<ElementBounds> = emptyList()
)

/**
 * Supported hardware bezel profiles.
 */
enum class DeviceProfile(
    val displayName: String,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val cornerRadiusDp: Int,
    val hasPunchHole: Boolean
) {
    PIXEL_8("Pixel 8", 412, 915, 36, true),
    GALAXY_S24_ULTRA("Galaxy S24 Ultra", 412, 919, 12, true),
    TABLET_10("Tablet 10\"", 800, 1280, 18, true),
    FRAMELESS("Frameless", 412, 892, 0, false)
}

/**
 * Canvas layout modes: Single focused device or Multi-Preview matrix grid.
 */
enum class ViewMode {
    SINGLE,
    MATRIX
}

/**
 * Multi-preview comparison presets.
 */
enum class MatrixPreset(val label: String) {
    THEME("Theme Comparison (Light / Dark)"),
    FONT_SCALE("Font Scale Matrix (0.85x - 1.5x)"),
    GROUP("Group Previews")
}

/**
 * Hoisted top-bar configuration state.
 */
data class InspectorConfig(
    val isDarkTheme: Boolean = true,
    val fontScale: Float = 1.0f,
    val isLandscape: Boolean = false
)

/**
 * Structured diagnostic information for render/compilation errors.
 */
data class ErrorDiagnostic(
    val title: String,
    val message: String,
    val filePath: String? = null,
    val lineNumber: Int? = null,
    val stackTrace: String? = null,
    val rawDetails: String = ""
)
