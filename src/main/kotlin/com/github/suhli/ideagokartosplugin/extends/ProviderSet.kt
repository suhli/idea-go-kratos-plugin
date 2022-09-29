package com.github.suhli.ideagokartosplugin.extends

import com.github.suhli.ideagokartosplugin.WireHelper
import com.intellij.psi.PsiFile


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