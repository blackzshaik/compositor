package io.compositor.parser

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry and catalog manager for discovered Compose previews.
 * Handles incremental updates, indexing, and atomic JSON persistence.
 */
class PreviewRegistry(private val storageFile: File? = null) {

    private val items = ConcurrentHashMap<String, PreviewItem>()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        if (storageFile != null && storageFile.exists() && storageFile.length() > 0) {
            loadFromFile(storageFile)
        }
    }

    /**
     * Generates a deterministic preview identifier.
     */
    fun generateId(moduleName: String, def: PreviewDefinition): String {
        val pkgPrefix = if (def.packageName.isNotBlank()) "${def.packageName}." else ""
        val classPrefix = if (!def.enclosingClass.isNullOrBlank()) "${def.enclosingClass}." else ""
        val target = "$pkgPrefix$classPrefix${def.functionName}"
        val tag = if (!def.parameters.name.isNullOrBlank()) "#${def.parameters.name}" else ""
        return "$moduleName:$target$tag"
    }

    /**
     * Constructs a safe, URL-encoded image route for a preview identifier.
     */
    fun formatImageUrl(previewId: String): String = formatPreviewImageUrl(previewId)

    /**
     * Updates or registers preview definitions for a given source file.
     * Preserves existing render results when applicable.
     */
    @Synchronized
    fun updateFilePreviews(
        filePath: String,
        definitions: List<PreviewDefinition>,
        moduleName: String = "app"
    ): List<PreviewItem> {
        val normalizedTarget = normalizePath(filePath)
        val existingIdsForFile = items.filter { (_, item) ->
            normalizePath(item.definition.filePath) == normalizedTarget
        }.keys.toSet()

        val currentIds = mutableSetOf<String>()
        val updatedList = mutableListOf<PreviewItem>()

        for (def in definitions) {
            val id = generateId(moduleName, def)
            currentIds.add(id)

            val existing = items[id]
            val item = PreviewItem(
                id = id,
                module = moduleName,
                definition = def,
                status = existing?.status ?: PreviewRenderStatus.PENDING,
                durationMs = existing?.durationMs,
                lastRenderedAt = existing?.lastRenderedAt,
                errorDetails = existing?.errorDetails,
                imagePath = existing?.imagePath,
                imageUrl = existing?.imageUrl?.takeIf { !it.contains('#') } ?: formatPreviewImageUrl(id),
                rootBounds = existing?.rootBounds
            )
            items[id] = item
            updatedList.add(item)
        }

        // Clean up removed previews from this file
        for (oldId in existingIdsForFile) {
            if (oldId !in currentIds) {
                items.remove(oldId)
            }
        }

        storageFile?.let { persist(it) }
        return updatedList
    }

    /**
     * Removes all previews associated with a deleted source file.
     */
    @Synchronized
    fun removeFile(filePath: String): Int {
        val normalizedTarget = normalizePath(filePath)
        val toRemove = items.filter { (_, item) ->
            normalizePath(item.definition.filePath) == normalizedTarget
        }.keys

        var removedCount = 0
        for (id in toRemove) {
            if (items.remove(id) != null) {
                removedCount++
            }
        }

        if (removedCount > 0) {
            storageFile?.let { persist(it) }
        }
        return removedCount
    }

    /**
     * Updates the rendering state of an individual preview item using a structured update payload.
     */
    @Synchronized
    fun updateRenderStatus(
        previewId: String,
        update: RenderStateUpdate
    ): PreviewItem? {
        val current = items[previewId] ?: return null
        val currentUrl = current.imageUrl
        val safeFallback = if (currentUrl != null && !currentUrl.contains('#')) {
            currentUrl
        } else {
            formatPreviewImageUrl(previewId)
        }
        val updated = current.copy(
            status = update.status,
            durationMs = update.durationMs ?: current.durationMs,
            lastRenderedAt = System.currentTimeMillis(),
            imagePath = update.imagePath ?: current.imagePath,
            imageUrl = update.imageUrl?.takeIf { !it.contains('#') } ?: safeFallback,
            errorDetails = update.errorDetails,
            rootBounds = update.rootBounds ?: current.rootBounds
        )
        items[previewId] = updated
        storageFile?.let { persist(it) }
        return updated
    }

    /**
     * Convenience method to update render status with standard parameters.
     */
    @Synchronized
    fun updateRenderStatus(
        previewId: String,
        status: PreviewRenderStatus,
        durationMs: Long? = null,
        imagePath: String? = null
    ): PreviewItem? = updateRenderStatus(
        previewId = previewId,
        update = RenderStateUpdate(
            status = status,
            durationMs = durationMs,
            imagePath = imagePath
        )
    )

    /**
     * Returns an immutable snapshot of the current preview catalog with indices.
     */
    @Synchronized
    fun getCatalog(): PreviewCatalog {
        val currentMap = items.toMap()
        val byModule = mutableMapOf<String, MutableList<String>>()
        val byFile = mutableMapOf<String, MutableList<String>>()
        val byGroup = mutableMapOf<String, MutableList<String>>()

        for ((id, item) in currentMap) {
            byModule.getOrPut(item.module) { mutableListOf() }.add(id)
            byFile.getOrPut(item.definition.filePath) { mutableListOf() }.add(id)
            val groupKey = item.definition.parameters.group ?: "Default"
            byGroup.getOrPut(groupKey) { mutableListOf() }.add(id)
        }

        return PreviewCatalog(
            previews = currentMap,
            byModule = byModule,
            byFile = byFile,
            byGroup = byGroup,
            totalCount = currentMap.size
        )
    }

    /**
     * Atomically serializes the preview catalog to the target file.
     */
    @Synchronized
    fun persist(targetFile: File) {
        val catalog = getCatalog()
        val serialized = json.encodeToString(catalog)
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val tempFile = File.createTempFile("previews", ".tmp", parent ?: File("."))
        try {
            tempFile.writeText(serialized)
            Files.move(
                tempFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (ignored: Exception) {
            // Fallback to direct write if atomic move fails across filesystems
            targetFile.writeText(serialized)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Loads catalog from an existing JSON file.
     */
    @Synchronized
    fun loadFromFile(file: File) {
        if (!file.exists()) return
        val content = file.readText()
        if (content.isBlank()) return

        val loaded = json.decodeFromString<PreviewCatalog>(content)
        items.clear()
        for ((key, item) in loaded.previews) {
            val sanitized = if (item.imageUrl != null && item.imageUrl.contains('#')) {
                item.copy(imageUrl = formatPreviewImageUrl(item.id))
            } else {
                item
            }
            items[key] = sanitized
        }
    }

    companion object {
        fun normalizePath(rawPath: String): String =
            rawPath.replace('\\', '/').trim()
    }
}

/**
 * Constructs a safe, URL-encoded image route for a preview identifier.
 */
fun formatPreviewImageUrl(previewId: String): String {
    val encoded = try {
        java.net.URLEncoder.encode(previewId, java.nio.charset.StandardCharsets.UTF_8.name())
            .replace("+", "%20")
    } catch (_: IllegalArgumentException) {
        previewId.replace("#", "%23")
    }
    return "/api/previews/$encoded/image"
}
