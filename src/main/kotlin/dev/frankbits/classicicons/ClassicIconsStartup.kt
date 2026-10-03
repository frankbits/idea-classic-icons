package dev.frankbits.classicicons

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Loads manager metadata and refreshes icons after project startup. */
class ClassicIconsStartup : ProjectActivity {
    /** Refreshes manager records after the project has finished starting. */
    override suspend fun execute(project: Project) {
        if (FileTypeIcons.refresh()) ClassicIconPatcher.refreshUi()
    }
}
