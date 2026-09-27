package io.compositor.renderer

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Utility to discover and dynamically invoke Jetpack Compose @Composable functions via reflection.
 */
object ComposableInvoker {

    private const val COMPOSER_CLASS_NAME = "androidx.compose.runtime.Composer"
    private const val PREVIEW_PARAMETER_ANNOTATION = "androidx.compose.ui.tooling.preview.PreviewParameter"
    private const val TOOLING_INVOKER_CLASS = "androidx.compose.ui.tooling.ComposableInvoker"
    private const val INSPECTION_MODE_CLASS = "androidx.compose.ui.platform.InspectionModeKt"
    private const val COMPOSITION_LOCAL_KT_CLASS = "androidx.compose.runtime.CompositionLocalKt"

    /**
     * Locates and invokes a Composable function wrapped with LocalInspectionMode = true.
     */
    fun invokeWithInspectionMode(
        className: String,
        methodName: String,
        composer: Any?,
        classLoader: ClassLoader
    ) {
        val wrapped = tryWrapWithInspectionMode(className, methodName, composer, classLoader)
        if (!wrapped) {
            invokeComposable(className, methodName, composer, classLoader)
        }
    }

    /**
     * Locates and invokes a Composable function by class and method name within the given classloader.
     *
     * @param className Fully qualified name of the containing class (e.g. "com.compositor.sample.GreetingKt")
     * @param methodName Name of the Composable function (e.g. "GreetingPreview")
     * @param composer Current Compose runtime Composer instance
     * @param classLoader ClassLoader containing the compiled Composable class
     */
    fun invokeComposable(
        className: String,
        methodName: String,
        composer: Any?,
        classLoader: ClassLoader
    ) {
        // 1. First attempt to invoke via Android Studio's ComposableInvoker from androidx.compose.ui:ui-tooling
        if (tryInvokeWithToolingInvoker(className, methodName, composer, classLoader)) {
            return
        }

        // 2. Fallback reflection invocation
        val targetClass = Class.forName(className, true, classLoader)
        val method = findComposableMethod(targetClass, methodName)

        method.isAccessible = true
        val isStatic = Modifier.isStatic(method.modifiers)
        val instance = if (isStatic) null else resolveInstance(targetClass)

        val args = prepareArguments(method, composer)
        try {
            method.invoke(instance, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e.cause ?: e
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException", "CyclomaticComplexMethod")
    private fun tryWrapWithInspectionMode(
        className: String,
        methodName: String,
        composer: Any?,
        classLoader: ClassLoader
    ): Boolean {
        return try {
            val inspectionClass = Class.forName(INSPECTION_MODE_CLASS, true, classLoader)
            val getLocalInspectionModeMethod = inspectionClass.getMethod("getLocalInspectionMode")
            val localInspectionMode = getLocalInspectionModeMethod.invoke(null) ?: return false
            val providesMethod = localInspectionMode.javaClass.getMethod("provides", Any::class.java)
            val providedValue = providesMethod.invoke(localInspectionMode, true) ?: return false

            val clKtClass = Class.forName(COMPOSITION_LOCAL_KT_CLASS, true, classLoader)
            val clpArrayMethod = clKtClass.methods.firstOrNull {
                it.name == "CompositionLocalProvider" && it.parameterTypes.firstOrNull()?.isArray == true
            }
            val clpSingleMethod = clKtClass.methods.firstOrNull {
                it.name == "CompositionLocalProvider" &&
                    it.parameterTypes.size >= 3 &&
                    it.parameterTypes[0].name.contains("ProvidedValue") &&
                    !it.parameterTypes[0].isArray
            }

            val contentLambda: (Any?, Any?) -> Unit = { childComposer, _ ->
                invokeComposable(className, methodName, childComposer, classLoader)
            }

            when {
                clpArrayMethod != null -> {
                    val array = java.lang.reflect.Array.newInstance(providedValue.javaClass, 1)
                    java.lang.reflect.Array.set(array, 0, providedValue)
                    if (clpArrayMethod.parameterTypes.size == 5) {
                        clpArrayMethod.invoke(null, array, contentLambda, composer, 56, 0)
                    } else {
                        clpArrayMethod.invoke(null, array, contentLambda, composer, 56)
                    }
                    true
                }
                clpSingleMethod != null -> {
                    if (clpSingleMethod.parameterTypes.size == 4) {
                        clpSingleMethod.invoke(null, providedValue, contentLambda, composer, 56)
                    } else {
                        clpSingleMethod.invoke(null, providedValue, contentLambda, composer, 56, 0)
                    }
                    true
                }
                else -> false
            }
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e.cause ?: e
        } catch (_: Throwable) {
            false
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun tryInvokeWithToolingInvoker(
        className: String,
        methodName: String,
        composer: Any?,
        classLoader: ClassLoader
    ): Boolean {
        return try {
            val toolingClass = Class.forName(TOOLING_INVOKER_CLASS, true, classLoader)
            val method = toolingClass.methods.firstOrNull {
                it.name == "invokeComposable" && it.parameterTypes.size == 4
            } ?: return false

            method.isAccessible = true
            val isStatic = Modifier.isStatic(method.modifiers)
            val instance = if (isStatic) null else {
                toolingClass.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
            }
            method.invoke(instance, className, methodName, composer, emptyArray<Any?>())
            true
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e.cause ?: e
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Resolves an instance of the class if the Composable is an instance method.
     */
    private fun resolveInstance(clazz: Class<*>): Any {
        // Check for Kotlin singleton object instance field
        val instanceField = clazz.declaredFields.firstOrNull { it.name == "INSTANCE" }
        if (instanceField != null) {
            instanceField.isAccessible = true
            val instance = instanceField.get(null)
            if (instance != null) return instance
        }

        // Fallback to constructor
        val constructor = clazz.declaredConstructors.firstOrNull { it.parameterTypes.isEmpty() }
            ?: clazz.declaredConstructors.firstOrNull()
            ?: error("No constructor found for ${clazz.name}")
        constructor.isAccessible = true

        return try {
            if (constructor.parameterTypes.isEmpty()) {
                constructor.newInstance()
            } else {
                val cArgs = Array(constructor.parameterTypes.size) { idx ->
                    defaultForPrimitive(constructor.parameterTypes[idx])
                }
                constructor.newInstance(*cArgs)
            }
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e.cause ?: e
        }
    }

    /**
     * Finds the Composable method, matching Compose-transformed signatures.
     */
    fun findComposableMethod(targetClass: Class<*>, methodName: String): Method {
        val allMethods = targetClass.methods + targetClass.declaredMethods

        // Prefer method taking Composer parameter
        val composableMethods = allMethods.filter { m ->
            m.name == methodName && m.parameterTypes.any { it.name == COMPOSER_CLASS_NAME }
        }
        if (composableMethods.isNotEmpty()) {
            // Pick method with shortest signature (or matching composer count)
            return composableMethods.minByOrNull { it.parameterTypes.size } ?: composableMethods.first()
        }

        // Fallback to parameterless method
        val parameterlessMethod = allMethods.firstOrNull { m ->
            m.name == methodName && m.parameterTypes.isEmpty()
        }
        if (parameterlessMethod != null) {
            return parameterlessMethod
        }

        val available = allMethods.map { it.name }.distinct()
        throw NoSuchMethodException(
            "Composable method '$methodName' not found in class '${targetClass.name}'. " +
                "Available methods: $available"
        )
    }

    /**
     * Builds parameter array passing the composer, resolved preview params, and default bitmasks.
     */
    private fun prepareArguments(method: Method, composer: Any?): Array<Any?> {
        val paramTypes = method.parameterTypes
        val args = arrayOfNulls<Any?>(paramTypes.size)
        val composerIndex = paramTypes.indexOfFirst { it.name == COMPOSER_CLASS_NAME }

        if (composerIndex == -1) {
            for (i in paramTypes.indices) {
                args[i] = defaultForPrimitive(paramTypes[i])
            }
            return args
        }

        // 1. Real user parameters before Composer
        for (i in 0 until composerIndex) {
            args[i] = resolvePreviewParameter(method, i) ?: defaultForPrimitive(paramTypes[i])
        }

        // 2. Composer parameter
        args[composerIndex] = composer

        // 3. Changed and Default bitmasks after Composer
        val totalIntsAfterComposer = paramTypes.size - 1 - composerIndex
        val userParamCount = composerIndex
        val changedParamCount = if (userParamCount == 0) 1 else ((userParamCount * 3 + 30) / 31)

        for (i in (composerIndex + 1) until paramTypes.size) {
            val offsetAfterComposer = i - composerIndex
            if (offsetAfterComposer <= changedParamCount) {
                // $changed parameter: 0 informs Compose runtime to compute/force
                args[i] = 0
            } else {
                // $default parameter: -1 (all bits 1) instructs Compose compiler bytecode
                // to evaluate default expressions for all arguments with defaults!
                args[i] = -1
            }
        }

        return args
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    private fun resolvePreviewParameter(method: Method, paramIndex: Int): Any? {
        val annotations = method.parameterAnnotations.getOrNull(paramIndex) ?: return null
        val previewAnnotation = annotations.firstOrNull {
            it.annotationClass.java.name == PREVIEW_PARAMETER_ANNOTATION
        } ?: return null

        return try {
            val providerMethod = previewAnnotation.javaClass.getMethod("provider")
            val providerClass = providerMethod.invoke(previewAnnotation) as? Class<*> ?: return null
            val constructor = providerClass.getDeclaredConstructor().apply { isAccessible = true }
            val provider = constructor.newInstance()
            val values = providerClass.getMethod("getValues").invoke(provider)
            when (values) {
                is Sequence<*> -> values.firstOrNull()
                is Iterable<*> -> values.firstOrNull()
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun defaultForPrimitive(type: Class<*>): Any? {
        return when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0.0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Character.TYPE -> '\u0000'
            else -> null
        }
    }
}
