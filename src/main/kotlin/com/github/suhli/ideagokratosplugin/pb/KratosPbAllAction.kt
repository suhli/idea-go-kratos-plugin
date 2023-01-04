package com.github.suhli.ideagokratosplugin.pb

import com.github.suhli.ideagokratosplugin.helper.genAllPb
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAwareAction

class KratosPbAllAction : DumbAwareAction("Run Kratos Pb All") {

    companion object {
        val ID = "com.github.suhli.ideagokratosplugin.KratosPbAllAction"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tasks = genAllPb(project)
        if (tasks.isEmpty()) {
            return
        }
        runKratosTaskInBackground("generate proto buffer and clients", project, tasks,true)
    }
}