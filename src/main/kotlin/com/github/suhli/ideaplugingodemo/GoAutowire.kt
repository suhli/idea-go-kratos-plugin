package com.github.suhli.ideaplugingodemo

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil

class GoAutowire : RunLineMarkerContributor(), DumbAware {
    companion object{
        private val LOG: Logger = Logger.getInstance("GoAutowire")
        public val TOKEN = "wired"
    }

    override fun getInfo(element: PsiElement): Info? {
        var file = element.containingFile
        if(file.language.id != "go" ){
            return null
        }
        if(element !is PsiComment){
            return null
        }
        if (!element.text.contains(TOKEN)) {
            return null
        }
        val actions = arrayOf<AnAction>(ActionManager.getInstance().getAction(AutowireRunner.ID))
        return Info(
            AllIcons.RunConfigurations.TestState.Run, actions
        ) { psiElement: PsiElement ->
            StringUtil.join(ContainerUtil.mapNotNull(actions) { action ->
                "Create Wire Provider Set"
            }, "\n")
        }
    }

}