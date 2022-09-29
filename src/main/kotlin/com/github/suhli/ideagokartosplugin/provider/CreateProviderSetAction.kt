package com.github.suhli.ideagokartosplugin.provider

import com.github.suhli.ideagokartosplugin.ProviderHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiFile

class CreateProviderSetAction : DumbAwareAction() {
    companion object{
        const val ID = "RunCreateProviderSetAction"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val dir = file.containingDirectory
        ProviderHelper.createProviderSet(dir)

    }
}