package com.tencent.bk.devops.atom.task.utils

import com.tencent.bk.devops.atom.task.DockerAtomParam
import com.tencent.bk.devops.atom.task.utils.TicketUtils.getTicketUserAndPass
import com.tencent.bk.devops.atom.task.utils.shell.CommonShellUtils
import java.io.File

object DockerUtils {

    fun dockerLogin(loginIp: String, ticketId: String, workspace: File) {
        val pair = getTicketUserAndPass(ticketId)
        val username = pair.first
        val password = pair.second
        // WARNING! Using --password via the CLI is insecure. Use --password-stdin.
        val commandStr = "docker login  --username $username --password $password $loginIp"
        println("docker login  --username $username --password ***** $loginIp")
        CommonShellUtils.execute(commandStr, workspace)
    }

    fun dockerLogout(loginIp: String, workspace: File) {
        val commandStr = "docker logout $loginIp"
        println("[execute script]: $commandStr")
        CommonShellUtils.execute(commandStr, workspace)
    }

    fun dockerPush(dockerAtomParam: DockerAtomParam, workspace: File) {
        with(dockerAtomParam) {
            println("开始推送镜像")
            val fullImageName = getFullImageName()
            val commandStr = "docker push $fullImageName"
            println("[execute script]: $commandStr")
            CommonShellUtils.execute(commandStr, workspace)
            println("推送成功")
        }
    }

    fun dockerBuild(dockerAtomParam: DockerAtomParam, workspace: File) {
        with(dockerAtomParam) {
            val buildScript = StringBuilder()
            val fullImageName = getFullImageName()
            println(fullImageName)
            buildScript.append("docker build --network=host --pull -f $dockerFilePath -t $fullImageName $dockerBuildDir")
            dockerBuildArgs?.split("\n")?.filter { !it.isBlank() }?.forEach {
                buildScript.append(" --build-arg ").append(it.trim())
            }
            dockerBuildHosts?.split("\n")?.filter { !it.isBlank() }?.forEach {
                buildScript.append(" --add-host ").append(it.trim())
            }
            println("[execute script]: $buildScript")
            CommonShellUtils.execute(buildScript.toString(), workspace)
        }
    }

    private fun DockerAtomParam.getFullImageName() =
            if (targetImage.contains("hub.docker.com")) targetImage.removePrefix("/").removeSuffix("/").replaceBefore("/", "").substring(1) + ":" + targetImageTag else targetImage.removePrefix("/").removeSuffix("/") + ":" + targetImageTag
}