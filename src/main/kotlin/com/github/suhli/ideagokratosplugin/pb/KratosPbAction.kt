package com.github.suhli.ideagokratosplugin.pb

import com.github.suhli.ideagokratosplugin.helper.genPbTask
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.protobuf.ide.settings.PbProjectSettings
import com.intellij.psi.PsiFile

class KratosPbAction : DumbAwareAction("Run Kratos Config") {
    companion object {
        const val ID = "com.github.suhli.ideagokratosplugin.KratosPbAction"
        const val TOKEN = "kratos:pb"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val task = genPbTask(file) ?: return
        val project = e.project ?: return
        PbProjectSettings.notifyUpdated(project)
        runKratosTaskInBackground("generate proto buffer", project, arrayListOf(task))
    }
}