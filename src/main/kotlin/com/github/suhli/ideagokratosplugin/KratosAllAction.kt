package com.github.suhli.ideagokratosplugin

import com.github.suhli.ideagokratosplugin.extends.KratosTask
import com.github.suhli.ideagokratosplugin.helper.genAllPb
import com.github.suhli.ideagokratosplugin.helper.genAllWire
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction

class KratosAllAction : DumbAwareAction("Kratos All") {
    companion object {
        private val LOG = Logger.getInstance(KratosAllAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tasks = arrayListOf<KratosTask>()
        tasks.addAll(genAllPb(project))
        tasks.addAll(genAllWire(project))
        if (tasks.isEmpty()) {
            return
        }
        runKratosTaskInBackground("run kratos all",project,tasks,false)
    }
}