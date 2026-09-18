package io.compositor.renderer

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LayoutLibBootstrapTest {

    @Test
    fun `resolvePlatformName returns known OS identifier`() {
        val platform = LayoutLibBootstrap.resolvePlatformName()

        assertTrue(
            platform in listOf("win", "mac", "mac-arm", "linux"),
            "Expected known platform, but got: $platform"
        )
    }

    @Test
    fun `getRuntimeCacheDir points to compositor layoutlib directory`() {
        val cacheDir = LayoutLibBootstrap.getRuntimeCacheDir()

        assertNotNull(cacheDir)
        assertTrue(cacheDir.path.contains(".compositor"))
        assertTrue(cacheDir.path.contains("14.0.11"))
    }
}
