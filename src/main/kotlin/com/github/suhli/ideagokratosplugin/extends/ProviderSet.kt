package com.github.suhli.ideagokratosplugin.extends

import com.goide.GoFileType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory


class ProviderSet(val providers: ArrayList<Provider>) {
    val pkg = providers.first().pkg
    val goPkg = providers.first().goPkg
    val name =  """${pkg.name?.replaceFirstChar { v -> v.uppercaseChar() }}ProviderSets"""
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

    private val providerTokens = providers.map { v ->
        v.name
    }.joinToString(",")
    val fileName = "${pkg.name}.set_gen.go"
    private val plainProviderSet: String
        get() {
            return arrayListOf(
                "package ${pkg.name}",
                "import  \"github.com/google/wire\"",
                "",
                "//kratos plugin generate",
                "var ${name} = wire.NewSet($providerTokens)"
            )
                .joinToString("\n")
        }
    val parent = pkg.containingFile.containingDirectory
    fun buildFile(): PsiFile {
        return PsiFileFactory.getInstance(parent.project)
            .createFileFromText(fileName, GoFileType.INSTANCE, plainProviderSet)
    }
}