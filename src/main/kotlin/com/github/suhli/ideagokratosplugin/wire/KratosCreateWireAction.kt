package com.github.suhli.ideagokratosplugin.wire

import com.github.suhli.ideagokratosplugin.helper.genWire
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class KratosCreateWireAction : DumbAwareAction("Run Create Wire") {

    companion object {
        const val ID = "com.github.suhli.ideagokratosplugin.KratosCreateWireAction"
    }


    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val task = genWire(file) ?: return
        runKratosTaskInBackground("generate wire", project,task)
    }

}