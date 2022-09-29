package com.github.suhli.ideagokartosplugin.wire

import com.github.suhli.ideagokartosplugin.WireHelper
import com.github.suhli.ideagokartosplugin.extends.WireConfig
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiFile

class CreateWireAction : DumbAwareAction("Create wire.go"){

    companion object{
        const val ID = "RunCreateWireAction"
    }


    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        val dir = file.containingDirectory
        WireHelper.createWire(dir, WireConfig.fromLines(file.text.split("\n")))
    }

}