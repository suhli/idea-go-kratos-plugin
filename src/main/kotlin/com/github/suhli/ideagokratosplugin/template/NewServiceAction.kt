package com.github.suhli.ideagokratosplugin.template

import com.github.suhli.ideagokratosplugin.InputTextDialog
import com.github.suhli.ideagokratosplugin.extends.KratosTask
import com.github.suhli.ideagokratosplugin.extends.KratosTaskResult
import com.github.suhli.ideagokratosplugin.helper.ConfigHelper
import com.github.suhli.ideagokratosplugin.helper.DirHelper
import com.github.suhli.ideagokratosplugin.helper.runKratosTaskInBackground
import com.goide.sdk.GoSdkUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.protobuf.lang.psi.PbFile
import com.intellij.psi.PsiFile
import java.nio.charset.Charset

class NewServiceAction : DumbAwareAction("Kratos New Service") {
    companion object {
        private val LOG = Logger.getInstance(NewServiceAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file: PsiFile? = e.getData(CommonDataKeys.PSI_FILE)
        if (file == null || file !is PbFile) {
            Notifications.Bus.notify(
                Notification(
                    "com.github.suhli.ideagokratosplugin",
                    "Run Kratos New Api Filed:No Pb File",
                    NotificationType.ERROR
                ), project
            )
            return
        }
        val path = DirHelper.relativeToRoot(file) ?: ""
        val rootConfig = ConfigHelper.rootConfig(project)
        InputTextDialog(project, "New Service", "Service Name:", { v ->
            val exe = GoSdkUtil.findExecutableInGoPath("kratos", project, null)
            if (exe != null) {
                val cmdList = arrayListOf(exe.path, "proto", "server", path, "-t", v)
                if (rootConfig.layoutRepository.isNotEmpty()) {
                    cmdList.add("-r")
                    cmdList.add(rootConfig.layoutRepository)
                }
                val cmd = GeneralCommandLine(cmdList)
                    .withCharset(Charset.forName("UTF-8"))
                    .withWorkDirectory(project.basePath)
                runKratosTaskInBackground("new kratos no mod", project, arrayListOf(KratosTask({
                    val output = ExecUtil.execAndGetOutput(cmd)
                    LOG.info("run command code:${output.exitCode} output:${output.stdout} err:${output.stderr}")
                    if (output.exitCode != 0) {
                        KratosTaskResult.error(RuntimeException(output.stderr))
                    } else {
                        KratosTaskResult.success()
                    }
                }, "new kratos no mod")))
            }


        }, "Create").showAndGet()
    }
}