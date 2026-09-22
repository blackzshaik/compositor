package io.compositor.renderer

import kotlinx.serialization.Serializable
import java.io.File

/**
 * Screen orientation options for preview rendering.
 */
enum class Orientation {
    PORTRAIT,
    LANDSCAPE
}

/**
 * Device configuration defining screen resolution, pixel density, orientation, and theme.
 */
data class CompositorDeviceConfig(
    val name: String = "Pixel 5",
    val widthPx: Int = 1080,
    val heightPx: Int = 2340,
    val dpi: Int = 440,
    val orientation: Orientation = Orientation.PORTRAIT,
    val theme: String = "android:Theme.Material.NoActionBar"
) {
    /**
     * Effective rendering width considering orientation.
     */
    val effectiveWidth: Int
        get() = if (orientation == Orientation.PORTRAIT) widthPx else heightPx

    /**
     * Effective rendering height considering orientation.
     */
    val effectiveHeight: Int
        get() = if (orientation == Orientation.PORTRAIT) heightPx else widthPx

    /**
     * Returns a copy with the specified orientation.
     */
    fun withOrientation(newOrientation: Orientation): CompositorDeviceConfig =
        copy(orientation = newOrientation)

    companion object {
        val PIXEL_5: CompositorDeviceConfig = CompositorDeviceConfig(
            name = "Pixel 5",
            widthPx = 1080,
            heightPx = 2340,
            dpi = 440,
            orientation = Orientation.PORTRAIT,
            theme = "android:Theme.Material.NoActionBar"
        )

        val PIXEL_7: CompositorDeviceConfig = CompositorDeviceConfig(
            name = "Pixel 7",
            widthPx = 1080,
            heightPx = 2400,
            dpi = 416,
            orientation = Orientation.PORTRAIT,
            theme = "android:Theme.Material.NoActionBar"
        )

        val TABLET_10: CompositorDeviceConfig = CompositorDeviceConfig(
            name = "Nexus 10 Tablet",
            widthPx = 1600,
            heightPx = 2560,
            dpi = 320,
            orientation = Orientation.LANDSCAPE,
            theme = "android:Theme.Material.NoActionBar"
        )
    }
}

/**
 * Element coordinate bounds extracted from the LayoutLib render hierarchy.
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
 * Request payload specifying the target Composable and rendering environment.
 */
data class RenderRequest(
    val composableId: String,
    val className: String,
    val methodName: String,
    val classpath: List<File> = emptyList(),
    val resourceDirs: List<File> = emptyList(),
    val deviceConfig: CompositorDeviceConfig = CompositorDeviceConfig.PIXEL_5,
    val outputFile: File
)

/**
 * Structured result of a headless rendering pass.
 */
sealed interface RenderResult {
    /**
     * Successful rasterization with image path and bounds metadata.
     */
    data class Success(
        val imageFile: File,
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val rootBounds: ElementBounds? = null
    ) : RenderResult

    /**
     * Failed render with diagnostic context and error details.
     */
    data class Failure(
        val errorMessage: String,
        val cause: Throwable? = null,
        val stackTrace: String = ""
    ) : RenderResult
}
