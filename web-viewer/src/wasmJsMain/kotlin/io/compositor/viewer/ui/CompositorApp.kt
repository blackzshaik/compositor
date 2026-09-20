package io.compositor.viewer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.compositor.viewer.models.ViewMode
import io.compositor.viewer.net.CompositorClient
import io.compositor.viewer.state.CompositorState
import io.compositor.viewer.ui.canvas.PreviewCanvas
import io.compositor.viewer.ui.diagnostics.ErrorDiagnosticCard
import io.compositor.viewer.ui.inspector.ElementInspectorOverlay
import io.compositor.viewer.ui.inspector.ElementInspectorSheet
import io.compositor.viewer.ui.matrix.PreviewMatrixGrid
import io.compositor.viewer.ui.sidebar.PreviewSidebar
import io.compositor.viewer.ui.topbar.InspectorTopBar
import kotlinx.coroutines.launch

/**
 * Root Compose Multiplatform App container for Compositor Web Viewer.
 */
@Composable
fun CompositorApp() {
    val state = remember { CompositorState() }
    val client = remember { CompositorClient() }
    val scope = rememberCoroutineScope()

    val colorScheme = if (state.inspectorConfig.isDarkTheme) {
        darkColorScheme(
            background = Color(0xFF121214),
            surface = Color(0xFF1E1E22),
            primary = Color(0xFF8AB4F8),
            surfaceVariant = Color(0xFF28282D)
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF8F9FA),
            surface = Color(0xFFFFFFFF),
            primary = Color(0xFF1A73E8),
            surfaceVariant = Color(0xFFECEFF1)
        )
    }

    // Connect to daemon and fetch initial catalog
    LaunchedEffect(Unit) {
        try {
            val catalog = client.fetchCatalog()
            state.catalog = catalog
            state.isLoadingCatalog = false
            if (state.selectedPreviewId == null && catalog.previews.isNotEmpty()) {
                state.selectPreview(catalog.previews.keys.first())
            }
        } catch (_: Exception) {
            state.isLoadingCatalog = false
        }

        launch {
            client.listenWebSocket(
                onEvent = { state.handleWsEvent(it) },
                onStatusChange = { state.isConnected = it }
            )
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                InspectorTopBar(
                    isConnected = state.isConnected,
                    viewMode = state.viewMode,
                    deviceProfile = state.deviceProfile,
                    zoomScale = state.zoomScale,
                    isDarkTheme = state.inspectorConfig.isDarkTheme,
                    fontScale = state.inspectorConfig.fontScale,
                    isLandscape = state.inspectorConfig.isLandscape,
                    isInspectorMode = state.isInspectorMode,
                    isSidebarExpanded = state.isSidebarExpanded,
                    onToggleSidebar = { state.isSidebarExpanded = !state.isSidebarExpanded },
                    onViewModeChange = { state.viewMode = it },
                    onDeviceProfileChange = { state.deviceProfile = it },
                    onZoomChange = { state.setZoom(it) },
                    onResetZoom = { state.resetZoomPan() },
                    onToggleTheme = { state.toggleTheme() },
                    onFontScaleChange = { state.setFontScale(it) },
                    onToggleOrientation = { state.toggleOrientation() },
                    onToggleInspector = { state.toggleInspectorMode() },
                    onRefresh = {
                        state.selectedPreviewId?.let { id ->
                            scope.launch {
                                state.isRendering = true
                                client.triggerRender(id)
                            }
                        }
                    }
                )

                // Main workspace
                Row(modifier = Modifier.weight(1f)) {
                    // Navigation Sidebar
                    AnimatedVisibility(visible = state.isSidebarExpanded) {
                        PreviewSidebar(
                            previews = state.filteredPreviews,
                            selectedPreviewId = state.selectedPreviewId,
                            searchQuery = state.searchQuery,
                            isLoading = state.isLoadingCatalog,
                            onSearchChange = { state.searchQuery = it },
                            onSelectPreview = { state.selectPreview(it) }
                        )
                    }

                    // Canvas Viewport or Matrix Grid
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.viewMode == ViewMode.SINGLE) {
                            val activeItem = state.selectedPreview
                            val imgUrl = activeItem?.let { client.getPreviewImageUrl(it.id) }
                            PreviewCanvas(
                                previewItem = activeItem,
                                imageUrl = imgUrl,
                                deviceProfile = state.deviceProfile,
                                isLandscape = state.inspectorConfig.isLandscape,
                                zoomScale = state.zoomScale,
                                panOffsetX = state.panOffsetX,
                                panOffsetY = state.panOffsetY,
                                isRendering = state.isRendering,
                                onPan = { dx, dy ->
                                    state.panOffsetX += dx
                                    state.panOffsetY += dy
                                },
                                overlayContent = {
                                    ElementInspectorOverlay(
                                        rootBounds = null,
                                        isInspectorMode = state.isInspectorMode,
                                        hoveredBounds = state.hoveredBounds,
                                        selectedBounds = state.selectedBounds,
                                        onHoverElement = { state.hoveredBounds = it },
                                        onSelectElement = { state.selectedBounds = it }
                                    )
                                }
                            )
                        } else {
                            PreviewMatrixGrid(
                                selectedPreview = state.selectedPreview,
                                allPreviews = state.catalog.previews.values.toList(),
                                preset = state.matrixPreset,
                                zoomScale = state.zoomScale,
                                panOffsetX = state.panOffsetX,
                                panOffsetY = state.panOffsetY,
                                isRendering = state.isRendering,
                                onPan = { dx, dy ->
                                    state.panOffsetX += dx
                                    state.panOffsetY += dy
                                },
                                onSelectPreview = { id ->
                                    state.selectPreview(id)
                                    state.viewMode = ViewMode.SINGLE
                                },
                                onPresetChange = { state.matrixPreset = it },
                                getImageUrl = { id -> client.getPreviewImageUrl(id) }
                            )
                        }
                    }

                    // Element Inspector Sheet
                    AnimatedVisibility(visible = state.isInspectorMode && state.selectedBounds != null) {
                        ElementInspectorSheet(
                            element = state.selectedBounds,
                            onClose = { state.selectedBounds = null }
                        )
                    }
                }
            }

            // Resilient Error Diagnostic Boundary Card
            ErrorDiagnosticCard(
                diagnostic = state.activeError,
                onDismiss = { state.dismissError() }
            )
        }
    }
}
