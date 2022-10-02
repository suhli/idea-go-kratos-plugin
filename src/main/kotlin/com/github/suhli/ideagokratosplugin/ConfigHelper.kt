package com.github.suhli.ideagokratosplugin

import com.github.suhli.ideagokratosplugin.extends.KratosConfig
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

class ConfigHelper {
    companion object {
        fun lookUpConfig(file:PsiFile): KratosConfig? {
            val manager = PsiManager.getInstance(file.project)
            var parent = file.parent
            while (parent != null){
                if(!parent.isDirectory){
                    parent = parent.parentDirectory
                    continue
                }
                val files = FileTypeIndex.getFiles(KratosConfigFileType.INSTANCE, GlobalSearchScope.fileScope(file.project,parent.virtualFile))
                if(files.isNotEmpty()){
                    val lines = manager.findFile(files.first())?.text?.split("\n") ?: arrayListOf()
                    return KratosConfig.fromLines(lines)
                }
                parent = parent.parentDirectory
            }
            return null
        }
    }
}