package dev.frankbits.classicicons

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Nach dem Start: Dateitypen einlesen und Icons neu auflösen lassen. */
class ClassicIconsStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (FileTypeIcons.refresh()) ClassicIconPatcher.refreshUi()
    }
}
