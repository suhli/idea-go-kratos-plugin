package com.github.suhli.ideagokratosplugin

import com.github.suhli.ideagokratosplugin.pb.KratosPbClientAction
import com.github.suhli.ideagokratosplugin.pb.KratosPbAction
import com.github.suhli.ideagokratosplugin.wire.KratosCreateWireAction
import com.goide.GoFileType
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.protobuf.lang.PbFileType
import com.intellij.protobuf.lang.psi.PbServiceBody
import com.intellij.protobuf.lang.psi.PbServiceMethod
import com.intellij.protobuf.lang.psi.ProtoLeafElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FileTypeIndex

class KratosRunCommentMarkerContributor : RunLineMarkerContributor(), DumbAware {

    companion object {
        val Log = Logger.getInstance(KratosRunCommentMarkerContributor::class.java)
    }

    override fun getInfo(element: PsiElement): Info? {
        val file = element.containingFile
        val type = FileTypeIndex.getIndexedFileType(file.virtualFile, element.project)
        if (type is PbFileType) {
            if (element is PsiComment) {
                if (element.text.contains(KratosPbClientAction.TOKEN)) {
                    return Info(
                        AllIcons.RunConfigurations.TestState.Run, null, ActionManager.getInstance().getAction(
                            KratosPbClientAction.ID
                        )
                    )
                }
                if (element.text.contains(KratosPbAction.TOKEN)) {
                    return Info(
                        AllIcons.RunConfigurations.TestState.Run, null, ActionManager.getInstance().getAction(
                            KratosPbAction.ID
                        )
                    )
                }
            }
        }
        if (type is KratosConfigFileType) {
            return Info(
                AllIcons.RunConfigurations.TestState.Run, null, ActionManager.getInstance().getAction(
                    KratosCreateWireAction.ID
                )
            )
        }
        return null
    }
}