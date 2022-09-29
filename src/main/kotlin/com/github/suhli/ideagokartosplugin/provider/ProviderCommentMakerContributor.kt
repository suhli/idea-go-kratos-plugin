package com.github.suhli.ideagokartosplugin.provider

import com.github.suhli.ideagokartosplugin.ProviderHelper
import com.goide.GoFileType
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil

class ProviderCommentMakerContributor : RunLineMarkerContributor(), DumbAware {

    override fun getInfo(element: PsiElement): Info? {
        val file = element.containingFile
        if(file.fileType !is GoFileType ){
            return null
        }
        if(element !is PsiComment){
            return null
        }
        if (!element.text.contains(ProviderHelper.TOKEN)) {
            return null
        }
        val actions = arrayOf<AnAction>(ActionManager.getInstance().getAction(CreateProviderSetAction.ID))
        return Info(
            AllIcons.RunConfigurations.TestState.Run, actions
        ) { psiElement: PsiElement ->
            StringUtil.join(ContainerUtil.mapNotNull(actions) { action ->
                "Create Wire Provider Set"
            }, "\n")
        }
    }

}