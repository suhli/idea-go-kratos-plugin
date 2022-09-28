package com.github.suhli.ideaplugingodemo

import com.goide.GoFileType
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoPackageClause
import com.goide.psi.impl.GoPackageClauseImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory

class AutowireRunner : DumbAwareAction() {
    companion object{
        const val ID = "runAutowiredFileAction"
    }

    private fun collectProviders(p: PsiFile): ArrayList<GoFunctionDeclaration> {
        val providers = arrayListOf<GoFunctionDeclaration>()
        var flg = false
        for(child in p.children){
            if (child is PsiComment && child.text.contains(GoAutowire.TOKEN)){
                flg = true
            }
            if(flg && child is GoFunctionDeclaration){
                providers.add(child)
            }
        }
        return providers
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val dir = file.containingDirectory

        val pkg = (file.children.find { v -> v is GoPackageClause } ?: return) as GoPackageClauseImpl

        val fileName = "${pkg.name}.wire.go"
        val providers = arrayListOf<GoFunctionDeclaration>()
        for (file in dir.files) {
            providers.addAll(collectProviders(file))
        }
        val providerTokens = providers.map { v ->
            v.name
        }.joinToString(",")
        val result = """
            package ${pkg.name}
            
            import (
                "github.com/google/wire"
            )
            
            var ${pkg.name?.replaceFirstChar { v -> v.toUpperCase() }}ProviderSets = wire.NewSet($providerTokens)
        """.trimIndent()

        val resultFile =
            PsiFileFactory.getInstance(file.project).createFileFromText(fileName, GoFileType.INSTANCE, result)
        val applicationManager = ApplicationManager.getApplication()
        applicationManager.runWriteAction {
            dir.files.find { v -> v.name == fileName }?.delete()
            dir.add(resultFile)
        }
    }
}