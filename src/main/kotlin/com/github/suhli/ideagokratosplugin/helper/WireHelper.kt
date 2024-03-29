package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.KratosConfigFileType
import com.github.suhli.ideagokratosplugin.extends.*
import com.goide.GoFileType
import com.goide.formatter.GoFormatterUtil
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoPointerType
import com.goide.psi.GoType
import com.goide.psi.impl.GoPackage
import com.goide.sdk.GoSdkUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import java.nio.charset.Charset

private const val TOKEN = "wired"

private var LOG: Logger? = null
private fun getLogger(): Logger {
    if (LOG == null) {
        LOG = Logger.getInstance("WireHelper")
    }
    return LOG!!
}

private fun scanForNotProvideTypes(providerSets: List<ProviderSet>): HashSet<ProviderType> {
    val arguments = arrayListOf<ProviderType>()
    val provides = hashSetOf<String>()
    for (providerSet in providerSets) {
        provides.addAll(providerSet.returns.map { v -> v.identifier })
        arguments.addAll(providerSet.arguments)
    }
    val notExistsArguments = hashSetOf<ProviderType>()
    for (arg in arguments) {
        if (!provides.contains(arg.identifier)) {
            notExistsArguments.add(arg)
            provides.add(arg.identifier)
        }
    }
    return notExistsArguments
}

private fun collectProviderSets(dir: PsiDirectory): ArrayList<ProviderSet> {
    val providers = arrayListOf<ProviderSet>()
    for (sub in dir.subdirectories) {
        providers.addAll(collectProviderSets(sub))
    }
    val provider = collectProviderSetInDir(dir)
    if (provider != null) providers.add(provider)
    return providers
}

private fun collectProviderSetInDir(dir: PsiDirectory): ProviderSet? {
    val providers = arrayListOf<Provider>()
    for (file in dir.files) {
        if (file !is GoFile) {
            continue
        }
        providers.addAll(collectProviders(file))
    }
    if (providers.isEmpty()) {
        return null
    }
    return ProviderSet(providers)
}

private fun collectProviders(p: PsiFile): ArrayList<Provider> {
    val providers = arrayListOf<Provider>()
    var flg = false
    for (child in p.children) {
        if (child is PsiComment && child.text.contains(TOKEN)) {
            flg = true
        }
        if (flg && child is GoFunctionDeclaration) {
            providers.add(Provider(child))
            flg = false
        }
    }
    return providers
}

fun genAllWire(p: Project): List<KratosTask> {
    val files = FileTypeIndex.getFiles(KratosConfigFileType.INSTANCE, GlobalSearchScope.projectScope(p))
    val manager = PsiManager.getInstance(p)
    val tasks = arrayListOf<KratosTask>()
    for (file in files) {
        val psi = manager.findFile(file) ?: continue
        tasks.addAll(genWire(psi) ?: continue)
    }
    return tasks
}

fun genWire(file: PsiFile): List<KratosTask>? {
    val dir = file.containingDirectory
    return genWire(dir, KratosConfig.fromLines(file.text.split("\n")))
}

private fun packageToImport(v: GoPackage): String {
    return toImport(v.getImportPath(false)!!)
}

private fun toImport(v: String): String {
    return """"$v""""
}


private fun genWire(dir: PsiDirectory, config: KratosConfig): List<KratosTask>? {

    val project = dir.project
    var targetDir = dir
    if (config.getWireLocation().isNotEmpty()) {
        targetDir = DirHelper.cd(dir, config.getWireLocation()) ?: throw RuntimeException("no such dir")
    }
    val providerSets = collectProviderSets(dir)
    val notExistRequirements = scanForNotProvideTypes(providerSets)
    val injectionImports = providerSets.map { v -> packageToImport(v.goPkg) }
    val injections = providerSets.joinToString(",") { v -> """${v.pkg.name}.${v.name}""" }
    val notExistRequirementsImports = hashSetOf<String>()
    val notExistsRequirementDeclarations = hashSetOf<String>()



    for (notExists in notExistRequirements) {
        notExistRequirementsImports.add(packageToImport(notExists.pkg))
        var declaration = notExists.type.text
        if (!declaration.contains(".")) {
            if (notExists.type is GoPointerType) {
                declaration = "*${notExists.pkg.name}.${declaration.replace("*", "")}"
            } else {
                declaration = "${notExists.pkg.name}.${declaration}"
            }

        }
        notExistsRequirementDeclarations.add(declaration)
    }
    val imports = hashSetOf<String>()
    imports.addAll(injectionImports)
    imports.addAll(notExistRequirementsImports)
    imports.add(toImport("github.com/google/wire"))
    imports.add(toImport("github.com/go-kratos/kratos/v2"))

    val comment = arrayListOf("//go:build wireinject", "// +build wireinject").joinToString("\n") + "\n"
    val plainWire = """
               // The build tag makes sure the stub is not built in the final build.
               package main
               import (
                    ${imports.sorted().joinToString("\n")}
               )
               
               
               // wireApp init kratos application.
               func wireApp(${
        notExistsRequirementDeclarations.sorted().joinToString(",")
    }) (*kratos.App, func(), error){
                    panic(wire.Build($injections, newApp))
               }
            """.trimIndent()
    val wireFileName = "wire.go"
    val wireFile =
        PsiFileFactory.getInstance(targetDir.project)
            .createFileFromText(wireFileName, GoFileType.INSTANCE, plainWire)
    GoFormatterUtil.reformat(wireFile)
    val documentManager = PsiDocumentManager.getInstance(project)

    val exe = GoSdkUtil.findExecutableInGoPath("wire", dir.project, null) ?: return null
    val paths = DirHelper.split(targetDir.virtualFile.canonicalPath!!)
    val cmd = GeneralCommandLine(exe.path, DirHelper.join(*paths))
        .withWorkDirectory(project.basePath)
    val tasks = arrayListOf<KratosTask>()

    tasks.addAll(providerSets.map { providerSet ->
        KratosTask({
            val parent = providerSet.parent
            parent.files.find { v -> v.name == providerSet.fileName }?.delete()
            parent.add(providerSet.buildFile())
            infoConsole(project, "generate ${providerSet.fileName} done")
        }, "Generate Provider Set${providerSet.fileName}", true)
    })
    tasks.add(KratosTask({
        targetDir.files.find { v -> v.name == wireFileName }?.delete()
        val file = targetDir.add(wireFile).containingFile
        val doc = documentManager.getDocument(file)!!
        doc.insertString(0, comment)
        documentManager.commitDocument(doc)
        infoConsole(project, "generate wire.go done")
    }, "Generate Wire File", true))
    tasks.add(KratosTask({
        runAndLog(project, cmd, false)
    }, "Wire Command"))
    return tasks
}