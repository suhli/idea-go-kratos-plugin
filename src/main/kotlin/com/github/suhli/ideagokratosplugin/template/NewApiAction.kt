package com.github.suhli.ideagokratosplugin.template

import com.github.suhli.ideagokratosplugin.InputTextDialog
import com.github.suhli.ideagokratosplugin.extends.KratosTask
import com.github.suhli.ideagokratosplugin.extends.KratosTaskResult
import com.github.suhli.ideagokratosplugin.helper.ConfigHelper
import com.github.suhli.ideagokratosplugin.helper.runAndLog
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.goide.sdk.GoSdkUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import java.nio.charset.Charset

class NewApiAction : DumbAwareAction("Kratos New Api") {
    companion object {
        private val LOG = Logger.getInstance(NewApiAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val rootConfig = ConfigHelper.rootConfig(project)
        InputTextDialog(project, "New Api", "Api Name:", { v ->
            val exe = GoSdkUtil.findExecutableInGoPath("kratos", project, null)
            if (exe != null) {
                val cmdList = arrayListOf(exe.path, "proto", "client", v)
                if (rootConfig.layoutRepository.isNotEmpty()) {
                    cmdList.add("-r")
                    cmdList.add(rootConfig.layoutRepository)
                }
                runKratosTaskInBackground("new kratos no mod", project, arrayListOf(KratosTask({
                    runAndLog(project,cmdList)
                }, "new kratos no mod")))
            }


        }, "Create").showAndGet()
    }
}