package com.github.suhli.ideagokratosplugin.pb

import com.github.suhli.ideagokratosplugin.helper.genClientTask
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiFile

class KratosPbClientAction : DumbAwareAction("Run Kratos Client") {
    companion object {
        const val ID = "com.github.suhli.ideagokratosplugin.KratosPbClientAction"
        const val TOKEN = "kratos:client"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val task = genClientTask(file) ?: return
        val project = e.project ?: return
        runKratosTaskInBackground("generate kratos clients", project, arrayListOf(task))
    }
}