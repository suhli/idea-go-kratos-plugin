package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.extends.KratosTask
import com.github.suhli.ideagokratosplugin.extends.KratosTaskResult
import com.github.suhli.ideagokratosplugin.pb.KratosPbAction
import com.github.suhli.ideagokratosplugin.pb.KratosPbClientAction
import com.goide.go.GoMethodInheritorsSearch
import com.goide.psi.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.protobuf.ide.settings.PbProjectSettings
import com.intellij.protobuf.lang.PbFileType
import com.intellij.protobuf.lang.psi.PbFile
import com.intellij.protobuf.lang.psi.PbServiceDefinition
import com.intellij.protobuf.lang.psi.PbServiceMethod
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType
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
    val parentPath = DirHelper.split(DirHelper.relativeToRoot(file.parent ?: return null) ?: return null)
    val otherPaths = hashSetOf<String>()
    otherPaths.addAll(findDependency(file))
    val cmds = arrayListOf("protoc")
    cmds.addAll(otherPaths)
    cmds.add("--proto_path=${DirHelper.join(project.basePath!!, *parentPath)}")
    cmds.add("--go_out=paths=source_relative:${DirHelper.join(project.basePath!!, *parentPath)}")
    cmds.add(DirHelper.join(project.basePath!!, *parentPath, file.name))
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
    val parentPath = DirHelper.split(DirHelper.relativeToRoot(file.parent ?: return null) ?: return null)
    val otherPaths = hashSetOf<String>()
    otherPaths.addAll(findDependency(file))
    val cmds = arrayListOf("protoc")
    cmds.addAll(otherPaths)
    cmds.add("--proto_path=${DirHelper.join(project.basePath!!, *parentPath)}")
    cmds.add("--go_out=paths=source_relative:${DirHelper.join(project.basePath!!, *parentPath)}")
    cmds.add("--go-http_out=paths=source_relative:${DirHelper.join(project.basePath!!, *parentPath)}")
    cmds.add("--go-grpc_out=paths=source_relative:${DirHelper.join(project.basePath!!, *parentPath)}")
    cmds.add(DirHelper.join(project.basePath!!, *parentPath, file.name))
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


private fun findPbImplementByName(file: PsiFile, name: String, method: String): GoMethodDeclaration? {
    val typeDeclaration = file.childrenOfType<GoTypeDeclaration>()
        .find { v -> v.children[0].firstChild.firstChild.text == "${name}Server" } ?: return null
    val server = typeDeclaration.children.first()?.firstChild?.children?.first() ?: return null
    val m = (server as GoInterfaceType).methods.find { v -> v.name == method } ?: return null
    val param = DefinitionsScopedSearch.SearchParameters(m, GlobalSearchScope.projectScope(file.project), true)
    val results = arrayListOf<GoMethodDeclaration>()
    GoMethodInheritorsSearch().processQuery(param) {
        results.add(it as GoMethodDeclaration)
        true
    }
    return results.find { !it.containingFile.name.endsWith(".pb.go") }

}

fun goToImplementMethod(element: PsiElement) {
    if (element is PbServiceMethod) {
        val file = element.containingFile
        val name = file.name.replace(".proto", "")
        val service = element.findParentOfType<PbServiceDefinition>() ?: return
        val generated = file.containingDirectory.findFile("${name}_grpc.pb.go") ?: return
        val result = findPbImplementByName(
            generated,
            service.name ?: return,
            element.name?.replaceFirstChar { it.uppercase() } ?: return) ?: return
        OpenFileDescriptor(
            file.project,
            result.containingFile.virtualFile,
            result.startOffsetInParent
        ).navigate(true)
    }
}

fun findImplementMethodFromCli(element: GoReferenceExpression): GoMethodDeclaration? {
    val declareMethod = element.reference.getOnlyResolveResult(null)?.element ?: return null
    val file = declareMethod.containingFile
    if (!file.name.endsWith(".pb.go")) {
        return null
    }
    val cliElem = declareMethod.parent.parent.parent ?: return null;
    if (cliElem is GoTypeSpec) {
        val serverName = cliElem.name?.replace("Client", "") ?: return null
        return findPbImplementByName(file, serverName, element.reference.canonicalText)
    }
    return null;
}