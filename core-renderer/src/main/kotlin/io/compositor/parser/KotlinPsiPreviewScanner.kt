package io.compositor.parser

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import java.io.File

/**
 * High-performance preview discovery scanner utilizing Kotlin Compiler Embedded PSI.
 * Parses `.kt` source files directly into AST to extract `@Preview` definitions.
 */
class KotlinPsiPreviewScanner {

    private val disposable = Disposer.newDisposable()
    private val environment: KotlinCoreEnvironment = KotlinCoreEnvironment.createForProduction(
        disposable,
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
    private val psiFactory: KtPsiFactory = KtPsiFactory(environment.project, markGenerated = false)

    /**
     * Parses the Kotlin source code string and extracts all `@Preview` definitions.
     */
    fun parseSource(source: String, filePath: String = ""): List<PreviewDefinition> {
        val ktFile = psiFactory.createFile("preview_scan.kt", source)
        return extractPreviewsFromFile(ktFile, source, filePath)
    }

    /**
     * Parses a single `.kt` file on disk and extracts its `@Preview` definitions.
     */
    fun parseFile(file: File): List<PreviewDefinition> {
        if (!file.exists() || !file.isFile || file.extension != "kt") {
            return emptyList()
        }
        val source = file.readText()
        val ktFile = psiFactory.createFile(file.name, source)
        return extractPreviewsFromFile(ktFile, source, file.invariantSeparatorsPath)
    }

    /**
     * Recursively scans a directory for Kotlin source files and extracts all preview definitions.
     */
    fun scanDirectory(directory: File): List<PreviewDefinition> {
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        val excludedDirs = setOf("build", ".git", ".gradle", "node_modules", ".idea")
        val results = mutableListOf<PreviewDefinition>()

        directory.walkTopDown()
            .onEnter { dir -> dir.name !in excludedDirs }
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                results.addAll(parseFile(file))
            }

        return results
    }

    /**
     * Scans typical Android source roots under the given project directory.
     */
    fun scanProjectSources(projectRoot: File): List<PreviewDefinition> {
        val commonSourceRoots = listOf(
            File(projectRoot, "src/main/java"),
            File(projectRoot, "src/main/kotlin"),
            File(projectRoot, "src")
        )
        val candidateRoots = commonSourceRoots.filter { it.exists() && it.isDirectory }
        val targetRoots = if (candidateRoots.isNotEmpty()) candidateRoots else listOf(projectRoot)

        val results = mutableListOf<PreviewDefinition>()
        for (root in targetRoots) {
            results.addAll(scanDirectory(root))
        }
        return results.distinctBy { def ->
            "${def.filePath}:${def.packageName}.${def.enclosingClass ?: ""}.${def.functionName}#${def.parameters.name}"
        }
    }

    private fun extractPreviewsFromFile(
        ktFile: KtFile,
        source: String,
        filePath: String
    ): List<PreviewDefinition> {
        val packageName = ktFile.packageFqName.asString()
        val customMultiPreviews = discoverCustomMultiPreviews(ktFile)
        val functions = ktFile.collectDescendantsOfType<KtNamedFunction>()
        val definitions = mutableListOf<PreviewDefinition>()

        for (function in functions) {
            val functionName = function.name ?: continue
            val enclosingClass = resolveEnclosingClass(function)
            val line = calculateLineNumber(source, function.textOffset)

            val annotations = function.annotationEntries
            for (annotation in annotations) {
                val shortName = annotation.shortName?.asString() ?: continue

                if (isPreviewAnnotation(shortName)) {
                    val params = parseAnnotationParameters(annotation)
                    definitions.add(
                        PreviewDefinition(
                            functionName = functionName,
                            enclosingClass = enclosingClass,
                            packageName = packageName,
                            parameters = params,
                            line = calculateLineNumber(source, annotation.textOffset),
                            filePath = filePath
                        )
                    )
                } else if (shortName in customMultiPreviews) {
                    val expandedParamsList = customMultiPreviews[shortName] ?: emptyList()
                    for (params in expandedParamsList) {
                        definitions.add(
                            PreviewDefinition(
                                functionName = functionName,
                                enclosingClass = enclosingClass,
                                packageName = packageName,
                                parameters = params,
                                line = calculateLineNumber(source, annotation.textOffset),
                                filePath = filePath
                            )
                        )
                    }
                } else if (shortName in KNOWN_MULTIPREVIEWS) {
                    val expandedParamsList = KNOWN_MULTIPREVIEWS[shortName] ?: emptyList()
                    for (params in expandedParamsList) {
                        definitions.add(
                            PreviewDefinition(
                                functionName = functionName,
                                enclosingClass = enclosingClass,
                                packageName = packageName,
                                parameters = params,
                                line = calculateLineNumber(source, annotation.textOffset),
                                filePath = filePath
                            )
                        )
                    }
                }
            }
        }

        return definitions
    }

    private fun discoverCustomMultiPreviews(ktFile: KtFile): Map<String, List<PreviewParameters>> {
        val map = mutableMapOf<String, List<PreviewParameters>>()
        val classes = ktFile.collectDescendantsOfType<KtClass>()

        for (klass in classes) {
            if (!klass.isAnnotation()) continue
            val className = klass.name ?: continue
            val previewAnnotations = klass.annotationEntries.filter { entry ->
                val name = entry.shortName?.asString()
                name != null && isPreviewAnnotation(name)
            }
            if (previewAnnotations.isNotEmpty()) {
                val paramsList = previewAnnotations.map { parseAnnotationParameters(it) }
                map[className] = paramsList
            }
        }

        return map
    }

