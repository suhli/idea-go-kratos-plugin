package com.github.suhli.ideagokratosplugin.marker

import com.github.suhli.ideagokratosplugin.helper.createRpcInRestClient
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.protobuf.lang.psi.ProtoLeafElement
import com.intellij.psi.PsiElement
import java.awt.event.MouseEvent
import java.util.function.Supplier

class RunGrpcMarkerProvider : LineMarkerProvider {



    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element is ProtoLeafElement && element.text == "rpc") {
            val handler = object :GutterIconNavigationHandler<ProtoLeafElement>{
                override fun navigate(e: MouseEvent?, elt: ProtoLeafElement?) {
                    createRpcInRestClient(element.parent)
                }
            }
            return LineMarkerInfo<ProtoLeafElement>(
                element,
                element.textRange,
                AllIcons.RunConfigurations.TestState.Run,
                null, handler, GutterIconRenderer.Alignment.LEFT,
                Supplier { "RUN GRPC" }
            )
        }
        return null
    }
}