package com.github.suhli.ideagokartosplugin.extends

import com.github.suhli.ideagokartosplugin.WireHelper
import com.goide.psi.GoType
import com.goide.psi.impl.GoPackage

class ProviderType(val type: GoType, val pkg: GoPackage) {
    val identifier = "${pkg.getImportPath(false).toString()}.${WireHelper.getRealType(type)}"
}