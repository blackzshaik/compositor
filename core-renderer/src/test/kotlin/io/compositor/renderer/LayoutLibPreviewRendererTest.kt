package io.compositor.renderer

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LayoutLibPreviewRendererTest {

    @Test
    fun `render GreetingPreview from sample app succeeds`() {
        val sampleClassesDir = resolveSampleClassesDir()
        if (sampleClassesDir == null || !sampleClassesDir.exists()) {
            println("Skipping integration test: sample classes directory not found")
            return
        }

        val sampleResDir = resolveSampleResourcesDir()
        val sampleRJar = resolveSampleRJar()
        val outputFile = File("build/test-previews/greeting_preview.png")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val classpathList = listOfNotNull(
            sampleClassesDir,
            sampleRJar?.takeIf { it.exists() }
        )

        val request = RenderRequest(
            composableId = "com.compositor.sample.GreetingKt.GreetingPreview",
            className = "com.compositor.sample.GreetingKt",
            methodName = "GreetingPreview",
            classpath = classpathList,
            resourceDirs = listOfNotNull(sampleResDir?.takeIf { it.exists() }),
            deviceConfig = CompositorDeviceConfig.PIXEL_5,
            outputFile = outputFile
        )

        val renderer = LayoutLibPreviewRenderer()
        val result = renderer.render(request)

        when (result) {
            is RenderResult.Success -> {
                assertTrue(result.imageFile.exists(), "Output PNG should exist on disk")
                assertTrue(result.imageFile.length() > 5000, "Image size should exceed 5KB")
                assertTrue(result.width > 0, "Image width should be positive")
                assertTrue(result.height > 0, "Image height should be positive")
                assertTrue(result.durationMs >= 0, "Duration should be non-negative")
                assertNotNull(result.rootBounds, "Root bounds should be captured")
            }
            is RenderResult.Failure -> {
                println("Render failed with: ${result.errorMessage}")
                println(result.stackTrace)
                throw AssertionError("Rendering failed: ${result.errorMessage}", result.cause)
            }
        }
    }

    @Test
    fun `render with non-existent method returns failure cleanly`() {
        val outputFile = File("build/test-previews/failed_preview.png")
        val request = RenderRequest(
            composableId = "invalid_id",
            className = "com.compositor.sample.GreetingKt",
            methodName = "NonExistentMethod",
            classpath = emptyList(),
            resourceDirs = emptyList(),
            deviceConfig = CompositorDeviceConfig.PIXEL_5,
            outputFile = outputFile
        )

        val renderer = LayoutLibPreviewRenderer()
        val result = renderer.render(request)

        assertTrue(result is RenderResult.Failure, "Expected failure for invalid method")
        val failure = result as RenderResult.Failure
        assertFalse(failure.errorMessage.isBlank())
    }

    private fun resolveSampleClassesDir(): File? {
        val propertyPath = System.getProperty("compositor.sample.classes.dir")
        if (!propertyPath.isNullOrBlank()) {
            val dir = File(propertyPath)
            if (dir.exists()) return dir
        }

        val fallbackCandidates = listOf(
            File("../samples/sample-app/build/tmp/kotlin-classes/debug"),
            File("samples/sample-app/build/tmp/kotlin-classes/debug")
        )
        return fallbackCandidates.firstOrNull { it.exists() }
    }

    private fun resolveSampleResourcesDir(): File? {
        val propertyPath = System.getProperty("compositor.sample.resources.dir")
        if (!propertyPath.isNullOrBlank()) {
            val dir = File(propertyPath)
            if (dir.exists()) return dir
        }

        val fallbackCandidates = listOf(
            File("../samples/sample-app/build/intermediates/merged_res/debug/mergeDebugResources/merged.dir"),
            File("samples/sample-app/build/intermediates/merged_res/debug/mergeDebugResources/merged.dir")
        )
        return fallbackCandidates.firstOrNull { it.exists() }
    }

    private fun resolveSampleRJar(): File? {
        val propertyPath = System.getProperty("compositor.sample.rjar")
        if (!propertyPath.isNullOrBlank()) {
            val file = File(propertyPath)
            if (file.exists()) return file
        }

        val rJarSubpath =
            "build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"
        val fallbackCandidates = listOf(
            File("../samples/sample-app/$rJarSubpath"),
            File("samples/sample-app/$rJarSubpath")
        )
        return fallbackCandidates.firstOrNull { it.exists() }
    }
}
