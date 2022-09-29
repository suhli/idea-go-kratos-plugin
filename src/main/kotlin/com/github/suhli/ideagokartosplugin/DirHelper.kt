package com.github.suhli.ideagokartosplugin

import com.intellij.psi.PsiDirectory

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
    }
}