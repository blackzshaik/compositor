package io.compositor.pipeline

import io.compositor.parser.PreviewRenderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class EndToEndPipelineTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `end to end pipeline scans renders and serves preview`() {
        val sampleClassesDir = resolveSampleClassesDir()
        val sampleResDir = resolveSampleResourcesDir()
        val sampleRJar = resolveSampleRJar()
        val sampleSrcDir = resolveSampleSrcDir()

        if (sampleClassesDir == null || !sampleClassesDir.exists() || sampleSrcDir == null || !sampleSrcDir.exists()) {
            println("Skipping EndToEndPipelineTest: sample app sources or classes not found")
            return
        }

        val testPort = 3088
        val outputDir = File(tempDir, "previews").apply { mkdirs() }

        val config = PipelineConfig(
            projectRoot = tempDir,
            port = testPort,
            watchRoots = listOf(sampleSrcDir),
            classesDirs = listOf(sampleClassesDir),
            resourceDirs = listOfNotNull(sampleResDir?.takeIf { it.exists() }),
            rJar = sampleRJar?.takeIf { it.exists() },
            outputDir = outputDir
        )

        val pipeline = CompositorPipeline(config)

        try {
            pipeline.start(autoRender = false)

            val catalog = pipeline.previewRegistry.getCatalog()
            assertTrue(catalog.totalCount > 0, "Initial scan should discover sample app previews")

            val greetingPreviewId = catalog.previews.keys.firstOrNull { it.contains("GreetingPreview") }
            assertNotNull(greetingPreviewId, "GreetingPreview must be discovered")

            // Initial render
            val initialItem = pipeline.renderPreview(greetingPreviewId!!)
            assertNotNull(initialItem, "Initial rendering should succeed")
            assertEquals(PreviewRenderStatus.RENDERED, initialItem?.status)

            // Incremental re-render (simulating live editor modification)
            val startTime = System.currentTimeMillis()
            val renderedItem = pipeline.renderPreview(greetingPreviewId)
            val duration = System.currentTimeMillis() - startTime

            assertNotNull(renderedItem, "Rendering should return updated preview item")
            assertEquals(PreviewRenderStatus.RENDERED, renderedItem?.status)
            val renderDuration = renderedItem?.durationMs ?: duration
            assertTrue(
                renderDuration < 5000,
                "Incremental rendering must complete in under 5s (was ${renderDuration}ms)"
            )

            val imageFile = File(renderedItem?.imagePath ?: "")
            assertTrue(imageFile.exists(), "Rendered PNG file must exist on disk")
            assertTrue(imageFile.length() > 5000, "Image size should exceed 5KB")

            verifyRestEndpoints(testPort, greetingPreviewId)
        } finally {
            pipeline.stop()
        }
    }

    private fun verifyRestEndpoints(testPort: Int, previewId: String) {
        val httpClient = HttpClient.newHttpClient()

        val statusReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$testPort/api/status"))
            .GET()
            .build()
        val statusResp = httpClient.send(statusReq, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, statusResp.statusCode())
        assertTrue(statusResp.body().contains("\"status\": \"running\""))

        val imageReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$testPort/api/previews/$previewId/image"))
            .GET()
            .build()
        val imageResp = httpClient.send(imageReq, HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(200, imageResp.statusCode())
        assertEquals("image/png", imageResp.headers().firstValue("Content-Type").orElse(""))
        assertTrue(imageResp.body().isNotEmpty(), "Image bytes should be returned")
    }

    private fun resolveSampleSrcDir(): File? {
        val candidates = listOf(
            File("../samples/sample-app/src/main/java"),
            File("samples/sample-app/src/main/java")
        )
        return candidates.firstOrNull { it.exists() }
    }

    private fun resolveSampleClassesDir(): File? {
        val propertyPath = System.getProperty("compositor.sample.classes.dir")
        if (!propertyPath.isNullOrBlank()) {
            val dir = File(propertyPath)
            if (dir.exists()) return dir
        }

        val candidates = listOf(
            File("../samples/sample-app/build/tmp/kotlin-classes/debug"),
            File("samples/sample-app/build/tmp/kotlin-classes/debug")
        )
        return candidates.firstOrNull { it.exists() }
    }

    private fun resolveSampleResourcesDir(): File? {
        val propertyPath = System.getProperty("compositor.sample.resources.dir")
        if (!propertyPath.isNullOrBlank()) {
            val dir = File(propertyPath)
            if (dir.exists()) return dir
        }

        val candidates = listOf(
            File("../samples/sample-app/build/intermediates/merged_res/debug/mergeDebugResources/merged.dir"),
            File("samples/sample-app/build/intermediates/merged_res/debug/mergeDebugResources/merged.dir")
        )
        return candidates.firstOrNull { it.exists() }
    }

    private fun resolveSampleRJar(): File? {
        val propertyPath = System.getProperty("compositor.sample.rjar")
        if (!propertyPath.isNullOrBlank()) {
            val file = File(propertyPath)
            if (file.exists()) return file
        }

        val rJarSubpath =
            "build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"
        val candidates = listOf(
            File("../samples/sample-app/$rJarSubpath"),
            File("samples/sample-app/$rJarSubpath")
        )
        return candidates.firstOrNull { it.exists() }
    }
}
