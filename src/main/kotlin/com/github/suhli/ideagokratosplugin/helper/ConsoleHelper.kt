package com.github.suhli.ideagokratosplugin.helper

import com.goide.GoIcons
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
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

public fun infoConsole(project: Project, str: String) {
    val c = getConsole(project)
    for (i in str.split("\n")) {
        c.print("\n",ConsoleViewContentType.NORMAL_OUTPUT)
        c.print(i, ConsoleViewContentType.NORMAL_OUTPUT)
    }
    c.print("\n",ConsoleViewContentType.NORMAL_OUTPUT)
}

public fun errorConsole(project: Project, str: String) {
    val c = getConsole(project)
    for (i in str.split("\n")) {
        c.print("\n",ConsoleViewContentType.NORMAL_OUTPUT)
        c.print(i, ConsoleViewContentType.ERROR_OUTPUT)
    }
    c.print("\n",ConsoleViewContentType.NORMAL_OUTPUT)
}

public fun clearConsole(project: Project) {
    val c = getConsole(project)
    c.clear()
}