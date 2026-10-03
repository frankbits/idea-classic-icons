package dev.frankbits.classicicons

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileTypes.FileTypeManager
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

    /** Records a path without replacing an icon already associated with it. */
    fun recordPath(path: String) {
        synchronized(iconsByPath) { record(path).metadata += IconMetadata.Runtime }
    }

    /** Records a path and its resolved icon. */
    fun recordPath(path: String, icon: Icon) {
        synchronized(iconsByPath) {
            val entry = record(path)
            entry.icon = icon
            entry.metadata += IconMetadata.Runtime
        }
    }

    /** Adds FileTypeManager metadata to the record for [path]. */
    fun recordFileType(path: String, icon: Icon, typeName: String, extension: String) {
        synchronized(iconsByPath) {
            val entry = record(path)
            entry.icon = icon
            entry.metadata += IconMetadata.FileType(typeName, extension)
        }
    }

    /** Adds ActionManager metadata to the record for [path]. */
    fun recordAction(path: String, icon: Icon, actionId: String) {
        synchronized(iconsByPath) {
            val entry = record(path)
            entry.icon = icon
            entry.metadata += IconMetadata.Action(actionId)
        }
    }

    /** Returns a snapshot suitable for displaying the registry in the settings UI. */
    fun iconRecords(): List<IconRecord> =
        synchronized(iconsByPath) { iconsByPath.values.toList() }

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
