package com.github.suhli.ideagokratosplugin.pb

import com.github.suhli.ideagokratosplugin.PbHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.protobuf.lang.PbFileType
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

class KratosPbAllAction : DumbAwareAction("Run Kratos Pb All") {

    companion object{
        val ID = "com.github.suhli.ideagokratosplugin.KratosPbAllAction"
    }

    override fun actionPerformed(e: AnActionEvent) {
        PbHelper.all(e.project ?: return)
    }
}