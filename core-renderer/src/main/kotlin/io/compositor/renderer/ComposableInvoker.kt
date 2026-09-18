package io.compositor.renderer

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Utility to discover and dynamically invoke Jetpack Compose @Composable functions via reflection.
 */
object ComposableInvoker {

    private const val COMPOSER_CLASS_NAME = "androidx.compose.runtime.Composer"

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
        val targetClass = Class.forName(className, true, classLoader)
        val method = findComposableMethod(targetClass, methodName)

        method.isAccessible = true
        val isStatic = Modifier.isStatic(method.modifiers)
        val instance = if (isStatic) null else resolveInstance(targetClass)

        val args = prepareArguments(method, composer)
        method.invoke(instance, *args)
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

        // Fallback to no-arg constructor
        val constructor = clazz.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance()
    }

    /**
     * Finds the Composable method, matching Compose-transformed signatures.
     */
    fun findComposableMethod(targetClass: Class<*>, methodName: String): Method {
        val allMethods = targetClass.methods + targetClass.declaredMethods

        // Prefer method taking Composer parameter
        val composableMethod = allMethods.firstOrNull { m ->
            m.name == methodName && m.parameterTypes.any { it.name == COMPOSER_CLASS_NAME }
        }
        if (composableMethod != null) {
            return composableMethod
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
     * Builds parameter array passing the composer and default bitmasks ($changed, $default).
     */
    private fun prepareArguments(method: Method, composer: Any?): Array<Any?> {
        val paramTypes = method.parameterTypes
        val args = arrayOfNulls<Any?>(paramTypes.size)

        for (i in paramTypes.indices) {
            val paramType = paramTypes[i]
            when {
                paramType.name == COMPOSER_CLASS_NAME -> {
                    args[i] = composer
                }
                paramType == java.lang.Integer.TYPE || paramType == java.lang.Integer::class.java -> {
                    args[i] = 0
                }
                else -> {
                    args[i] = null
                }
            }
        }
        return args
    }
}
