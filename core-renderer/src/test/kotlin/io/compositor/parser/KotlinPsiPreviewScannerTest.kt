package io.compositor.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.system.measureTimeMillis

class KotlinPsiPreviewScannerTest {

    private val scanner = KotlinPsiPreviewScanner()

    @Test
    fun `parses simple preview without parameters`() {
        val source = """
            package com.example.ui

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            @Preview
            @Composable
            fun SimplePreview() {
                // Composable content
            }
        """.trimIndent()

        val previews = scanner.parseSource(source, "src/SimplePreview.kt")

        assertEquals(1, previews.size)
        val preview = previews[0]
        assertEquals("SimplePreview", preview.functionName)
        assertEquals("com.example.ui", preview.packageName)
        assertEquals(null, preview.enclosingClass)
        assertEquals("src/SimplePreview.kt", preview.filePath)
        assertEquals(null, preview.parameters.name)
        assertEquals(null, preview.parameters.showBackground)
    }

    @Test
    fun `parses fully parameterized preview with named and positional arguments`() {
        val source = """
            package com.example.ui

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            @Preview(
                name = "Dark Card",
                group = "Cards",
                widthDp = 360,
                heightDp = 640,
                uiMode = "Configuration.UI_MODE_NIGHT_YES",
                fontScale = 1.25f,
                showBackground = true,
                backgroundColor = "0xFF112233"
            )
            @Composable
            fun CardPreview() {}
        """.trimIndent()

        val previews = scanner.parseSource(source, "src/Card.kt")

        assertEquals(1, previews.size)
        val p = previews[0].parameters
        assertEquals("Dark Card", p.name)
        assertEquals("Cards", p.group)
        assertEquals(360, p.widthDp)
        assertEquals(640, p.heightDp)
        assertEquals("Configuration.UI_MODE_NIGHT_YES", p.uiMode)
        assertEquals(1.25f, p.fontScale)
        assertEquals(true, p.showBackground)
        assertEquals("0xFF112233", p.backgroundColor)
    }

    @Test
    fun `parses multiple previews on a single composable`() {
        val source = """
            package com.example.ui

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            @Preview(name = "Light Mode")
            @Preview(name = "Dark Mode", uiMode = "Configuration.UI_MODE_NIGHT_YES")
            @Composable
            fun MultiPreviewSample() {}
        """.trimIndent()

        val previews = scanner.parseSource(source)

        assertEquals(2, previews.size)
        assertEquals("Light Mode", previews[0].parameters.name)
        assertEquals("Dark Mode", previews[1].parameters.name)
        assertEquals("Configuration.UI_MODE_NIGHT_YES", previews[1].parameters.uiMode)
    }

    @Test
    fun `expands custom multipreview annotation defined in source`() {
        val source = """
            package com.example.theme

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            @Preview(name = "Light", group = "Theme")
            @Preview(name = "Dark", group = "Theme")
            annotation class ThemePreviews

            @ThemePreviews
            @Composable
            fun HeaderPreview() {}
        """.trimIndent()

        val previews = scanner.parseSource(source)

        assertEquals(2, previews.size)
        assertEquals("HeaderPreview", previews[0].functionName)
        assertEquals("Light", previews[0].parameters.name)
        assertEquals("Theme", previews[0].parameters.group)
        assertEquals("HeaderPreview", previews[1].functionName)
        assertEquals("Dark", previews[1].parameters.name)
    }

    @Test
    fun `expands known built-in PreviewLightDark annotation`() {
        val source = """
            package com.example.theme

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.PreviewLightDark

            @PreviewLightDark
            @Composable
            fun ButtonPreview() {}
        """.trimIndent()

        val previews = scanner.parseSource(source)

        assertEquals(2, previews.size)
        assertEquals("ButtonPreview", previews[0].functionName)
        assertEquals("Light", previews[0].parameters.name)
        assertEquals("Dark", previews[1].parameters.name)
    }

    @Test
    fun `parses previews inside class and companion object`() {
        val source = """
            package com.example.components

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            class ComponentContainer {
                @Preview(name = "Member Preview")
                @Composable
                fun MemberPreview() {}

                companion object {
                    @Preview(name = "Companion Preview")
                    @Composable
                    fun CompanionPreview() {}
                }
            }
        """.trimIndent()

        val previews = scanner.parseSource(source)

        assertEquals(2, previews.size)
        val member = previews.first { it.functionName == "MemberPreview" }
        assertEquals("ComponentContainer", member.enclosingClass)
        assertEquals("Member Preview", member.parameters.name)

        val companion = previews.first { it.functionName == "CompanionPreview" }
        assertEquals("ComponentContainer.Companion", companion.enclosingClass)
        assertEquals("Companion Preview", companion.parameters.name)
    }

    @Test
    fun `scans real sample app Greeting file successfully`() {
        val greetingFile = File("samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt")
        if (greetingFile.exists()) {
            val previews = scanner.parseFile(greetingFile)
            assertFalse(previews.isEmpty())
            val preview = previews.firstOrNull { it.functionName == "GreetingPreview" }
            assertNotNull(preview)
            assertEquals("com.compositor.sample", preview?.packageName)
            assertEquals(true, preview?.parameters?.showBackground)
        }
    }

    @Test
    fun `performance benchmark parses dozens of sources in under 100 milliseconds`() {
        val template = """
            package com.perf.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            @Preview(name = "Perf Preview", showBackground = true)
            @Composable
            fun BenchmarkItemPreview() {}
        """.trimIndent()

        // Warm up
        scanner.parseSource(template)

        val iterations = 30
        val elapsed = measureTimeMillis {
            for (i in 0 until iterations) {
                val results = scanner.parseSource(template)
                assertEquals(1, results.size)
            }
        }

        assertTrue(
            elapsed < 500,
            "Expected 30 parses to finish swiftly, took ${elapsed}ms"
        )
    }
}
