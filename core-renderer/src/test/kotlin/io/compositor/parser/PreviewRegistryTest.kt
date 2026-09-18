package io.compositor.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class PreviewRegistryTest {

    @Test
    fun `generates deterministic IDs properly`() {
        val registry = PreviewRegistry()
        val def1 = PreviewDefinition(
            functionName = "SimplePreview",
            packageName = "com.test",
            line = 10,
            filePath = "src/Test.kt"
        )
        assertEquals("app:com.test.SimplePreview", registry.generateId("app", def1))

        val def2 = PreviewDefinition(
            functionName = "InnerPreview",
            enclosingClass = "Container",
            packageName = "com.test",
            parameters = PreviewParameters(name = "Dark"),
            line = 25,
            filePath = "src/Test.kt"
        )
        assertEquals("sample:com.test.Container.InnerPreview#Dark", registry.generateId("sample", def2))
    }

    @Test
    fun `registers and updates file previews with state retention`() {
        val registry = PreviewRegistry()
        val def = PreviewDefinition(
            functionName = "SamplePreview",
            packageName = "com.sample",
            line = 12,
            filePath = "src/Sample.kt"
        )

        val items = registry.updateFilePreviews("src/Sample.kt", listOf(def), "app")
        assertEquals(1, items.size)
        assertEquals(PreviewRenderStatus.PENDING, items[0].status)

        // Mark rendered
        val id = items[0].id
        registry.updateRenderStatus(
            previewId = id,
            status = PreviewRenderStatus.RENDERED,
            durationMs = 450,
            imagePath = "/build/output.png"
        )

        val catalog = registry.getCatalog()
        assertEquals(PreviewRenderStatus.RENDERED, catalog.previews[id]?.status)
        assertEquals(450L, catalog.previews[id]?.durationMs)

        // Re-parse with same definition retains status
        val updated = registry.updateFilePreviews("src/Sample.kt", listOf(def), "app")
        assertEquals(PreviewRenderStatus.RENDERED, updated[0].status)
    }

    @Test
    fun `removes obsolete previews when definitions are deleted`() {
        val registry = PreviewRegistry()
        val def1 = PreviewDefinition(
            functionName = "PreviewA",
            packageName = "com.sample",
            line = 10,
            filePath = "src/Test.kt"
        )
        val def2 = PreviewDefinition(
            functionName = "PreviewB",
            packageName = "com.sample",
            line = 20,
            filePath = "src/Test.kt"
        )

        registry.updateFilePreviews("src/Test.kt", listOf(def1, def2))
        assertEquals(2, registry.getCatalog().totalCount)

        // File modified: PreviewB removed
        registry.updateFilePreviews("src/Test.kt", listOf(def1))
        val catalog = registry.getCatalog()
        assertEquals(1, catalog.totalCount)
        assertTrue(catalog.previews.containsKey("app:com.sample.PreviewA"))
    }

    @Test
    fun `removes all previews when file is deleted`() {
        val registry = PreviewRegistry()
        val def1 = PreviewDefinition(
            functionName = "Preview1",
            packageName = "com.sample",
            line = 5,
            filePath = "src/OldFile.kt"
        )
        registry.updateFilePreviews("src/OldFile.kt", listOf(def1))
        assertEquals(1, registry.getCatalog().totalCount)

        val removed = registry.removeFile("src/OldFile.kt")
        assertEquals(1, removed)
        assertEquals(0, registry.getCatalog().totalCount)
    }

    @Test
    fun `indexes previews by module, file, and group`() {
        val registry = PreviewRegistry()
        val def1 = PreviewDefinition(
            functionName = "HeaderPreview",
            packageName = "com.sample",
            parameters = PreviewParameters(group = "Headers"),
            line = 10,
            filePath = "src/Header.kt"
        )
        val def2 = PreviewDefinition(
            functionName = "FooterPreview",
            packageName = "com.sample",
            parameters = PreviewParameters(group = "Footers"),
            line = 15,
            filePath = "src/Footer.kt"
        )

        registry.updateFilePreviews("src/Header.kt", listOf(def1), "features")
        registry.updateFilePreviews("src/Footer.kt", listOf(def2), "features")

        val catalog = registry.getCatalog()
        assertEquals(2, catalog.totalCount)
        assertEquals(2, catalog.byModule["features"]?.size)
        assertEquals(1, catalog.byGroup["Headers"]?.size)
        assertEquals(1, catalog.byGroup["Footers"]?.size)
    }

    @Test
    fun `persists catalog to JSON and reloads successfully`() {
        val tempDir = Files.createTempDirectory("compositor_test").toFile()
        val storage = File(tempDir, "previews.json")

        try {
            val registry = PreviewRegistry(storage)
            val def = PreviewDefinition(
                functionName = "GreetingPreview",
                packageName = "com.compositor.sample",
                parameters = PreviewParameters(name = "Default Greeting", showBackground = true),
                line = 30,
                filePath = "src/Greeting.kt"
            )

            registry.updateFilePreviews("src/Greeting.kt", listOf(def))
            assertTrue(storage.exists())
            assertTrue(storage.length() > 0)

            // Reload into new registry instance
            val newRegistry = PreviewRegistry(storage)
            val catalog = newRegistry.getCatalog()
            assertEquals(1, catalog.totalCount)

            val item = catalog.previews.values.first()
            assertEquals("GreetingPreview", item.definition.functionName)
            assertEquals("Default Greeting", item.definition.parameters.name)
            assertEquals(true, item.definition.parameters.showBackground)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `loads existing compositor preview catalog if present`() {
        val rootPreviewsFile = File("../.compositor/previews.json")
        val altFile = File(".compositor/previews.json")
        val fileToTest = when {
            rootPreviewsFile.exists() -> rootPreviewsFile
            altFile.exists() -> altFile
            else -> null
        }

        if (fileToTest != null) {
            val registry = PreviewRegistry(fileToTest)
            val catalog = registry.getCatalog()
            assertTrue(catalog.totalCount >= 1)
            val greeting = catalog.previews.values.firstOrNull {
                it.definition.functionName == "GreetingPreview"
            }
            assertEquals("GreetingPreview", greeting?.definition?.functionName)
            assertEquals(PreviewRenderStatus.RENDERED, greeting?.status)
        }
    }
}
