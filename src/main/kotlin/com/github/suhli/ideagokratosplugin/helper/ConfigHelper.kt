package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.KratosConfigFileType
import com.github.suhli.ideagokratosplugin.extends.KratosConfig
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

class ConfigHelper {
    companion object {
        fun rootConfig(project: Project): KratosConfig {
            val file = FileTypeIndex.getFiles(KratosConfigFileType.INSTANCE, GlobalSearchScope.projectScope(project)).minByOrNull { v->v.path.length } ?: return KratosConfig()

            val manager = PsiManager.getInstance(project)
            val lines = manager.findFile(file)?.text?.split("\n") ?: arrayListOf()
            return KratosConfig.fromLines(lines)
        }

        fun lookUpConfig(file: PsiFile): KratosConfig? {
            val manager = PsiManager.getInstance(file.project)
            var parent = file.parent
            while (parent != null) {
                if (!parent.isDirectory) {
                    parent = parent.parentDirectory
                    continue
                }
                val virtualFile = FileTypeIndex.getFiles(
                    KratosConfigFileType.INSTANCE,
                    GlobalSearchScope.fileScope(file.project, parent.virtualFile)
                ).minByOrNull { v -> v.path.length }
                if(virtualFile == null){
                    parent = parent.parentDirectory
                    continue
                }
                val lines = manager.findFile(virtualFile)?.text?.split("\n") ?: arrayListOf()
                return KratosConfig.fromLines(lines)

            }
            return null
        }
    }
}