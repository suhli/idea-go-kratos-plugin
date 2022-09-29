package com.github.suhli.ideagokartosplugin

import com.goide.GoFileType
import com.goide.formatter.GoFormatterUtil
import com.goide.psi.*
import com.goide.psi.impl.GoPackage
import com.goide.psi.impl.GoPackageClauseImpl
import com.goide.sdk.GoPackageUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*

class ProviderHelper {


    class Provider(val declaration: GoFunctionDeclaration) {
        val pkg: GoPackageClause =
            declaration.containingFile.children.find { v -> v is GoPackageClause } as GoPackageClause
        val name = declaration.name
        val path = declaration.containingFile.getImportPath(false)
        val goPkg =
            GoPackageUtil.findByImportPath(path!!, declaration.project, null, ResolveState.initial()).first()
        private val imports: Map<String, GoPackage>

        init {
            imports = hashMapOf<String, GoPackage>()
            val importList = declaration.containingFile.imports
            for (i in importList) {
                val pkg = GoPackageUtil.findByImportPath(i.path, i.project, null, ResolveState.initial()).first()
                if (i.alias != null) {
                    imports.put(i.alias!!, pkg)
                } else {
                    imports.put(pkg.name, pkg)
                }
            }
        }

        private fun goTypeToProviderType(type: GoType?): ProviderType? {
            val text = type?.text
            if (text == null) {
                return null
            }
            if (text.contains(".")) {
                val argList = text.split(".")
                var pkgName = argList.first()
                if(pkgName.startsWith("*")){
                    pkgName = pkgName.substring(1)
                }
                val pkg = imports.get(pkgName) ?: return null
                return ProviderType(type, pkg)
            } else {
                return ProviderType(type, goPkg)
            }
        }

        val returns: List<ProviderType>
            get() {
                val list = arrayListOf<ProviderType>()
                val returns = declaration.resultType
                val type = goTypeToProviderType(returns) ?: return list
                list.add(type)
                return list
            }

        val arguments: List<ProviderType>
            get() {
                val list = arrayListOf<ProviderType>()
                val params =
                    declaration.children.first().children.first().children.filterIsInstance<GoParameterDeclaration>()
                for (param in params) {
                    val type = goTypeToProviderType(param.type) ?: continue
                    list.add(type)
                }
                return list
            }
    }

    class ProviderSet(val providers: ArrayList<Provider>) {
        val pkg = providers.first().pkg
        val goPkg = providers.first().goPkg
        val name =  """${pkg.name?.replaceFirstChar { v -> v.uppercaseChar() }}ProviderSets"""
        var file: PsiFile? = null

        val arguments: List<ProviderType>
        val returns: List<ProviderType>

        init {
            arguments = arrayListOf<ProviderType>()
            returns = arrayListOf<ProviderType>()
            for (provider in providers) {
                arguments.addAll(provider.arguments)
                returns.addAll(provider.returns)
            }
        }
    }

    class ProviderType(val type: GoType, val pkg: GoPackage) {
        val identifier = "${pkg.getImportPath(false).toString()}.${ProviderHelper.getRealType(type)}"
    }

    companion object {
        const val TOKEN = "wired"

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

        fun packageToImport(v:GoPackage): String {
            return """"${v.getImportPath(false)}""""
        }

        public fun createWire(dir: PsiDirectory) {
            val providerSets = collectWireProviderSets(dir)
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
//            val imports = notExistRequirements.map { v->packageToImport(v.pkg) }.joinToString("\n")
            val injectionImports = providerSets.map { v->packageToImport(v.goPkg) }.joinToString("\n")
            val injections = providerSets.map { v->"""${v.pkg.name}.${v.name}""" }.joinToString(",")
//            val notExistsRequirementDeclarations = notExistRequirements.map { v->v. }
            val notExistRequirementsImports = arrayListOf<String>()
            val notExistsRequirementDeclarations = arrayListOf<String>()
            for((index,notExists) in notExistRequirements.withIndex()){
                val name = notExists.pkg.name + index
                notExistRequirementsImports.add("${packageToImport(notExists.pkg)}")
                notExistsRequirementDeclarations.add("${notExists.type.text}")
            }
            val plainWire = """
               //go:build wireinject  
               

               // The build tag makes sure the stub is not built in the final build.
               package main_test
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
            val wireFileName = "wire.gen_test.go"
            val wireFile =
                PsiFileFactory.getInstance(dir.project)
                    .createFileFromText(wireFileName, GoFileType.INSTANCE, plainWire)
            GoFormatterUtil.reformat(wireFile)
            applicationManager.runWriteAction {
                dir.files.find { v -> v.name == wireFileName }?.delete()
                dir.add(wireFile)
            }
        }

        private fun collectWireProviderSets(dir: PsiDirectory): ArrayList<ProviderSet> {
            val providers = arrayListOf<ProviderSet>()
            for (sub in dir.subdirectories) {
                providers.addAll(collectWireProviderSets(sub))
            }
            val provider = collectProviderSet(dir)
            if (provider != null) providers.add(provider)
            return providers
        }

        private fun collectProviderSet(dir: PsiDirectory): ProviderSet? {
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

        private fun lookupPackage(dir: PsiDirectory): GoPackageClauseImpl? {
            val file = dir.files.find { v -> v.fileType is GoFileType }
            return (file?.children?.find { v -> v is GoPackageClause } ?: return null) as GoPackageClauseImpl
        }

        fun createProviderSet(dir: PsiDirectory) {

            val providers = arrayListOf<Provider>()
            for (file in dir.files) {
                providers.addAll(collectProviders(file))
            }
            if (providers.isEmpty()) {
                return
            }
            val pkg = lookupPackage(dir) ?: return
            val fileName = "${pkg.name}.wire.go"

            val providerTokens = providers.map { v ->
                v.name
            }.joinToString(",")
            val result = """
            package ${pkg.name}
            
            import (
                "github.com/google/wire"
            )
            
            //kartos plugin generate
            var ${pkg.name?.replaceFirstChar { v -> v.uppercaseChar() }}ProviderSets = wire.NewSet($providerTokens)
        """.trimIndent()

            val resultFile =
                PsiFileFactory.getInstance(dir.project).createFileFromText(fileName, GoFileType.INSTANCE, result)
            val applicationManager = ApplicationManager.getApplication()
            applicationManager.runWriteAction {
                dir.files.find { v -> v.name == fileName }?.delete()
                dir.add(resultFile)
            }
        }
    }
}