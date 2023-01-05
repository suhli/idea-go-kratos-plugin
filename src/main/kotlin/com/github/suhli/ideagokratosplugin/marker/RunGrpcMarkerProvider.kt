package com.github.suhli.ideagokratosplugin.marker

import com.github.suhli.ideagokratosplugin.helper.createHttpInRestClient
import com.github.suhli.ideagokratosplugin.helper.createRpcInRestClient
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.protobuf.lang.annotation.Proto3Annotator
import com.intellij.protobuf.lang.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType
import java.awt.event.MouseEvent
import java.util.function.Supplier

class RunGrpcMarkerProvider : LineMarkerProvider {
    private val HTTP_KEYS = arrayListOf("get", "put", "post", "delete", "patch")


    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element is ProtoLeafElement && element.text == "rpc") {
            val handler =
                GutterIconNavigationHandler<ProtoLeafElement> { _, _ -> createRpcInRestClient(element.parent) }
            return LineMarkerInfo(
                element,
                element.textRange,
                AllIcons.RunConfigurations.TestState.Run,
                null, handler, GutterIconRenderer.Alignment.LEFT
            ) { "RUN GRPC" }
        }
        if (element is PbOptionName && element.text == "(google.api.http)") {
            val expr = element.findParentOfType<PbOptionExpression>() ?: return null
            val agg = expr.childrenOfType<PbAggregateValue>()[0]
            val texts = agg.childrenOfType<PbTextField>()
            for (text in texts) {
                val key = text.children[0].text
                if (HTTP_KEYS.contains(key)) {
                    val path = (text.children[1] as PbTextStringValue).value
                    val parent = text.findParentOfType<PbServiceMethod>() ?: continue
                    val handler =
                        GutterIconNavigationHandler<PbOptionName> { _, _ -> createHttpInRestClient(parent,key,path) }
                    return LineMarkerInfo(
                        element,
                        element.textRange,
                        AllIcons.RunConfigurations.TestState.Run,
                        null, handler, GutterIconRenderer.Alignment.LEFT
                    ) { "RUN HTTP" }
                }
            }
            val key = texts[0].text
            if (HTTP_KEYS.contains(key)) {
                val path = texts[1].text
                print(path)
            }
        }
        return null
    }
}