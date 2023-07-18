package com.github.suhli.ideagokratosplugin.helper

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import com.intellij.util.ResourceUtil
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class DirHelper {
    companion object {

        fun split(dir: String): Array<String> {
            return dir.split("/").toTypedArray()
        }

        fun join(vararg target: String): String {
            val dirs = arrayListOf<String>()
            dirs.addAll(target)
            return dirs.joinToString(File.separator)
        }

        fun cd(dir: PsiDirectory, path: String): PsiDirectory? {
            var target = dir
            val paths = path.split(File.separator)
            for (name in paths) {
                target = target.subdirectories.find { v -> v.name == name } ?: return null
            }
            return target
        }

        fun relativeToRoot(file: PsiFileSystemItem): String? {
            val project = file.project
            val root = project.basePath ?: return null
            var path = file.virtualFile.path.replace(root, "")
            if (path.startsWith(File.separator)) {
                path = path.replaceFirst(File.separator, "")
            }
            return path
        }

        fun isFileInDir(path: String, name: String): Boolean {
            val p = Paths.get(path)
            val file = VfsUtil.findFile(p, false) ?: return false
            return file.findFileOrDirectory(name) != null
        }
    }
}