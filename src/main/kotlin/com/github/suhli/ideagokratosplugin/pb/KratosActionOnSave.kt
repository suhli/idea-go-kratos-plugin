package com.github.suhli.ideagokratosplugin.pb

import com.github.suhli.ideagokratosplugin.helper.genPbTask
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.ActionOnSave
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.protobuf.lang.psi.PbFile
import com.intellij.psi.PsiManager

class KratosActionOnSave : ActionOnSave() {
    override fun isEnabledForProject(project: Project): Boolean {
        return true
    }
    override fun processDocuments(project: Project, documents: Array<out Document>) {
        super.processDocuments(project, documents)
        val file = FileEditorManager.getInstance(project).selectedEditor?.file ?: return
        val f = PsiManager.getInstance(project).findFile(file)
        if(f is PbFile){
            val task = genPbTask(f) ?: return
            runKratosTaskInBackground("gen pb",project, arrayListOf(task))
        }
    }
}