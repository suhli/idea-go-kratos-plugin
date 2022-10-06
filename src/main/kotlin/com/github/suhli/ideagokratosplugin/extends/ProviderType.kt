package com.github.suhli.ideagokratosplugin.extends

import com.goide.psi.GoType
import com.goide.psi.impl.GoPackage

class ProviderType(val type: GoType, val pkg: GoPackage) {
    val identifier: String
        get() {
            val text = type.text
            var name = text.split('.').last()
            if (name.startsWith("*")) name = name.substring(1)
            return "${pkg.getImportPath(false).toString()}.${name}"
        }
}