package io.compositor.parser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Strongly-typed parameters extracted from `@Preview` annotation attributes.
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
 * Rendering lifecycle state for a discovered preview.
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
 * Stateful catalog entry tracking rendering status, timing, and image artifacts.
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
 * Payload applied to update a preview's render state and timing metrics.
 */
@Serializable
data class RenderStateUpdate(
    val status: PreviewRenderStatus,
    val durationMs: Long? = null,
    val imagePath: String? = null,
    val imageUrl: String? = null,
    val errorDetails: String? = null
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
