package com.tencent.bk.devops.atom.task.utils

import com.tencent.bk.devops.atom.task.enums.OSType
import com.tencent.bk.devops.atom.task.env.AgentEnv.getOS
import java.io.File

object WorkspaceUtils {

    fun getLandun() =
            File(".")

    fun getWorkspace() =
            File(getLandun(), "workspace")

    fun getAgentJar() =
            File(getLandun(), "agent.jar")

    fun getAgentInstallScript(): File {
        val os = getOS()
        return if (os == OSType.WINDOWS) {
            File(getLandun(), "agent_install.bat")
        } else {
            File(getLandun(), "agent_install.sh")
        }
    }

    fun getAgentUnInstallScript(): File {
        val os = getOS()
        return if (os == OSType.WINDOWS) {
            File(getLandun(), "agent_uninstall.bat")
        } else {
            File(getLandun(), "agent_uninstall.sh")
        }
    }

    fun getAgentStartScript(): File {
        val os = getOS()
        return if (os == OSType.WINDOWS) {
            File(getLandun(), "agent_start.bat")
        } else {
            File(getLandun(), "agent_start.sh")
        }
    }

    fun getAgentStopScript(): File {
        val os = getOS()
        return if (os == OSType.WINDOWS) {
            File(getLandun(), "agent_stop.bat")
        } else {
            File(getLandun(), "agent_stop.sh")
        }
    }

    fun getPipelineWorkspace(pipelineId: String, workspace: String): File {
        return if (workspace.isNotBlank()) {
            File(workspace)
        } else {
            File(getWorkspace(), "$pipelineId/src")
        }
    }
}
