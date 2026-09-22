package io.compositor.renderer

import sun.misc.Unsafe
import java.lang.reflect.Field

/**
 * Initializes Android platform framework environment variables, such as android.os.Build,
 * required by modern Jetpack Compose runtime components (e.g. LayerManager in Compose 1.7+).
 */
object AndroidBuildBootstrap {

    private val stringBuildFields = mapOf(
        "FINGERPRINT" to "google/redfin/redfin:14/UP1A.231005.007/10754064:user/release-keys",
        "MANUFACTURER" to "Google",
        "MODEL" to "Pixel 5",
        "BRAND" to "google",
        "DEVICE" to "redfin",
        "PRODUCT" to "redfin",
        "HARDWARE" to "redfin",
        "BOARD" to "redfin",
        "ID" to "UP1A.231005.007",
        "DISPLAY" to "UP1A.231005.007",
        "TAGS" to "release-keys",
        "TYPE" to "user",
        "USER" to "android-build",
        "HOST" to "android-build",
        "BOOTLOADER" to "unknown",
        "RADIO" to "unknown",
        "SERIAL" to "unknown"
    )

    private val versionStringFields = mapOf(
        "CODENAME" to "REL",
        "RELEASE" to "14",
        "INCREMENTAL" to "10754064",
        "SECURITY_PATCH" to "2024-03-05"
    )

    /**
     * Injects synthetic build properties into android.os.Build and android.os.Build$VERSION.
     */
    @Suppress("TooGenericExceptionCaught")
    fun initializeBuildFields(
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader
            ?: AndroidBuildBootstrap::class.java.classLoader
    ) {
        val unsafe = getUnsafeInstance() ?: return

        try {
            val buildClass = Class.forName("android.os.Build", true, classLoader)
            for ((name, value) in stringBuildFields) {
                setStaticFieldIfNull(unsafe, buildClass, name, value)
            }
        } catch (_: Throwable) {
            // Ignored if android.os.Build is not on this classloader
        }

        try {
            val versionClass = Class.forName("android.os.Build\$VERSION", true, classLoader)
            for ((name, value) in versionStringFields) {
                setStaticFieldIfNull(unsafe, versionClass, name, value)
            }
            setIntStaticFieldIfZero(unsafe, versionClass, "SDK_INT", 35)
        } catch (_: Throwable) {
            // Ignored if android.os.Build$VERSION is not on this classloader
        }
    }

    private fun setStaticFieldIfNull(unsafe: Unsafe, targetClass: Class<*>, fieldName: String, value: String) {
        try {
            val field: Field = targetClass.getDeclaredField(fieldName)
            val base = unsafe.staticFieldBase(field)
            val offset = unsafe.staticFieldOffset(field)
            if (unsafe.getObject(base, offset) == null) {
                unsafe.putObject(base, offset, value)
            }
        } catch (_: Throwable) {}
    }

    private fun setIntStaticFieldIfZero(unsafe: Unsafe, targetClass: Class<*>, fieldName: String, value: Int) {
        try {
            val field: Field = targetClass.getDeclaredField(fieldName)
            val base = unsafe.staticFieldBase(field)
            val offset = unsafe.staticFieldOffset(field)
            if (unsafe.getInt(base, offset) == 0) {
                unsafe.putInt(base, offset, value)
            }
        } catch (_: Throwable) {}
    }

    private fun getUnsafeInstance(): Unsafe? {
        return try {
            val theUnsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
            theUnsafeField.isAccessible = true
            theUnsafeField.get(null) as Unsafe
        } catch (_: Throwable) {
            null
        }
    }
}
