package com.github.suhli.ideagokartosplugin

import com.github.suhli.ideagokartosplugin.extends.Provider
import com.github.suhli.ideagokartosplugin.extends.ProviderSet
import com.github.suhli.ideagokartosplugin.extends.ProviderType
import com.github.suhli.ideagokartosplugin.extends.WireConfig
import com.goide.GoFileType
import com.goide.formatter.GoFormatterUtil
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoType
import com.goide.psi.impl.GoPackage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView


class WireHelper {
    companion object {
        private const val TOKEN = "wired"

        fun getRealType(type: GoType?): String? {
            val text = type?.text ?: return null
            var name = text.split('.').last()
            if (name.startsWith("*")) name = name.substring(1)
            return name
        }

        private fun scanForNotProvideTypes(providerSets: List<ProviderSet>): HashSet<ProviderType> {
            val arguments = arrayListOf<ProviderType>()
            val provides = hashSetOf<String>()
            for (providerSet in providerSets) {
                provides.addAll(providerSet.returns.map { v->v.identifier })
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

        private fun packageToImport(v:GoPackage): String {
            return """"${v.getImportPath(false)}""""
        }

        fun createWire(dir: PsiDirectory,config:WireConfig) {
            var targetDir = dir
            if(config.location.isNotEmpty()){
                targetDir = DirHelper.cd(dir,config.location) ?: throw RuntimeException("no such dir")
            }
            val providerSets = collectProviderSets(dir)
            val applicationManager = ApplicationManager.getApplication()
            for (providerSet in providerSets) {
                val providerTokens = providerSet.providers.map { v ->
                    v.name
                }.joinToString(",")
                val file = providerSet.pkg.containingFile
                val parent = file.containingDirectory
                val fileName = "${providerSet.pkg.name}.set_gen.go"
                val plainProviderSet = """
                    package ${providerSet.pkg.name}
                    import (
                        "github.com/google/wire"
                    )
                    
                    //kartos plugin generate
                    var ${providerSet.name} = wire.NewSet($providerTokens)
                """.trimIndent()
                val providerSetFile =
                    PsiFileFactory.getInstance(dir.project)
                        .createFileFromText(fileName, GoFileType.INSTANCE, plainProviderSet)
                applicationManager.runWriteAction {
                    parent.files.find { v -> v.name == fileName }?.delete()
                    providerSet.file = parent.add(providerSetFile) as PsiFile?
                }
            }
            val notExistRequirements = scanForNotProvideTypes(providerSets)
            val injectionImports = providerSets.joinToString("\n") { v -> packageToImport(v.goPkg) }
            val injections = providerSets.joinToString(",") { v -> """${v.pkg.name}.${v.name}""" }
            val notExistRequirementsImports = arrayListOf<String>()
            val notExistsRequirementDeclarations = arrayListOf<String>()
            for(notExists in notExistRequirements){
                notExistRequirementsImports.add(packageToImport(notExists.pkg))
                notExistsRequirementDeclarations.add(notExists.type.text)
            }
            val plainWire = """
               //go:build wireinject  
               

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
               func wireApp(${notExistsRequirementDeclarations.joinToString(",")}) (*kratos.App, func(), error){
                    panic(wire.Build($injections, newApp))
               }
            """.trimIndent()
            val wireFileName = "wire.gen.go"
            val wireFile =
                PsiFileFactory.getInstance(targetDir.project)
                    .createFileFromText(wireFileName, GoFileType.INSTANCE, plainWire)
            GoFormatterUtil.reformat(wireFile)
            val cmd = "cd ${targetDir.virtualFile.canonicalPath} && wire"
            applicationManager.runWriteAction {
                targetDir.files.find { v -> v.name == wireFileName }?.delete()
                targetDir.add(wireFile)
                println(cmd)
                val terminalView: TerminalView = TerminalView.getInstance(targetDir.project)
                terminalView.createLocalShellWidget(null,"wire").executeCommand(cmd)
            }
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

    }
}