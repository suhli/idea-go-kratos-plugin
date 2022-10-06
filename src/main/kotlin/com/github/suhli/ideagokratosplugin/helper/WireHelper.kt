package com.github.suhli.ideagokratosplugin.helper

import com.github.suhli.ideagokratosplugin.KratosConfigFileType
import com.github.suhli.ideagokratosplugin.extends.*
import com.goide.GoFileType
import com.goide.formatter.GoFormatterUtil
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionDeclaration
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
private val LOG = Logger.getInstance("WireHelper")

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
    return """"${v.getImportPath(false)}""""
}


private fun genWire(dir: PsiDirectory, config: KratosConfig): List<KratosTask>? {

    val project = dir.project
    var targetDir = dir
    if (config.wireLocation.isNotEmpty()) {
        targetDir = DirHelper.cd(dir, config.wireLocation) ?: throw RuntimeException("no such dir")
    }
    val providerSets = collectProviderSets(dir)
    val notExistRequirements = scanForNotProvideTypes(providerSets)
    val injectionImports = providerSets.joinToString("\n") { v -> packageToImport(v.goPkg) }
    val injections = providerSets.joinToString(",") { v -> """${v.pkg.name}.${v.name}""" }
    val notExistRequirementsImports = arrayListOf<String>()
    val notExistsRequirementDeclarations = arrayListOf<String>()

    for (notExists in notExistRequirements) {
        notExistRequirementsImports.add(packageToImport(notExists.pkg))
        notExistsRequirementDeclarations.add(notExists.type.text)
    }
    val comment = arrayListOf("//go:build wireinject", "// +build wireinject").joinToString("\n") + "\n"
    val plainWire = """
               // The build tag makes sure the stub is not built in the final build.
               package main
               import (
                    ${notExistRequirementsImports.joinToString("\n")}
                    $injectionImports
                    "github.com/google/wire"
                    "github.com/go-kratos/kratos/v2"
                    "os"
                    "github.com/go-kratos/kratos/v2/transport/grpc"
                    "github.com/go-kratos/kratos/v2/transport/http"
                    
               )
               
               var (
               	// Name is the name of the compiled software.
               	Name string
               	// Version is the version of the compiled software.
               	Version string

               	id, _ = os.Hostname()
               )
               
               func newApp(logger log.Logger, gs *grpc.Server, hs *http.Server) *kratos.App {
               	return kratos.New(
               		kratos.ID(id),
               		kratos.Name(Name),
               		kratos.Version(Version),
               		kratos.Metadata(map[string]string{}),
               		kratos.Logger(logger),
               		kratos.Server(
               			gs,
               			hs,
               		),
               	)
               }
               
               // wireApp init kratos application.
               func wireApp(${notExistsRequirementDeclarations.sorted().joinToString(",")}) (*kratos.App, func(), error){
                    panic(wire.Build($injections, newApp))
               }
            """.trimIndent()
    val wireFileName = "wire.go"
    val content =
        PsiFileFactory.getInstance(targetDir.project)
            .createFileFromText(wireFileName, GoFileType.INSTANCE, plainWire)

    GoFormatterUtil.reformat(content)
    val wireFile = PsiFileFactory.getInstance(targetDir.project)
        .createFileFromText(wireFileName, GoFileType.INSTANCE, comment + content.text)
    val exe = GoSdkUtil.findExecutableInGoPath("wire", dir.project, null) ?: return null
    val cmd = GeneralCommandLine(exe.path, targetDir.virtualFile.canonicalPath)
        .withCharset(Charset.forName("UTF-8"))
        .withWorkDirectory(project.basePath)
    val tasks = arrayListOf<KratosTask>()

    tasks.addAll(providerSets.map { providerSet ->
        KratosTask({
            val parent = providerSet.parent
            parent.files.find { v -> v.name == providerSet.fileName }?.delete()
            parent.add(providerSet.buildFile())
        }, "Generate Provider Set${providerSet.fileName}", true)
    })
    tasks.add(KratosTask({
        targetDir.files.find { v -> v.name == wireFileName }?.delete()
        targetDir.add(wireFile)

    }, "Generate Wire File", true))
    tasks.add(KratosTask({
        LOG.debug("will run:${cmd.commandLineString}")
        val result = ExecUtil.execAndGetOutput(cmd)
        LOG.debug("wire command result:")
        LOG.debug(result.stderr)
    }, "Wire Command"))
    return tasks
}