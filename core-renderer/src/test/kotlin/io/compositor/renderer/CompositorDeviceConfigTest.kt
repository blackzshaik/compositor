package io.compositor.renderer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CompositorDeviceConfigTest {

    @Test
    fun `pixel 5 preset has valid portrait dimensions`() {
        val config = CompositorDeviceConfig.PIXEL_5

        assertEquals("Pixel 5", config.name)
        assertEquals(1080, config.widthPx)
        assertEquals(2340, config.heightPx)
        assertEquals(440, config.dpi)
        assertEquals(Orientation.PORTRAIT, config.orientation)
        assertEquals(1080, config.effectiveWidth)
        assertEquals(2340, config.effectiveHeight)
    }

    @Test
    fun `pixel 7 preset has valid portrait dimensions`() {
        val config = CompositorDeviceConfig.PIXEL_7

        assertEquals("Pixel 7", config.name)
        assertEquals(1080, config.widthPx)
        assertEquals(2400, config.heightPx)
        assertEquals(416, config.dpi)
        assertEquals(Orientation.PORTRAIT, config.orientation)
    }

    @Test
    fun `tablet 10 preset has valid landscape dimensions`() {
        val config = CompositorDeviceConfig.TABLET_10

        assertEquals("Nexus 10 Tablet", config.name)
        assertEquals(Orientation.LANDSCAPE, config.orientation)
        assertEquals(2560, config.effectiveWidth)
        assertEquals(1600, config.effectiveHeight)
    }

    @Test
    fun `withOrientation switches dimensions correctly`() {
        val portrait = CompositorDeviceConfig.PIXEL_5
        val landscape = portrait.withOrientation(Orientation.LANDSCAPE)

        assertEquals(Orientation.LANDSCAPE, landscape.orientation)
        assertEquals(2340, landscape.effectiveWidth)
        assertEquals(1080, landscape.effectiveHeight)
    }

    @Test
    fun `element bounds calculates correct width and height`() {
        val bounds = ElementBounds(
            className = "android.widget.TextView",
            left = 20,
            top = 30,
            width = 200,
            height = 50
        )

        assertEquals("android.widget.TextView", bounds.className)
        assertEquals(20, bounds.left)
        assertEquals(30, bounds.top)
        assertEquals(200, bounds.width)
        assertEquals(50, bounds.height)
        assertNotNull(bounds.children)
    }
}
