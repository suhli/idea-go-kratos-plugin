package com.github.suhli.ideagokratosplugin.extends

import com.goide.psi.*
import com.goide.psi.impl.GoPackage
import com.goide.sdk.GoPackageUtil
import com.intellij.psi.ResolveState
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
            if(returns is GoTypeList){
                for(type in returns.typeList){
                    list.add(goTypeToProviderType(type) ?: continue)
                }
            }else{
                val type = goTypeToProviderType(returns) ?: return list
                list.add(type)
            }
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
