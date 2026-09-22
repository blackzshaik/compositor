package io.compositor.viewer.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.compositor.viewer.models.DaemonWsMessage
import io.compositor.viewer.models.DeviceProfile
import io.compositor.viewer.models.ElementBounds
import io.compositor.viewer.models.ErrorDiagnostic
import io.compositor.viewer.models.InspectorConfig
import io.compositor.viewer.models.MatrixPreset
import io.compositor.viewer.models.PreviewCatalog
import io.compositor.viewer.models.PreviewItem
import io.compositor.viewer.models.PreviewRenderStatus
import io.compositor.viewer.models.ViewMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Central hoisted state holder for the Compositor Web Viewer.
 */
class CompositorState {
    private val json = Json { ignoreUnknownKeys = true }
    var catalog: PreviewCatalog by mutableStateOf(PreviewCatalog())
    var selectedPreviewId: String? by mutableStateOf(null)
    var searchQuery: String by mutableStateOf("")

    var isConnected: Boolean by mutableStateOf(false)
    var isSidebarExpanded: Boolean by mutableStateOf(true)
    var isInspectorMode: Boolean by mutableStateOf(false)
    var selectedBounds: ElementBounds? by mutableStateOf(null)
    var hoveredBounds: ElementBounds? by mutableStateOf(null)
    var activeError: ErrorDiagnostic? by mutableStateOf(null)

    var deviceProfile: DeviceProfile by mutableStateOf(DeviceProfile.PIXEL_8)
    var viewMode: ViewMode by mutableStateOf(ViewMode.SINGLE)
    var matrixPreset: MatrixPreset by mutableStateOf(MatrixPreset.THEME)
    var inspectorConfig: InspectorConfig by mutableStateOf(InspectorConfig())

    var zoomScale: Float by mutableFloatStateOf(0.75f)
    var panOffsetX: Float by mutableFloatStateOf(0f)
    var panOffsetY: Float by mutableFloatStateOf(0f)

    var isRendering: Boolean by mutableStateOf(false)
    var isLoadingCatalog: Boolean by mutableStateOf(true)
    var lastImageUpdateEpoch: Long by mutableLongStateOf(0L)

    val selectedPreview: PreviewItem?
        get() = selectedPreviewId?.let { catalog.previews[it] }

    val filteredPreviews by derivedStateOf {
        val query = searchQuery.trim().lowercase()
        val all = catalog.previews.values.toList()
        if (query.isEmpty()) {
            all
        } else {
            all.filter { item ->
                item.definition.functionName.lowercase().contains(query) ||
                    (item.definition.enclosingClass?.lowercase()?.contains(query) == true) ||
                    item.module.lowercase().contains(query) ||
                    (item.definition.parameters.group?.lowercase()?.contains(query) == true) ||
                    item.definition.filePath.lowercase().contains(query)
            }
        }
    }

    /**
     * Updates the active preview selection.
     */
    fun selectPreview(id: String) {
        selectedPreviewId = id
        selectedBounds = null
        hoveredBounds = null
        activeError = null
    }

    /**
     * Toggles light and dark UI themes.
     */
    fun toggleTheme() {
        inspectorConfig = inspectorConfig.copy(isDarkTheme = !inspectorConfig.isDarkTheme)
    }

    /**
     * Updates the typography font scale (e.g. 0.85x - 1.5x).
     */
    fun setFontScale(scale: Float) {
        inspectorConfig = inspectorConfig.copy(fontScale = scale)
    }

    /**
     * Flips between portrait and landscape orientations.
     */
    fun toggleOrientation() {
        inspectorConfig = inspectorConfig.copy(isLandscape = !inspectorConfig.isLandscape)
    }

