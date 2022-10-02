package com.github.suhli.ideagokratosplugin

import com.goide.GoIcons
import com.intellij.openapi.fileTypes.FileType
import javax.swing.Icon

class KratosConfigFileType:FileType {
    companion object{
        val INSTANCE: KratosConfigFileType = KratosConfigFileType()
    }


    override fun getName(): String {
        return "kratos"
    }

    override fun getDescription(): String {
        return "Kratos Generate Config"
    }

    override fun getDefaultExtension(): String {
        return "kratos"
    }

    override fun getIcon(): Icon {
        return GoIcons.ICON
    }

    override fun isBinary(): Boolean {
        return false
    }
}