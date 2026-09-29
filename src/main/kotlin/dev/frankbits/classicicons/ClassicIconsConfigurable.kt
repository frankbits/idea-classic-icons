package dev.frankbits.classicicons

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows

class ClassicIconsConfigurable : BoundConfigurable("Classic Icons") {
    private val settings get() = ClassicIconsSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        buttonsGroup("Use classic icons for:") {
            row { radioButton("All icons", IconScope.ALL) }
            row { radioButton("Only files and folders (file types, project tree)", IconScope.FILES_AND_FOLDERS) }
        }.bind(settings::scope)
        row("Additional file icon filters:") {
            textArea()
                .rows(4)
                .align(AlignX.FILL)
                .bindText(settings::extraFilters)
                .comment("One path fragment per line (e.g. MarkdownPlugin). Only used in \"Only files and folders\" mode.")
        }
        row { comment("Changes apply immediately; a restart may be needed for already cached icons.") }
    }

    override fun apply() {
        super.apply()
        IconLoader.clearCache()
    }
}
