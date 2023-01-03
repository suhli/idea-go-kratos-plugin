package com.github.suhli.ideagokratosplugin.helper

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import java.io.File

class DirHelper {
    companion object{

        fun join(dir:String,vararg target:String): String {
            val dirs = arrayListOf<String>()
            dirs.addAll(dir.split(File.separator))
            dirs.addAll(target)
            return dirs.joinToString(File.separator)
        }

        fun cd(dir:PsiDirectory,path:String) :PsiDirectory?{
            var target = dir
            val paths = path.split(File.separator)
            for(name in paths){
                target = target.subdirectories.find { v->v.name == name } ?: return null
            }
            return target
        }

        fun relativeToRoot(file: PsiFileSystemItem): String? {
            val project = file.project
            val root = project.basePath ?: return null
            var path = file.virtualFile.path.replace(root,"")
            if(path.startsWith(File.separator)){
                path = path.replaceFirst(File.separator,"")
            }
            return path
        }
    }
}