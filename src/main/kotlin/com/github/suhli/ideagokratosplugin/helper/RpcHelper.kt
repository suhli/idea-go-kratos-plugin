package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.extends.KratosRpcField
import com.intellij.httpClient.http.request.HttpRequestFileType
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.protobuf.lang.psi.PbServiceMethod
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.util.PathUtil

fun createRpcInRestClient(element:PsiElement){
    if (element is PbServiceMethod) {
        val arg = element.serviceMethodTypeList.first().messageTypeName
        val fields = KratosRpcField.getFieldsInMessageTypeName(arg)
        val project = element.project
        val method = element.nameIdentifier?.text ?: return
        val pkg = element.pbFile.packageStatement!!.packageName!!.text

        val body = "{\n" + fields.joinToString(",\n") { v -> v.toJson() } + "\n}\n"
        val fileName = PathUtil.makeFileName("kratos-api", HttpRequestFileType.INSTANCE.defaultExtension)
        val fileService = ScratchFileService.getInstance()
        var file: VirtualFile?
        try {
            file = fileService.findFile(
                ScratchRootType.getInstance(),
                fileName,
                ScratchFileService.Option.create_if_missing
            )
        } catch (e: Exception) {
            return
        }
        val psi = PsiManager.getInstance(project).findFile(file)!!
        val documentManager = PsiDocumentManager.getInstance(project)
        val doc = documentManager.getDocument(psi)!!
        val applicationManager = ApplicationManager.getApplication()
        val editorManager = FileEditorManager.getInstance(project)
        editorManager.openFile(file, true)
        val request = "\n" + """
                    ### ${pkg}.${element.name}.${element.name}
                    GRPC localhost:9000/$pkg.${element.name}/$method
                """.trimIndent() + "\n\n" + body
        applicationManager.runWriteAction {
            doc.insertString(doc.textLength, request)
            documentManager.commitDocument(doc)
        }

    }
}