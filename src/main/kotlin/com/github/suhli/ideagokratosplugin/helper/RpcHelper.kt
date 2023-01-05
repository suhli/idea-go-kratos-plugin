package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.extends.KratosRpcField
import com.intellij.httpClient.http.request.HttpRequestFileType
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.protobuf.lang.psi.PbServiceDefinition
import com.intellij.protobuf.lang.psi.PbServiceMethod
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.PathUtil


private fun getFieldFromProtobuf(element: PbServiceMethod): List<KratosRpcField> {
    val arg = element.serviceMethodTypeList.first().messageTypeName
    return KratosRpcField.getFieldsInMessageTypeName(arg)
}


fun createHttpInRestClient(element: PsiElement, method: String, path: String) {
    if (element is PbServiceMethod) {
        var p = path
        val svc = element.parent.parent as PbServiceDefinition
        val pkg = element.pbFile.packageStatement!!.packageName!!.text

        var fields = getFieldFromProtobuf(element)
        val regex = Regex("\\{.+\\}")
        if (path.contains(regex)) {
            val matches = regex.findAll(path)
            for (m in matches) {
                val value = m.value.trim { c->c == '{' || c == '}' }
                fields = fields.filter { v -> v.name != value }
            }
        }
        var body = ""
        if (method == "get" || method == "delete") {
            if (fields.isNotEmpty()) {
                p += "?" + fields.joinToString("&") { v -> v.name + "=" }
            }
        } else {
            body = "{\n" + fields.joinToString(",\n") { v -> v.toJson() } + "\n}\n"
        }
        var req = """
         ### ${pkg}.${svc.name}.${element.name}
         ${method.uppercase()} localhost:8000${p}
         """.trimIndent()
        if(body.isNotEmpty()){
            req += "\n\n" + body
        }
        val project = element.project
        writeRequest(project, "\n" + req)
    }
}

private fun findOrCreateScratchRequest(project: Project): PsiFile? {
    val fileName = PathUtil.makeFileName("kratos-api", HttpRequestFileType.INSTANCE.defaultExtension)
    val fileService = ScratchFileService.getInstance()
    val file: VirtualFile?
    try {
        file = fileService.findFile(
            ScratchRootType.getInstance(),
            fileName,
            ScratchFileService.Option.create_if_missing
        )
    } catch (e: Exception) {
        return null
    }
    val editorManager = FileEditorManager.getInstance(project)
    editorManager.openFile(file, true)
    return PsiManager.getInstance(project).findFile(file)
}

private fun writeRequest(project: Project, request: String) {
    val psi = findOrCreateScratchRequest(project) ?: return
    val documentManager = PsiDocumentManager.getInstance(project)
    val doc = documentManager.getDocument(psi)!!
    val applicationManager = ApplicationManager.getApplication()
    applicationManager.runWriteAction {
        doc.insertString(doc.textLength, request)
        documentManager.commitDocument(doc)
    }
}

fun createRpcInRestClient(element: PsiElement) {
    if (element is PbServiceMethod) {
        val project = element.project
        val method = element.nameIdentifier?.text ?: return
        val svc = element.parent.parent as PbServiceDefinition
        val pkg = element.pbFile.packageStatement!!.packageName!!.text

        val fields = getFieldFromProtobuf(element)
        val body = "{\n" + fields.joinToString(",\n") { v -> v.toJson() } + "\n}\n"
        val request = "\n" + """
                ### ${pkg}.${svc.name}.${element.name}
                GRPC localhost:9000/$pkg.${svc.name}/$method
                """.trimIndent() + "\n\n" + body
        writeRequest(project, request)
    }
}