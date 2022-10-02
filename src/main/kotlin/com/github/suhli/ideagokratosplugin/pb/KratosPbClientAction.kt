package com.github.suhli.ideagokratosplugin.pb

import com.github.suhli.ideagokratosplugin.PbHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.terminal.TerminalView

class KratosPbClientAction : DumbAwareAction("Run Kratos Client") {
    companion object{
        const val ID = "com.github.suhli.ideagokratosplugin.KratosPbClientAction"
        const val TOKEN = "kratos:client"
    }
    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        PbHelper.runClient(file)
    }
}