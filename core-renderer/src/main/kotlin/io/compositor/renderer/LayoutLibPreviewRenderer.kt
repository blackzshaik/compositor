package io.compositor.renderer

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Environment
import app.cash.paparazzi.PaparazziSdk
import com.android.ide.common.rendering.api.RenderSession
import com.android.ide.common.rendering.api.ViewInfo
import java.awt.image.BufferedImage
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.imageio.ImageIO
import kotlin.jvm.functions.Function2

/**
 * Headless in-memory Compose preview renderer powered by Android LayoutLib.
 *
 * Replaces testing harnesses like Paparazzi JUnit rules by directly instantiating
 * and rendering Composables within an isolated JVM render session.
 */
class LayoutLibPreviewRenderer(
    private val customRuntimeRoot: File? = null
) {

    /**
     * Executes headless rendering for the requested Composable preview.
     */
    @Suppress("TooGenericExceptionCaught")
    fun render(request: RenderRequest): RenderResult = synchronized(RENDER_LOCK) {
        val startTime = System.currentTimeMillis()
        println("[Compositor Renderer] Starting render for ${request.composableId} (${request.className}#${request.methodName})...")

        try {
            resetSdkState()
            LayoutLibBootstrap.ensureInitialized(customRuntimeRoot)

            val classLoader = if (request.classpath.isNotEmpty()) {
                CompositorClassLoader.create(request.classpath)
            } else {
                Thread.currentThread().contextClassLoader ?: javaClass.classLoader
            }

            AndroidBuildBootstrap.initializeBuildFields(classLoader)
            val originalClassLoader = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = classLoader

            try {
                val result = executeRender(request, classLoader, startTime)
                val durationMs = System.currentTimeMillis() - startTime
                println("[Compositor Renderer] Render SUCCEEDED for ${request.composableId} in ${durationMs}ms")
                result
            } finally {
                Thread.currentThread().contextClassLoader = originalClassLoader
            }
        } catch (t: Throwable) {
            resetSdkState()
            val durationMs = System.currentTimeMillis() - startTime
            System.err.println("[Compositor Renderer] Render FAILED for ${request.composableId} in ${durationMs}ms: ${t.message}")
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            RenderResult.Failure(
                errorMessage = t.message ?: "Unknown rendering failure",
                cause = t,
                stackTrace = sw.toString()
            )
        }
    }

    private fun resetSdkState() {
        try {
            val rendererField = PaparazziSdk::class.java.getDeclaredField("renderer")
            rendererField.isAccessible = true
            rendererField.set(null, null)

            val sessionParamsField = PaparazziSdk::class.java.getDeclaredField("sessionParamsBuilder")
            sessionParamsField.isAccessible = true
            sessionParamsField.set(null, null)
        } catch (_: ReflectiveOperationException) {
            // Field access ignored if internal signature changes
        }
    }

    private fun executeRender(
        request: RenderRequest,
        classLoader: ClassLoader,
        startTime: Long
    ): RenderResult {
        val environment = createEnvironment(request)
        val deviceConfig = resolveDeviceConfig(request.deviceConfig)

        var capturedImage: BufferedImage? = null

        val sdk = PaparazziSdk(
            environment = environment,
            deviceConfig = deviceConfig,
            theme = request.deviceConfig.theme,
            onNewFrame = { frame ->
                capturedImage = frame
            }
        )

        sdk.setup()
        sdk.prepare()

        val rootBounds: ElementBounds?
        try {
            val contentLambda: (Any?, Any?) -> Unit = { composer, _ ->
                ComposableInvoker.invokeComposable(
                    className = request.className,
                    methodName = request.methodName,
                    composer = composer,
                    classLoader = classLoader
                )
            }

            val snapshotMethod = PaparazziSdk::class.java.methods.firstOrNull { method ->
                method.name == "snapshot" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == Function2::class.java
            } ?: error("PaparazziSdk.snapshot(Function2) method not found")

            snapshotMethod.invoke(sdk, contentLambda)
            rootBounds = extractRootBounds(sdk)
        } finally {
            sdk.teardown()
            resetSdkState()
        }

        val image = capturedImage ?: error("LayoutLib produced no frame for ${request.composableId}")
        val outputFile = request.outputFile
        outputFile.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
        ImageIO.write(image, "PNG", outputFile)

        val durationMs = System.currentTimeMillis() - startTime

        return RenderResult.Success(
            imageFile = outputFile,
            width = image.width,
            height = image.height,
            durationMs = durationMs,
            rootBounds = rootBounds
        )
    }

    private fun createEnvironment(request: RenderRequest): Environment {
        val pkg = request.className.substringBeforeLast(".", "")
        val localResDirs = request.resourceDirs.map { it.absolutePath }

        val rClassAvailable = if (pkg.isNotEmpty()) {
            try {
                Class.forName("$pkg.R") != null
            } catch (_: ClassNotFoundException) {
                false
            } catch (_: LinkageError) {
                false
            }
        } else {
            false
        }

        return Environment(
            appTestDir = "",
            packageName = pkg,
            compileSdkVersion = 35,
            resourcePackageNames = if (rClassAvailable) listOf(pkg) else emptyList(),
            localResourceDirs = localResDirs,
            moduleResourceDirs = emptyList(),
            libraryResourceDirs = emptyList(),
            allModuleAssetDirs = emptyList(),
            libraryAssetDirs = emptyList()
        )
    }

    private fun resolveDeviceConfig(config: CompositorDeviceConfig): DeviceConfig {
        return when (config.name) {
            "Pixel 5" -> DeviceConfig.PIXEL_5
            "Pixel 7" -> DeviceConfig.PIXEL_6
            "Nexus 10 Tablet" -> DeviceConfig.NEXUS_10
            else -> DeviceConfig.PIXEL_5
        }
    }

    private fun extractRootBounds(sdk: PaparazziSdk): ElementBounds? {
        val sessionField = PaparazziSdk::class.java.declaredFields.firstOrNull { field ->
            RenderSession::class.java.isAssignableFrom(field.type)
        }
        sessionField?.isAccessible = true

        val renderSession = sessionField?.get(sdk) as? RenderSession
        val primaryRoot = renderSession?.rootViews?.firstOrNull()

        return primaryRoot?.let { toElementBounds(it) }
    }

    private fun toElementBounds(viewInfo: ViewInfo): ElementBounds {
        val children = viewInfo.children?.map { toElementBounds(it) } ?: emptyList()
        val className = viewInfo.className ?: "android.view.View"
        val width = (viewInfo.right - viewInfo.left).coerceAtLeast(0)
        val height = (viewInfo.bottom - viewInfo.top).coerceAtLeast(0)

        return ElementBounds(
            className = className,
            left = viewInfo.left,
            top = viewInfo.top,
            width = width,
            height = height,
            children = children
        )
    }

    companion object {
        private val RENDER_LOCK = Any()
    }
}
