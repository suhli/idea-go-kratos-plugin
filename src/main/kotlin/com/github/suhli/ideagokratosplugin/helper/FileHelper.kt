package com.github.suhli.ideagokratosplugin.helper


public fun fileName(path:String): String {
    return path.split("/").last()
}