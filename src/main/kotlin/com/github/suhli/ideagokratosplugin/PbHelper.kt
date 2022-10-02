package com.github.suhli.ideagokratosplugin

import com.github.suhli.ideagokratosplugin.pb.KratosPbAction
import com.github.suhli.ideagokratosplugin.pb.KratosPbClientAction
import com.intellij.openapi.project.Project
import com.intellij.protobuf.lang.PbFileType
import com.intellij.protobuf.lang.psi.PbFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.terminal.TerminalView

class PbHelper {
    companion object {

        fun all(p:Project){
            val files = FileTypeIndex.getFiles(PbFileType.INSTANCE, GlobalSearchScope.projectScope(p))
            val manager = PsiManager.getInstance(p)
            for (vf in files) {
                val file = manager.findFile(vf) ?: continue
                val clientPbComment =
                    file.children.find { v -> v is PsiComment && v.text.contains(KratosPbClientAction.TOKEN) }
                if (clientPbComment != null) {
                    println("run client:${file.name}")
                    runClient(file)
                }
                val pbComment = file.children.find { v -> v is PsiComment && v.text.contains(KratosPbAction.TOKEN) }
                if (pbComment != null) {
                    println("run pb:${file.name}")
                    runPb(file)
                }
            }
        }

        fun runClient(file: PsiFile) {
            val path = DirHelper.relativeToRoot(file) ?: return
            val cmd = "kratos proto client $path"
            CmdHelper.getInstance(file.project).add(cmd)
        }

        private fun findDependency(file: PsiFile): List<String> {
            if (file !is PbFile) {
                return arrayListOf<String>()
            }
            val comments = file.children.filter { v -> v is PsiComment && v.text.contains("depends:") }
            return comments.map { v ->
                val text = v.text
                val match = Regex("depends:(.+)").find(text)
                val path = match?.groupValues?.find { v -> !v.contains("depends") } ?: ""
                if (path.isNotEmpty()) "--proto_path=${path}" else ""
            }.filter { v -> v.isNotEmpty() }
        }

        fun runPb(file: PsiFile) {
            if (file !is PbFile) {
                return
            }
            val path = DirHelper.relativeToRoot(file) ?: return
            val parentPath = DirHelper.relativeToRoot(file.parent ?: return) ?: return
            val otherPaths = findDependency(file)
            var protoPath = ""
            if (otherPaths.isNotEmpty()) {
                protoPath += otherPaths.joinToString(" \\\n") + " \\\n"
            }
            protoPath += "--proto_path=./$parentPath \\"
            val cmd = """protoc $protoPath
                   --go_out=paths=source_relative:./$parentPath \
	               ./$parentPath/*.proto
            """.trimIndent()
            CmdHelper.getInstance(file.project).add(cmd)
//            val terminalView = TerminalView.getInstance(file.project)
//            terminalView.createLocalShellWidget(null, "kratos").executeCommand(cmd)
        }
    }
}