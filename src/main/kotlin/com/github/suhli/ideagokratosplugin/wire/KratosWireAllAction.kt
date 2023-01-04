package com.github.suhli.ideagokratosplugin.wire

import com.github.suhli.ideagokratosplugin.helper.genAllWire
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.runBackgroundableTask

class KratosWireAllAction : AnAction("Run Wire All") {
    companion object {
        const val ID = "com.github.suhli.ideagokratosplugin.KratosWireAllAction"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tasks = genAllWire(project)
        runKratosTaskInBackground("generate all wire", project,tasks,false)
    }
}