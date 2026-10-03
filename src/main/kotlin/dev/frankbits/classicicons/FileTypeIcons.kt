package dev.frankbits.classicicons

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.icons.IconPathProvider
import javax.swing.Icon

/**
 * Registry of icon paths discovered through IntelliJ managers or at runtime.
 *
 * A path is stored only once. Its record can contain several sources and
 * metadata entries because the same icon may be used by multiple managers.
 */
object FileTypeIcons {
    /** Origin used to classify records in the settings UI. */
    enum class Source { MANAGER, RUNTIME }

    /** Metadata supplied by [FileTypeManager]. */
    data class FileTypeMetadata(val typeName: String, val extension: String)
    /** Metadata supplied by [ActionManager]. */
    data class ActionMetadata(val actionId: String)
    /** All known information for one icon path. */
    data class IconRecord(
        val path: String,
        var icon: Icon?,
        val sources: MutableSet<Source> = mutableSetOf(),
        val fileTypes: MutableList<FileTypeMetadata> = mutableListOf(),
        val actions: MutableList<ActionMetadata> = mutableListOf()
    )

    private val iconsByPath = mutableMapOf<String, IconRecord>()

    /** Returns the registered file type names using [path], if known. */
    fun typesFor(path: String): Set<String>? =
        synchronized(iconsByPath) {
            iconsByPath[path]?.fileTypes?.map { it.typeName }?.toSet()
        }

    private fun record(path: String): IconRecord =
        iconsByPath.getOrPut(path) { IconRecord(path, null) }

    /** Records a path without replacing an icon already associated with it. */
    fun recordPath(path: String, source: Source = Source.RUNTIME) {
        synchronized(iconsByPath) { record(path).sources += source }
    }

    /** Records a path and its resolved icon. */
    fun recordPath(path: String, icon: Icon, source: Source = Source.RUNTIME) {
        synchronized(iconsByPath) {
            val entry = record(path)
            entry.icon = icon
            entry.sources += source
        }
    }

    /** Adds FileTypeManager metadata to the record for [path]. */
    fun recordFileType(path: String, icon: Icon, typeName: String, extension: String) {
        synchronized(iconsByPath) {
            val entry = record(path)
            entry.icon = icon
            entry.sources += Source.MANAGER
            if (entry.fileTypes.none { it.typeName == typeName }) {
                entry.fileTypes += FileTypeMetadata(typeName, extension)
            }
        }
    }

    /** Adds ActionManager metadata to the record for [path]. */
    fun recordAction(path: String, icon: Icon, actionId: String) {
        synchronized(iconsByPath) {
            val entry = record(path)
            entry.icon = icon
            entry.sources += Source.MANAGER
            if (entry.actions.none { it.actionId == actionId }) {
                entry.actions += ActionMetadata(actionId)
            }
        }
    }

    /** Returns a snapshot suitable for displaying the registry in the settings UI. */
    fun iconRecords(): List<IconRecord> =
        synchronized(iconsByPath) { iconsByPath.values.toList() }

    /** Refreshes manager-backed records and returns whether metadata changed. */
    fun refresh(): Boolean {
        val before = synchronized(iconsByPath) {
            iconsByPath.values.map { it.path to it.sources.toSet() to it.fileTypes.toList() to it.actions.toList() }
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
            iconsByPath.values.map { it.path to it.sources.toSet() to it.fileTypes.toList() to it.actions.toList() }
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
