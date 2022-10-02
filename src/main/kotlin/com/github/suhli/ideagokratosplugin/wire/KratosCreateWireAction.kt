package com.github.suhli.ideagokratosplugin.wire

import com.github.suhli.ideagokratosplugin.WireHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiFile

class KratosCreateWireAction : DumbAwareAction("Run Create Wire"){

    companion object{
        const val ID = "com.github.suhli.ideagokratosplugin.KratosCreateWireAction"
    }


    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        WireHelper.createWireByConfigFile(file)
    }

}