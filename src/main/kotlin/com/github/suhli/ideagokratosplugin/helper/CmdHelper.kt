package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.extends.KratosTask
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project


fun runKratosTaskInBackground(title:String,project:Project,tasks:List<KratosTask>){
    if(tasks.isEmpty()){
        return
    }
    runBackgroundableTask(title, project, true) {
        it.isIndeterminate = false
        val size = tasks.size
        it.fraction = 0.0
        for ((i, t) in tasks.withIndex()) {
            if(t.needWrite){
                WriteCommandAction.runWriteCommandAction(project, t.runnable)
            }else{
                t.runnable.run()
            }
            it.fraction = ((i + 1) / size).toDouble()
        }
        it.fraction = 1.0
    }
}