package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.extends.KratosTask
import com.github.suhli.ideagokratosplugin.extends.KratosTaskResult
import com.github.suhli.ideagokratosplugin.pb.KratosPbAction
import com.github.suhli.ideagokratosplugin.pb.KratosPbClientAction
import com.goide.GoIcons
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.ToolWindowManagerImpl
import com.intellij.protobuf.ide.settings.PbProjectSettings
import com.intellij.protobuf.lang.PbFileType
import com.intellij.protobuf.lang.psi.PbFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.toolWindow.ToolWindowEventSource
import java.io.File

private var LOG: Logger? = null
private fun getLogger(): Logger {
    if (LOG == null) {
        LOG = Logger.getInstance("PbHelper")
    }
    return LOG!!
}

private fun findDependency(file: PsiFile): HashSet<String> {
    val result = hashSetOf<String>()
    if (file !is PbFile) {
        return result
    }
    val dependsComments = file.children.filter { v -> v is PsiComment && v.text.contains("depends:") }
    for (comment in dependsComments) {
        val text = comment.text
        val match = Regex("depends:(.+)").find(text)
        var path = match?.groupValues?.find { m -> !m.contains("depends") } ?: ""

        if (path.isNotEmpty()) {
            path = path.split("/").joinToString(File.separator)
            result.add("--proto_path=${path}")
        }
    }
    return result
}

fun genPbTask(file: PsiFile): KratosTask? {
    if (file !is PbFile) {
        return null
    }
    val project = file.project
    val parentPath = DirHelper.relativeToRoot(file.parent ?: return null) ?: return null
    val otherPaths = hashSetOf<String>()
    otherPaths.addAll(findDependency(file))
    val cmds = arrayListOf("protoc")
    cmds.addAll(otherPaths)
    cmds.add("--proto_path=${DirHelper.join(project.basePath!!, *DirHelper.split(parentPath))}")
    cmds.add("--go_out=paths=source_relative:${DirHelper.join(project.basePath!!, *DirHelper.split(parentPath))}")
    cmds.add(DirHelper.join(project.basePath!!, parentPath, file.name))
    return KratosTask(
        {
            runAndLog(project, cmds)
            KratosTaskResult.dismiss()
        },
        "Generate Client Task"
    )
}

fun genClientTask(file: PsiFile): KratosTask? {
    val project = file.project
    val parentPath = DirHelper.relativeToRoot(file.parent ?: return null) ?: return null
    val otherPaths = hashSetOf<String>()
    otherPaths.addAll(findDependency(file))
    val cmds = arrayListOf("protoc")
    cmds.addAll(otherPaths)
    cmds.add("--proto_path=${DirHelper.join(project.basePath!!, *DirHelper.split(parentPath))}")
    cmds.add("--go_out=paths=source_relative:${DirHelper.join(project.basePath!!, *DirHelper.split(parentPath))}")
    cmds.add("--go-http_out=paths=source_relative:${DirHelper.join(project.basePath!!, *DirHelper.split(parentPath))}")
    cmds.add("--go-grpc_out=paths=source_relative:${DirHelper.join(project.basePath!!, *DirHelper.split(parentPath))}")
    cmds.add(DirHelper.join(project.basePath!!, *parentPath.split("/").toTypedArray(), file.name))
    return KratosTask(
        {
            runAndLog(project, cmds)
            KratosTaskResult.dismiss()
        },
        "Generate Client Task"
    )
}

fun genAllPb(p: Project): List<KratosTask> {
    val files = FileTypeIndex.getFiles(PbFileType.INSTANCE, GlobalSearchScope.projectScope(p))
    val manager = PsiManager.getInstance(p)
    val tasks = arrayListOf<KratosTask>()
    for (vf in files) {
        val file = manager.findFile(vf) ?: continue
        val clientPbComment =
            file.children.find { v -> v is PsiComment && v.text.contains(KratosPbClientAction.TOKEN) }
        if (clientPbComment != null) {
            getLogger().info("find client comment:${file.virtualFile.path}")
            tasks.add(genClientTask(file) ?: continue)
        }
        val pbComment = file.children.find { v -> v is PsiComment && v.text.contains(KratosPbAction.TOKEN) }
        if (pbComment != null) {
            getLogger().info("find pb comment:${file.virtualFile.path}")
            tasks.add(genPbTask(file) ?: continue)
        }
    }
    PbProjectSettings.notifyUpdated(p)
    return tasks
}