    private fun isPreviewAnnotation(shortName: String): Boolean =
        shortName == "Preview" || shortName.endsWith(".Preview")

    private fun parseAnnotationParameters(annotation: KtAnnotationEntry): PreviewParameters {
        val builder = ParametersBuilder()
        val valueArguments = annotation.valueArguments
        for (index in valueArguments.indices) {
            val argument = valueArguments[index]
            val argName = argument.getArgumentName()?.asName?.asString()
            val expr = argument.getArgumentExpression() ?: continue
            val resolvedName = argName ?: if (index == 0) "name" else null
            if (resolvedName != null) {
                applyArgument(builder, resolvedName, expr)
            }
        }
        return builder.build()
    }

    private fun applyArgument(
        builder: ParametersBuilder,
        name: String,
        expr: org.jetbrains.kotlin.psi.KtExpression
    ) {
        when (name) {
            "name" -> builder.name = extractStringOrExpression(expr)
            "group" -> builder.group = extractStringOrExpression(expr)
            "widthDp" -> builder.widthDp = extractIntLiteral(expr)
            "heightDp" -> builder.heightDp = extractIntLiteral(expr)
            "uiMode" -> builder.uiMode = extractStringOrExpression(expr)
            "fontScale" -> builder.fontScale = extractFloatLiteral(expr)
            "showBackground" -> builder.showBackground = expr.text.trim() == "true"
            "backgroundColor" -> builder.backgroundColor = extractStringOrExpression(expr)
            "apiLevel" -> builder.apiLevel = extractIntLiteral(expr)
            "locale" -> builder.locale = extractStringOrExpression(expr)
        }
    }

    private class ParametersBuilder {
        var name: String? = null
        var group: String? = null
        var widthDp: Int? = null
        var heightDp: Int? = null
        var uiMode: String? = null
        var fontScale: Float? = null
        var showBackground: Boolean? = null
        var backgroundColor: String? = null
        var apiLevel: Int? = null
        var locale: String? = null

        fun build(): PreviewParameters = PreviewParameters(
            name = name,
            group = group,
            widthDp = widthDp,
            heightDp = heightDp,
            uiMode = uiMode,
            fontScale = fontScale,
            showBackground = showBackground,
            backgroundColor = backgroundColor,
            apiLevel = apiLevel,
            locale = locale
        )
    }

    private fun extractStringOrExpression(expr: org.jetbrains.kotlin.psi.KtExpression): String =
        when (expr) {
            is KtStringTemplateExpression -> expr.entries.joinToString("") { it.text }
            else -> expr.text.trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
        }

    private fun extractIntLiteral(expr: org.jetbrains.kotlin.psi.KtExpression): Int? {
        val text = expr.text.trim()
        return if (text.startsWith("0x") || text.startsWith("0X")) {
            text.substring(2).toLongOrNull(HEX_RADIX)?.toInt()
        } else {
            text.toIntOrNull()
        }
    }

    private fun extractFloatLiteral(expr: org.jetbrains.kotlin.psi.KtExpression): Float? {
        val cleaned = expr.text.trim().removeSuffix("f").removeSuffix("F")
        return cleaned.toFloatOrNull()
    }

    private fun resolveEnclosingClass(function: KtNamedFunction): String? {
        val parentClass = function.getParentOfType<KtClassOrObject>(strict = true) ?: return null
        return if (parentClass is KtObjectDeclaration && parentClass.isCompanion()) {
            val outerClass = parentClass.getParentOfType<KtClassOrObject>(strict = true)
            if (outerClass != null) "${outerClass.name}.Companion" else parentClass.name
        } else {
            parentClass.name
        }
    }

    private fun calculateLineNumber(source: String, textOffset: Int): Int {
        if (textOffset <= 0) return 1
        val safeOffset = textOffset.coerceAtMost(source.length)
        var lineCount = 1
        for (i in 0 until safeOffset) {
            if (source[i] == '\n') {
                lineCount++
            }
        }
        return lineCount
    }

    companion object {
        private const val HEX_RADIX = 16

        val KNOWN_MULTIPREVIEWS: Map<String, List<PreviewParameters>> = mapOf(
            "PreviewLightDark" to listOf(
                PreviewParameters(
                    name = "Light",
                    uiMode = "Configuration.UI_MODE_NIGHT_NO",
                    showBackground = true
                ),
                PreviewParameters(
                    name = "Dark",
                    uiMode = "Configuration.UI_MODE_NIGHT_YES",
                    showBackground = true
                )
            ),
            "PreviewScreenSizes" to listOf(
                PreviewParameters(name = "Phone", widthDp = 360, heightDp = 640),
                PreviewParameters(name = "Foldable", widthDp = 673, heightDp = 841),
                PreviewParameters(name = "Tablet", widthDp = 1280, heightDp = 800),
                PreviewParameters(name = "Desktop", widthDp = 1920, heightDp = 1080)
            ),
            "PreviewFontScale" to listOf(
                PreviewParameters(name = "Normal Font", fontScale = 1.0f),
                PreviewParameters(name = "Large Font", fontScale = 1.5f),
                PreviewParameters(name = "Largest Font", fontScale = 2.0f)
            )
        )
    }
}
