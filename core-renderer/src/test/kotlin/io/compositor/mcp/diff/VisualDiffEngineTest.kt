package io.compositor.mcp.diff

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class VisualDiffEngineTest {

    @Test
    fun `identical images report zero differences`() {
        val img1 = createSolidImage(100, 100, Color.BLUE)
        val img2 = createSolidImage(100, 100, Color.BLUE)

        val result = VisualDiffEngine.compareBufferedImages(img1, img2)

        assertFalse(result.hasVisualDifferences)
        assertEquals(0.0, result.differencePercentage)
        assertEquals(0L, result.pixelCountDifferent)
        assertEquals(10000L, result.totalPixels)
        assertTrue(result.summary.contains("0.0% difference"))
    }

    @Test
    fun `modified image detects exact difference percentage and diff image`() {
        val img1 = createSolidImage(100, 100, Color.WHITE)
        val img2 = createSolidImage(100, 100, Color.WHITE)

        // Draw a 10x10 red square (100 pixels out of 10000 = 1.0%)
        val g = img2.createGraphics()
        try {
            g.color = Color.RED
            g.fillRect(10, 10, 10, 10)
        } finally {
            g.dispose()
        }

        val result = VisualDiffEngine.compareBufferedImages(img1, img2)

        assertTrue(result.hasVisualDifferences)
        assertEquals(1.0, result.differencePercentage)
        assertEquals(100L, result.pixelCountDifferent)
        assertNotNull(result.diffImageBase64)
        assertTrue(result.summary.contains("1.0% difference"))
    }

    @Test
    fun `images with mismatched dimensions are normalized and compared without crashing`() {
        val img1 = createSolidImage(50, 50, Color.GREEN)
        val img2 = createSolidImage(100, 100, Color.GREEN)

        val result = VisualDiffEngine.compareBufferedImages(img1, img2)

        assertTrue(result.hasVisualDifferences)
        assertEquals(10000L, result.totalPixels)
        assertNotNull(result.diffImageBase64)
    }

    @Test
    fun `compareImages handles raw PNG byte arrays`() {
        val img1 = createSolidImage(40, 40, Color.CYAN)
        val img2 = createSolidImage(40, 40, Color.CYAN)

        val bytes1 = toPngBytes(img1)
        val bytes2 = toPngBytes(img2)

        val result = VisualDiffEngine.compareImages(bytes1, bytes2)

        assertFalse(result.hasVisualDifferences)
        assertEquals(0.0, result.differencePercentage)
    }

    private fun createSolidImage(width: Int, height: Int, color: Color): BufferedImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        try {
            g.color = color
            g.fillRect(0, 0, width, height)
        } finally {
            g.dispose()
        }
        return img
    }

    private fun toPngBytes(image: BufferedImage): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", baos)
        return baos.toByteArray()
    }
}