    /**
     * Sets zoom scale, constrained between 0.25x and 3.0x.
     */
    fun setZoom(scale: Float) {
        zoomScale = scale.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /**
     * Resets canvas zoom and translation to standard viewport center.
     */
    fun resetZoomPan() {
        zoomScale = 0.75f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    /**
     * Toggles element bounds inspection mode.
     */
    fun toggleInspectorMode() {
        isInspectorMode = !isInspectorMode
        if (!isInspectorMode) {
            selectedBounds = null
            hoveredBounds = null
        }
    }

    /**
     * Dismisses the active error diagnostic card.
     */
    fun dismissError() {
        activeError = null
    }

    /**
     * Dispatches incoming daemon WebSocket messages.
     */
    @Suppress("CyclomaticComplexMethod")
    fun handleWsEvent(msg: DaemonWsMessage) {
        val payload = msg.payload
        when (msg.event) {
            "PREVIEW_RENDER_STARTED" -> {
                val id = payload?.get("previewId")?.jsonPrimitive?.contentOrNull
                if (id == null || id == selectedPreviewId) {
                    isRendering = true
                }
            }
            "PREVIEW_UPDATED" -> {
                val id = payload?.get("previewId")?.jsonPrimitive?.contentOrNull
                isRendering = false
                val ts = payload?.get("timestamp")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                lastImageUpdateEpoch = if (ts != null && ts > lastImageUpdateEpoch) ts else lastImageUpdateEpoch + 1L
                if (id == null || id == selectedPreviewId) {
                    activeError = null
                }
                val previewJson = payload?.get("preview")
                if (previewJson != null && id != null) {
                    try {
                        val parsed = json.decodeFromJsonElement<PreviewItem>(previewJson)
                        val updatedMap = catalog.previews.toMutableMap().apply { put(id, parsed) }
                        catalog = catalog.copy(previews = updatedMap)
                    } catch (_: Exception) {
                        updatePreviewStatusInCatalog(id, PreviewRenderStatus.RENDERED)
                    }
                } else {
                    updatePreviewStatusInCatalog(id, PreviewRenderStatus.RENDERED)
                }
            }
            "RENDER_ERROR" -> {
                isRendering = false
                val id = payload?.get("previewId")?.jsonPrimitive?.contentOrNull
                val errorText = payload?.get("error")?.jsonPrimitive?.contentOrNull ?: "Render failed"
                if (id == null || id == selectedPreviewId) {
                    activeError = parseDiagnosticError(errorText)
                }
                updatePreviewStatusInCatalog(id, PreviewRenderStatus.ERROR)
            }
        }
    }

    private fun updatePreviewStatusInCatalog(id: String?, status: PreviewRenderStatus) {
        if (id == null) return
        val currentItem = catalog.previews[id] ?: return
        val updatedItem = currentItem.copy(status = status)
        val updatedMap = catalog.previews.toMutableMap().apply { put(id, updatedItem) }
        catalog = catalog.copy(previews = updatedMap)
    }

    private fun parseDiagnosticError(raw: String): ErrorDiagnostic {
        val lines = raw.lines()
        val firstLine = lines.firstOrNull { it.isNotBlank() } ?: "Unknown rendering failure"

        val title = when {
            raw.contains("Unresolved reference", ignoreCase = true) -> "Unresolved Reference"
            raw.contains("MissingResource", ignoreCase = true) -> "Missing Resource Exception"
            raw.contains("ClassNotFound", ignoreCase = true) -> "Class Not Found"
            raw.contains("Compilation", ignoreCase = true) -> "Compilation Error"
            else -> "Render Diagnostic Error"
        }

        val fileMatch = FILE_LINE_REGEX.find(raw)
        val filePath = fileMatch?.groupValues?.getOrNull(1)
        val lineNumber = fileMatch?.groupValues?.getOrNull(2)?.toIntOrNull()

        return ErrorDiagnostic(
            title = title,
            message = firstLine,
            filePath = filePath,
            lineNumber = lineNumber,
            stackTrace = if (lines.size > 1) lines.drop(1).joinToString("\n") else null,
            rawDetails = raw
        )
    }

    companion object {
        const val MIN_ZOOM = 0.25f
        const val MAX_ZOOM = 3.00f
        private val FILE_LINE_REGEX = Regex("""([a-zA-Z0-9_\-/\\]+\.kt):?(\d+)?""")
    }
}
