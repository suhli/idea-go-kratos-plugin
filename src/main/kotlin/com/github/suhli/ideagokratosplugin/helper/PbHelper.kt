package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.extends.KratosTask
import com.github.suhli.ideagokratosplugin.extends.KratosTaskResult
import com.github.suhli.ideagokratosplugin.pb.KratosPbAction
import com.github.suhli.ideagokratosplugin.pb.KratosPbClientAction
import com.goide.go.GoMethodInheritorsSearch
import com.goide.psi.*
import com.goide.psi.impl.imports.GoImportResolver
import com.goide.sdk.GoPackageUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.protobuf.ide.settings.PbProjectSettings
import com.intellij.protobuf.lang.PbFileType
import com.intellij.protobuf.lang.psi.PbFile
import com.intellij.protobuf.lang.psi.PbImportStatement
import com.intellij.protobuf.lang.psi.PbServiceDefinition
import com.intellij.protobuf.lang.psi.PbServiceMethod
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType

private var LOG: Logger? = null
private fun getLogger(): Logger {
    if (LOG == null) {
        LOG = Logger.getInstance("PbHelper")
    }
    return LOG!!
}

private fun findProtoDir(entries: Set<String>, name: String): String? {
    for (e in entries) {
        if (e.startsWith("jar")) {
            continue
        }
        val loc = e.replace("file://", "")
        if (DirHelper.isFileInDir(loc, name)) {
            return loc
        }
    }
    return null
}

private fun setDependsOn(file: PsiFile) {
    val settings = PbProjectSettings.getInstance(file.project)
    val dependsComments = file.children.filter { v -> v is PsiComment && v.text.contains("dependsOn:") }
    val entries = settings.importPathEntries
    val entryPaths =
        settings.importPathEntries.filter { v -> v.location.startsWith("file") }.map { v -> v.location.replace("file://","") }.toHashSet()
    val externalEntries = hashSetOf<String>()
    for (i in dependsComments) {
        val target = i.text.replace("//dependsOn:", "").trim().split(" ")
        if (target.size < 2) {
            getLogger().error("not a effect comment:{}", i.text)
            continue
        }
        val pkg = target[0]
        val path = target[1]
        for (r in GoImportResolver.EP_NAME.extensionList.iterator()) {
            val resolve = r.resolve(pkg, file.project, null, ResolveState.initial()) ?: continue
            if (resolve.isEmpty() || resolve.first().directories.size == 0) {
                continue
            }
            val pkgDir = resolve.first().directories.first().path
            val toAddDir = DirHelper.join(pkgDir, path)
            if (pkgDir.split("@")[0].endsWith(pkg) && !entryPaths.contains(toAddDir)) {
                externalEntries.add(toAddDir)
                break
            }
        }
    }
    for (i in externalEntries) {
        entries.add(PbProjectSettings.ImportPathEntry("file://$i", ""))
    }
    settings.importPathEntries = entries
}

private fun findDependency(file: PsiFile): HashSet<String> {
    val result = hashSetOf<String>()
    if (file !is PbFile) {
        return result
    }
    setDependsOn(file)
    val settings = PbProjectSettings.getInstance(file.project)
    val imports = file.children.filter { v -> v is PbImportStatement }
    val exists = hashSetOf<String>()
    val entries = settings.importPathEntries.filter { v -> v.location.startsWith("file") }
        .map { v -> v.location.replace("file://", "") }.toSet()
    for (i in imports) {
        val name = i.children[0].text.replace("\"", "")
        val loc = findProtoDir(entries, name) ?: continue
        exists.add(loc)
        result.add("--proto_path=${loc}")
    }
    return result
}

private fun additionalArgs(file: PsiFile): HashSet<String> {
    val result = hashSetOf<String>()
    if (file !is PbFile) {
        return result
    }
    val dependsComments = file.children.filter { v -> v is PsiComment && v.text.contains("additional:") }
    for (comment in dependsComments) {
        val text = comment.text
        val match = Regex("additional:(.+)").find(text)
        val additional = match?.groupValues?.find { m -> !m.contains("additional") } ?: ""
        if (additional.isNotEmpty()) {
            result.add(additional)
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
    cmds.addAll(additionalArgs(file))
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
    cmds.addAll(additionalArgs(file))
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