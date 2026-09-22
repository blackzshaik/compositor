package io.compositor.compiler

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Result of on-demand source compilation.
 */
data class CompilationResult(
    val isSuccess: Boolean,
    val errorMessages: List<String> = emptyList()
) {
    val errorText: String
        get() = errorMessages.joinToString("\n")
}

/**
 * On-demand fast in-process Kotlin source compiler using embedded K2JVMCompiler.
 * Dynamically compiles modified .kt Composable files into target JVM bytecode
 * without needing external Gradle daemon tasks.
 */
object KotlinSourceCompiler {

    private const val COMPOSE_REGISTRAR_CLASS =
        "androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar"

    /**
     * Resolves the Compose compiler plugin embeddable JAR.
     */
    fun findComposeCompilerPluginJar(): File? {
        val sysProp = System.getProperty("compositor.compose.compiler.plugin.jar")
        if (!sysProp.isNullOrBlank()) {
            val file = File(sysProp)
            if (file.exists()) return file
        }

        val envProp = System.getenv("COMPOSITOR_COMPOSE_PLUGIN_JAR")
        if (!envProp.isNullOrBlank()) {
            val file = File(envProp)
            if (file.exists()) return file
        }

        return try {
            val clazz = Class.forName(COMPOSE_REGISTRAR_CLASS)
            val location = clazz.protectionDomain?.codeSource?.location
            location?.let { File(it.toURI()) }?.takeIf { it.exists() }
        } catch (_: ClassNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Compiles a single Kotlin source file into the specified output directory
     * using the provided project classpath and Compose compiler plugin.
     */
    @Suppress("TooGenericExceptionCaught")
    fun compile(
        sourceFile: File,
        outputDir: File,
        classpath: List<File>,
        composePluginJar: File? = findComposeCompilerPluginJar()
    ): CompilationResult {
        if (!sourceFile.exists() || !sourceFile.isFile) {
            return CompilationResult(
                isSuccess = false,
                errorMessages = listOf("Source file does not exist: ${sourceFile.absolutePath}")
            )
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val validClasspath = classpath.filter { it.exists() }
        val cpString = validClasspath.joinToString(File.pathSeparator) { it.absolutePath }
        val errors = mutableListOf<String>()
        val collector = createMessageCollector(sourceFile.name, errors)

        val hasCompose = detectComposeRuntime(validClasspath)
        val pluginJar = if (hasCompose) composePluginJar else null
        val args = createCompilerArguments(sourceFile, outputDir, cpString, pluginJar)

        val compiler = K2JVMCompiler()
        return try {
            val exitCode = compiler.exec(collector, Services.EMPTY, args)
            val success = exitCode.code == 0
            CompilationResult(
                isSuccess = success,
                errorMessages = if (!success && errors.isEmpty()) {
                    listOf("Compilation failed with exit code ${exitCode.code}")
                } else {
                    errors
                }
            )
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            CompilationResult(
                isSuccess = false,
                errorMessages = listOf("Compiler exception: ${e.message}\n$sw")
            )
        }
    }

    private fun detectComposeRuntime(validClasspath: List<File>): Boolean =
        validClasspath.any { file ->
            val n = file.name.lowercase()
            n.contains("compose.runtime") || n.contains("compose-runtime") ||
                n.contains("runtime-release") || n.contains("runtime-debug") ||
                n.contains("runtime-android") || n.contains("runtime-jvm") ||
                n.contains("runtime-desktop")
        }

    private fun createMessageCollector(
        sourceFileName: String,
        errors: MutableList<String>
    ): MessageCollector = object : MessageCollector {
        override fun clear() {
            errors.clear()
        }
        override fun hasErrors(): Boolean = errors.isNotEmpty()
        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            if (severity.isError) {
                val locStr = if (location != null) {
                    "${location.path}:${location.line}:${location.column}"
                } else {
                    sourceFileName
                }
                errors.add("[$severity] $locStr: $message")
            }
        }
    }

    private fun createCompilerArguments(
        sourceFile: File,
        outputDir: File,
        classpathString: String,
        composePluginJar: File?
    ): K2JVMCompilerArguments = K2JVMCompilerArguments().apply {
        freeArgs = listOf(sourceFile.absolutePath)
        destination = outputDir.absolutePath
        classpath = classpathString
        noStdlib = true
        jvmTarget = "21"
        if (composePluginJar != null && composePluginJar.exists()) {
            pluginClasspaths = arrayOf(composePluginJar.absolutePath)
        }
    }

    /**
     * Backward-compatible convenience wrapper returning boolean.
     */
    fun compileSource(
        sourceFile: File,
        outputDir: File,
        classpath: List<File>
    ): Boolean = compile(sourceFile, outputDir, classpath).isSuccess
}
