package com.github.suhli.ideagokratosplugin

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem

class DirHelper {
    companion object{
        fun cd(dir:PsiDirectory,path:String) :PsiDirectory?{
            var target = dir
            val paths = path.split("/")
            for(name in paths){
                target = target.subdirectories.find { v->v.name == name } ?: return null
            }
            return target
        }

        fun relativeToRoot(file: PsiFileSystemItem): String? {
            val project = file.project
            val root = project.basePath ?: return null
            var path = file.virtualFile.path.replace(root,"")
            if(path.startsWith("/")){
                path = path.replaceFirst("/","")
            }
            return path
        }
    }
}