package com.github.suhli.ideagokartosplugin.wire

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil

class CreateWireCommentMarkerContributor  : RunLineMarkerContributor(), DumbAware {

    companion object{
        public val FILENAME = ".wire"
    }

    override fun getInfo(element: PsiElement): Info? {
        val file = element.containingFile
        if(file.name != FILENAME){
            return null
        }
        val actions = arrayOf<AnAction>(ActionManager.getInstance().getAction(CreateWireAction.ID))
        return Info(
            AllIcons.RunConfigurations.TestState.Run, actions
        ) { psiElement: PsiElement ->
            StringUtil.join(ContainerUtil.mapNotNull(actions) { action ->
                "Create wire.go"
            }, "\n")
        }
    }
}