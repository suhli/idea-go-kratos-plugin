package com.github.suhli.ideagokratosplugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class KratosAllAction : DumbAwareAction("Kratos All") {
    override fun actionPerformed(e: AnActionEvent) {
        PbHelper.all(e.project ?: return)
        WireHelper.all(e.project ?: return)
    }
}