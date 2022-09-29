package com.github.suhli.ideagokartosplugin.wire

import com.github.suhli.ideagokartosplugin.ProviderHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiFile

class CreateWireAction : DumbAwareAction(){

    companion object{
        const val ID = "RunCreateWireAction"
    }


    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val dir = file.containingDirectory
        ProviderHelper.createWire(dir)
    }

}