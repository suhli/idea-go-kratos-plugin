package com.github.suhli.ideagokartosplugin

import com.goide.GoFileType
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoPackageClause
import com.goide.psi.GoParameterDeclaration
import com.goide.psi.GoParameters
import com.goide.psi.impl.GoPackageClauseImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*

class ProviderHelper {

    class Provider(val declaration: GoFunctionDeclaration){
        val pkg:GoPackageClause = declaration.containingFile.children.find { v->v is GoPackageClause } as GoPackageClause
        val name = declaration.name
        val path = declaration.containingFile.getImportPath(false)
    }

    class ProviderSet(val providers: ArrayList<Provider>){
        val pkg = providers.first().pkg
        val path = providers.first().path
    }

    companion object {
        const val TOKEN = "wired"

        private fun collectProviderSetArguments(providerSet: ProviderSet){
            for(provider in providerSet.providers){
                val params = provider.declaration.children.first().children.first().children.filterIsInstance<GoParameterDeclaration>()
                println(params[0].type?.text)
//                provider.declaration.
            }
        }

        public fun createWire(dir: PsiDirectory){
            val providerSets = collectWireProviderSets(dir)
            val applicationManager = ApplicationManager.getApplication()
            val requirements = arrayListOf<String>()
            for(providerSet in providerSets){
                val providerTokens = providerSet.providers.map { v ->
                    v.name
                }.joinToString(",")
                val file = providerSet.pkg.containingFile
                val parent = file.containingDirectory
                val fileName = "${providerSet.pkg.name}.wire.go"
                val plainProviderSet = """
                    package ${providerSet.pkg.name}
                    import (
                        "github.com/google/wire"
                    )
                    
                    //kartos plugin generate
                    var ${providerSet.pkg.name?.replaceFirstChar { v -> v.uppercaseChar() }}ProviderSets = wire.NewSet($providerTokens)
                """.trimIndent()
                val providerSetFile =
                    PsiFileFactory.getInstance(dir.project).createFileFromText(fileName, GoFileType.INSTANCE, plainProviderSet)
                applicationManager.runWriteAction {
                    parent.files.find { v -> v.name == fileName }?.delete()
                    parent.add(providerSetFile)
                }

                collectProviderSetArguments(providerSet)
            }
        }

        private fun collectWireProviderSets(dir: PsiDirectory): ArrayList<ProviderSet> {
            val providers = arrayListOf<ProviderSet>()
            for(sub in dir.subdirectories){
                providers.addAll(collectWireProviderSets(sub))
            }
            val provider = collectProviderSets(dir)
            if(provider != null) providers.add(provider)
            return providers
        }

        private fun collectProviderSets(dir: PsiDirectory): ProviderSet? {
            val providers = arrayListOf<Provider>()
            for(file in dir.files){
                if(file !is GoFile){
                    continue
                }
                providers.addAll(collectProviders(file))
            }
            if(providers.isEmpty()){
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