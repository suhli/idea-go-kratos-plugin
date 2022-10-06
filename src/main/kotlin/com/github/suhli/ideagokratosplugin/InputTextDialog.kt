package com.github.suhli.ideagokratosplugin

import com.esotericsoftware.kryo.NotNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent


class InputTextDialog(
    private val project: Project,
    private val name: String,
    private val fieldName: String,
    private val onApply:(String)->Unit,
    @NotNull private val okText: String
) :
    DialogWrapper(project, false) {

    var value: String = ""

    init {
        super.init()
    }

    override fun getTitle(): String {
        return name
    }


    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row(fieldName) {
                textField().bindText({ value }, { v -> value = v }).onApply { onApply(value) }
            }
        }
        return panel
    }

    override fun isOKActionEnabled(): Boolean {
        return value.isNotEmpty()
    }
}