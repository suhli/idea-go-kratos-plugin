package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.extends.KratosTask
import com.github.suhli.ideagokratosplugin.pb.KratosPbAction
import com.github.suhli.ideagokratosplugin.pb.KratosPbClientAction
import com.goide.sdk.GoSdkUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.protobuf.lang.PbFileType
import com.intellij.protobuf.lang.psi.PbFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import java.nio.charset.Charset
private var LOG:Logger? = null
private fun getLogger(): Logger {
    if(LOG == null){
        LOG = Logger.getInstance("PbHelper")
    }
    return LOG!!
}
private fun findDependency(file: PsiFile): List<String> {
    if (file !is PbFile) {
        return arrayListOf<String>()
    }
    val comments = file.children.filter { v -> v is PsiComment && v.text.contains("depends:") }
    return comments.map { v ->
        val text = v.text
        val match = Regex("depends:(.+)").find(text)
        val path = match?.groupValues?.find { m -> !m.contains("depends") } ?: ""
        if (path.isNotEmpty()) "--proto_path=${path}" else ""
    }.filter { v -> v.isNotEmpty() }
}

fun genPbTask(file: PsiFile): KratosTask? {
    if (file !is PbFile) {
        return null
    }
    val project = file.project
    val parentPath = DirHelper.relativeToRoot(file.parent ?: return null) ?: return null
    val otherPaths = findDependency(file)
    val cmds = arrayListOf("protoc")
    cmds.addAll(otherPaths)
    cmds.add("--proto_path=${project.basePath}/$parentPath")
    cmds.add("--go_out=paths=source_relative:${project.basePath}/$parentPath")
    cmds.add("${project.basePath}/$parentPath/${file.name}")
    val cmd = GeneralCommandLine(cmds)
        .withCharset(Charset.forName("UTF-8"))
        .withWorkDirectory(project.basePath)
    return KratosTask(
        {
            getLogger().info("will run pb command:${cmd.commandLineString}")
            val output = ExecUtil.execAndGetOutput(cmd)
            getLogger().info("pb command code:${output.exitCode} output:${output.stdout} err:${output.stderr}")
        },
        "Generate Client Task"
    )
}

fun genClientTask(file: PsiFile): KratosTask? {
    val path = DirHelper.relativeToRoot(file) ?: return null
    val project = file.project
    val exe = GoSdkUtil.findExecutableInGoPath("kratos", project, null) ?: return null
    val cmd = GeneralCommandLine(exe.path, "proto", "client", path)
        .withCharset(Charset.forName("UTF-8"))
        .withWorkDirectory(project.basePath)
    return KratosTask(
        {
            getLogger().info("will run client command:${cmd.commandLineString}")
            val output = ExecUtil.execAndGetOutput(cmd)
            getLogger().info("client command code:${output.exitCode} output:${output.stdout} err:${output.stderr}")
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
    return tasks
}