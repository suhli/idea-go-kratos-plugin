package com.github.suhli.ideagokratosplugin.marker

import com.github.suhli.ideagokratosplugin.helper.goToImplementMethod
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.protobuf.lang.psi.ProtoLeafElement
import com.intellij.psi.PsiElement

class FindProtoServiceImplementMarkerProvider: LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element is ProtoLeafElement && element.text == "rpc") {
            val handler =
                GutterIconNavigationHandler<ProtoLeafElement> { _, _ -> goToImplementMethod(element.parent) }
            return LineMarkerInfo(
                element,
                element.textRange,
                AllIcons.Gutter.ImplementedMethod,
                null, handler, GutterIconRenderer.Alignment.LEFT
            ) { "RUN GRPC" }
        }
        return null
    }
}