package com.github.suhli.ideagokratosplugin.wire

import com.github.suhli.ideagokratosplugin.WireHelper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class KratosWireAllAction : AnAction("Run Wire All") {
    companion object{
        const val ID = "com.github.suhli.ideagokratosplugin.KratosWireAllAction"
    }
    override fun actionPerformed(e: AnActionEvent) {
        WireHelper.all(e.project ?: return)
    }
}