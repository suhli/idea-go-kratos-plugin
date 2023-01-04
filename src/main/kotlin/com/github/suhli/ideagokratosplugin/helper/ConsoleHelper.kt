package com.github.suhli.ideagokratosplugin.helper

import com.goide.GoIcons
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.ToolWindowManagerImpl
import com.intellij.toolWindow.ToolWindowEventSource


private const val TOOL_WINDOW_ID = "kratos"
private val consoleMap = hashMapOf<String, ConsoleView>()
private val toolWindowMap = hashMapOf<String, ToolWindow>()
private fun getToolWindow(project: Project): ToolWindow {
    val windowManager = ToolWindowManager.getInstance(project)
    if (toolWindowMap[project.locationHash] == null) {
        toolWindowMap[project.locationHash] = ToolWindowManager.getInstance(project).registerToolWindow(
            RegisterToolWindowTask.closable(
                TOOL_WINDOW_ID, GoIcons.ICON,
                ToolWindowAnchor.BOTTOM
            )
        )
    }
    if (windowManager.isEditorComponentActive || TOOL_WINDOW_ID !== windowManager.activeToolWindowId) {
        (windowManager as ToolWindowManagerImpl).activateToolWindow(
            TOOL_WINDOW_ID,
            null,
            true,
            ToolWindowEventSource.ActivateActionOther
        )
    }
    return toolWindowMap[project.locationHash]!!
}

private fun getConsole(project: Project): ConsoleView {
    if (consoleMap[project.locationHash] == null) {
        val builder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        builder.setViewer(true)
        consoleMap[project.locationHash] = builder.console
        val toolWindow = getToolWindow(project)
        val content = toolWindow.contentManager.factory.createContent(
            consoleMap[project.locationHash]!!.component,
            "protoc output",
            false
        )
        toolWindow.getContentManager().addContent(content)
    }
    return consoleMap[project.locationHash]!!
}


public fun formatCmd(str: List<String>): Array<String> {
    return formatCmd(*str.toTypedArray())
}

public fun formatCmd(vararg str: String): Array<String> {
    val result = arrayListOf<String>()
    for (i in 1..str.size) {
        var l = str[i - 1]
        if (i == 1) {
            result.add(l)
            continue
        }
        result.add("  $l")
    }
    return result.toTypedArray()
}

public fun runAndLog(project: Project, cmds: List<String>){
    runAndLog(project, cmds,true)
}


public fun runAndLog(project: Project, cmd: GeneralCommandLine){
    runAndLog(project, cmd,true)
}

public fun runAndLog(project: Project, cmds: List<String>,clear:Boolean?) {
    val cmd = GeneralCommandLine(cmds)
        .withWorkDirectory(project.basePath)
        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
    runAndLog(project,cmd,clear)
}
public fun runAndLog(project: Project, cmd: GeneralCommandLine,clear:Boolean?) {
    val output = ExecUtil.execAndGetOutput(cmd)
    WriteCommandAction.runWriteCommandAction(project) {
        if(clear == true){
            clearConsole(project)
        }
        infoConsole(project, cmd.commandLineString)
        if (output.exitCode != 0) {
            if (output.stdout.trim().isNotEmpty()) {
                errorConsole(project, output.stdout)
            }
            if (output.stderr.trim().isNotEmpty()) {
                errorConsole(project, output.stderr)
            }
        } else {
            if (output.stdout.trim().isNotEmpty()) {
                infoConsole(project, output.stdout)
            }
            if (output.stderr.trim().isNotEmpty()) {
                infoConsole(project, output.stderr)
            }
        }
    }
}


public fun infoConsole(project: Project, vararg str: String) {
    val c = getConsole(project)
    for (i in str) {
        c.print(i, ConsoleViewContentType.NORMAL_OUTPUT)
        c.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }
}

public fun infoConsole(project: Project, str: String) {
    infoConsole(project, *str.split("\n").toTypedArray())
}

public fun errorConsole(project: Project, str: String) {
    val c = getConsole(project)
    for (i in str.split("\n")) {
        c.print(i, ConsoleViewContentType.ERROR_OUTPUT)
        c.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }
}

public fun clearConsole(project: Project) {
    val c = getConsole(project)
    c.clear()
}