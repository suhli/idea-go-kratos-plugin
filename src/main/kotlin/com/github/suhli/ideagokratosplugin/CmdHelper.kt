package com.github.suhli.ideagokratosplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView
import java.util.concurrent.Executors

class CmdHelper(val p: Project) {

    private val cmdQueue = arrayListOf<String>()
    val view = TerminalView.getInstance(p)
    val executors = Executors.newSingleThreadExecutor()
    var widget: ShellTerminalWidget? = null
    var hasCreate = false

    init {
        executors.execute(Runnable {
            run()
        })
    }

    private fun getShell(): ShellTerminalWidget? {
        if (widget?.isEnabled != true) {
            val window = ToolWindowManager.getInstance(p).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
            val contentManager = window?.contentManager
            val content = contentManager?.findContent(NAME)
            if (content != null) {
                widget = TerminalView.getWidgetByContent(content) as ShellTerminalWidget
            } else {
                synchronized(this) {
                    if (hasCreate) {
                        return null
                    }
                    hasCreate = true
                    ApplicationManager.getApplication().invokeLater {
                        widget = view.createLocalShellWidget(p.basePath, NAME)
                    }
                }
            }
        }
        return widget
    }

    fun add(cmd: String) {
        synchronized(this) {
            cmdQueue.add(cmd)
        }
    }

    private fun removeOne(): String? {
        synchronized(this) {
            if (cmdQueue.isEmpty()) {
                return null
            }
            return cmdQueue.removeAt(0)
        }
    }

    private fun run() {
        while (true) {
            val shell = getShell()
            if (shell == null) {
                println("shell empty ,waiting")
                Thread.sleep(1000)
                continue
            }
            if (shell.hasRunningCommands()) {
                println("has running cmd ,waiting")
                Thread.sleep(1000)
                continue
            }
            val cmd = removeOne()
            if (cmd?.isNotEmpty() == true) {
                println("will execute:")
                println(cmd)
                println("remain:${cmdQueue.size}")
                shell.executeCommand(cmd)
            }
            Thread.sleep(1000)
        }
    }


    companion object {
        private const val NAME = "KratosCmd"
        private var INSTANCE: CmdHelper? = null
        fun getInstance(p: Project): CmdHelper {
            if (INSTANCE == null) {
                synchronized(CmdHelper::class) {
                    if (INSTANCE == null) {
                        INSTANCE = CmdHelper(p)
                    }
                }
            }
            return INSTANCE!!
        }
    }

}