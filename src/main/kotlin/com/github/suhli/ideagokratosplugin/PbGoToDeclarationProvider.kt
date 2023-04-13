package com.github.suhli.ideagokratosplugin

import com.github.suhli.ideagokratosplugin.helper.findImplementMethodFromCli
import com.goide.psi.GoReferenceExpression
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.navigation.GotoRelatedItem
import com.intellij.navigation.GotoRelatedProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement


class PbGoToDeclarationProvider : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        val expr = element?.parent ?: return null;
        if (expr is GoReferenceExpression) {
            val declaration = findImplementMethodFromCli(expr)
            if(declaration != null){
                return arrayOf(declaration)
            }
        }
        return null
    }
}