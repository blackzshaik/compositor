package io.compositor.compiler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinSourceCompilerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `test find compose compiler plugin jar`() {
        val jar = KotlinSourceCompiler.findComposeCompilerPluginJar()
        assertNotNull(jar, "Compose compiler plugin JAR should be resolvable from classpath")
        assertTrue(jar!!.exists(), "Compose compiler plugin JAR file must exist")
    }

    @Test
    fun `test compile simple valid kotlin source`() {
        val srcFile = File(tempDir, "Simple.kt").apply {
            writeText(
                """
                package io.test

                fun hello(): String = "Hello from test"
                """.trimIndent()
            )
        }
        val outDir = File(tempDir, "classes")

        val result = KotlinSourceCompiler.compile(
            sourceFile = srcFile,
            outputDir = outDir,
            classpath = emptyList()
        )

        assertTrue(result.isSuccess, "Compilation should succeed for valid Kotlin source: ${result.errorText}")
        val classFile = File(outDir, "io/test/SimpleKt.class")
        assertTrue(classFile.exists(), "Compiled class file must exist")
    }

    @Test
    fun `test compile invalid kotlin source captures diagnostic errors`() {
        val srcFile = File(tempDir, "Invalid.kt").apply {
            writeText(
                """
                package io.test

                fun broken() {
                    val x: Int = "not an int"
                }
                """.trimIndent()
            )
        }
        val outDir = File(tempDir, "classes")

        val result = KotlinSourceCompiler.compile(
            sourceFile = srcFile,
            outputDir = outDir,
            classpath = emptyList()
        )

        assertFalse(result.isSuccess, "Compilation should fail for invalid source")
        assertTrue(result.errorMessages.isNotEmpty(), "Compiler errors should be captured")
        assertTrue(
            result.errorText.contains("Type mismatch") || result.errorText.contains("type mismatch") ||
                result.errorText.contains("Initializer type mismatch"),
            "Error message should mention type mismatch: ${result.errorText}"
        )
    }

    @Test
    fun `test compile sample greeting with compose plugin`() {
        val sampleSrc = File("../samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt")
            .takeIf { it.exists() }
            ?: File("samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt")

        if (!sampleSrc.exists()) return

        val classpath = resolveTestClasspath()
        if (classpath.isEmpty()) return

        val outDir = File(tempDir, "compiled-classes")
        val result = KotlinSourceCompiler.compile(
            sourceFile = sampleSrc,
            outputDir = outDir,
            classpath = classpath
        )

        assertTrue(result.isSuccess, "Greeting.kt should compile cleanly: ${result.errorText}")
        val classFile = File(outDir, "com/compositor/sample/GreetingKt.class")
        assertTrue(classFile.exists(), "GreetingKt.class must exist")
    }

    private fun resolveTestClasspath(): List<File> {
        val classpath = mutableListOf<File>()

        val compileDirProp = System.getProperty("compositor.sample.compile.dir")
        val compileDir = compileDirProp?.let { File(it) }?.takeIf { it.exists() }
            ?: File("../samples/sample-app/build/compositor/compile-jars").takeIf { it.exists() }
            ?: File("samples/sample-app/build/compositor/compile-jars").takeIf { it.exists() }

        compileDir?.listFiles()?.filter { it.extension == "jar" }?.let { classpath.addAll(it) }

        val extractedDir = File("../samples/sample-app/build/compositor/extracted-jars").takeIf { it.exists() }
            ?: File("samples/sample-app/build/compositor/extracted-jars").takeIf { it.exists() }

        extractedDir?.listFiles()?.filter { it.extension == "jar" }?.let { classpath.addAll(it) }

        val androidJarProp = System.getProperty("compositor.sample.android.jar")
        if (androidJarProp != null && File(androidJarProp).exists()) {
            classpath.add(File(androidJarProp))
        }

        return classpath
    }
}
