package dev.frankbits.classicicons

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.icons.IconPathProvider
import javax.swing.Icon

/**
 * Registry of icon paths discovered through IntelliJ managers or at runtime.
 *
 * A path is stored only once. Its record can contain several metadata entries
 * because the same icon may be used by multiple managers and at runtime.
 */
object IconRegistry {
    /** Typed information describing how an icon path was discovered. */
    sealed interface IconMetadata {
        /** File type association supplied by [FileTypeManager]. */
        data class FileType(val typeName: String, val extension: String) : IconMetadata

        /** Action association supplied by [ActionManager]. */
        data class Action(val actionId: String) : IconMetadata

        /** Marker for a path observed while IntelliJ resolved an icon. */
        data object Runtime : IconMetadata
    }

    /** All known information for one icon path. */
    data class IconRecord(
        val path: String,
        var icon: Icon?,
        val metadata: MutableSet<IconMetadata> = mutableSetOf()
    )

    private val iconsByPath = mutableMapOf<String, IconRecord>()
    private val observedClassLoaders = mutableSetOf<ClassLoader>()

    /** Returns the registered file type names using [path], if known. */
    fun typesFor(path: String): Set<String>? =
        synchronized(iconsByPath) {
            iconsByPath[path]?.metadata
                ?.filterIsInstance<IconMetadata.FileType>()
                ?.map { it.typeName }
                ?.toSet()
        }

    private fun record(path: String): IconRecord =
        iconsByPath.getOrPut(path) { IconRecord(path, null) }

    private fun updateRecord(path: String, icon: Icon?, metadata: IconMetadata) {
        synchronized(iconsByPath) {
            val entry = record(path)
            val isManagerMetadata = metadata !is IconMetadata.Runtime
            val hasManagerMetadata = entry.metadata.any { it !is IconMetadata.Runtime }
            if (icon != null && (isManagerMetadata || !hasManagerMetadata)) {
                entry.icon = icon
            }
            entry.metadata += metadata
        }
    }

    /** Records a runtime-discovered path without a resolved icon. */
    fun recordPath(path: String) = updateRecord(path, null, IconMetadata.Runtime)

    /** Records a runtime-discovered path and its resolved icon. */
    fun recordPath(path: String, icon: Icon) =
        updateRecord(path, icon, IconMetadata.Runtime)

    /** Records a runtime path and remembers the loader that resolved it. */
    fun recordPath(path: String, icon: Icon?, classLoader: ClassLoader?) {
        classLoader?.let { synchronized(iconsByPath) { observedClassLoaders += it } }
        if (icon != null) {
            recordPath(path, icon)
        } else {
            recordPath(path)
        }
    }

    /** Restores runtime paths cached by a previous IDE session. */
    fun restoreRuntimePaths(paths: Collection<String>) {
        synchronized(iconsByPath) {
            paths.forEach { path ->
                record(path).metadata += IconMetadata.Runtime
            }
        }
    }

    /** Resolves missing preview icons for runtime records using the supplied loaders. */
    fun resolveRuntimeIcons(classLoaders: Collection<ClassLoader>) {
        val records = synchronized(iconsByPath) {
            iconsByPath.values.filter {
                it.icon == null && it.metadata.contains(IconMetadata.Runtime)
            }.toList()
        }
        records.forEach { record ->
            classLoaders.asSequence()
                .mapNotNull { loader ->
                    val resource = loader.getResource(record.path.removePrefix("/"))
                    resource?.let { IconLoader.findIcon(it) }
                }
                .firstOrNull()
                ?.let { recordPath(record.path, it) }
        }
    }

    /** Adds FileTypeManager metadata to the record for [path]. */
    fun recordFileType(path: String, icon: Icon, typeName: String, extension: String) =
        updateRecord(path, icon, IconMetadata.FileType(typeName, extension))

    /** Adds ActionManager metadata to the record for [path]. */
    fun recordAction(path: String, icon: Icon, actionId: String) =
        updateRecord(path, icon, IconMetadata.Action(actionId))

    /** Returns a snapshot suitable for displaying the registry in the settings UI. */
    fun iconRecords(): List<IconRecord> =
        synchronized(iconsByPath) { iconsByPath.values.toList() }

    /** Returns runtime-only paths that cannot be resolved by any supplied loader. */
    fun unresolvedRuntimePaths(classLoaders: Collection<ClassLoader>): List<String> {
        val loaders = classLoaders.toSet()
        return synchronized(iconsByPath) {
            iconsByPath.values
                .filter { record ->
                    record.metadata.contains(IconMetadata.Runtime) &&
                        record.metadata.none { it is IconMetadata.FileType || it is IconMetadata.Action }
                }
                .map { it.path }
                .filter { path ->
                    loaders.none { loader ->
                        loader.getResource(path.removePrefix("/")) != null
                    }
                }
        }
    }

    /** Removes runtime metadata and records for the selected cached paths. */
    fun removeRuntimePaths(paths: Collection<String>) {
        synchronized(iconsByPath) {
            paths.forEach { path ->
                val record = iconsByPath[path] ?: return@forEach
                record.metadata.remove(IconMetadata.Runtime)
                if (record.metadata.isEmpty()) {
                    iconsByPath.remove(path)
                }
            }
        }
    }

    /** Returns classloaders that have resolved registered runtime paths this session. */
    fun observedClassLoaders(): Set<ClassLoader> =
        synchronized(iconsByPath) { observedClassLoaders.toSet() }

    /** Refreshes manager-backed records and returns whether metadata changed. */
    fun refresh(): Boolean {
        val before = synchronized(iconsByPath) {
            iconsByPath.values.map { it.path to it.metadata.toSet() }
        }
        FileTypeManager.getInstance().registeredFileTypes.forEach { type ->
            val icon = type.icon ?: return@forEach
            val path = (icon as? IconPathProvider)?.originalPath ?: return@forEach
            recordFileType(
                "/" + path.removePrefix("/"),
                icon,
                type.name,
                type.defaultExtension ?: ""
            )
        }
        registerActionIcons()
        val after = synchronized(iconsByPath) {
            iconsByPath.values.map { it.path to it.metadata.toSet() }
        }
        return before != after
    }

    private fun registerActionIcons() {
        val actionManager = ActionManager.getInstance()
        actionManager.getActionIdList("").forEach { actionId ->
            val icon = actionManager.getAction(actionId)?.templatePresentation?.icon ?: return@forEach
            val path = (icon as? IconPathProvider)?.originalPath ?: return@forEach
            recordAction("/" + path.removePrefix("/"), icon, actionId)
        }
    }
}
