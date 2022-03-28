package com.tencent.bk.devops.atom.task

import com.tencent.bk.devops.atom.AtomContext
import com.tencent.bk.devops.atom.pojo.StringData
import com.tencent.bk.devops.atom.spi.AtomService
import com.tencent.bk.devops.atom.spi.TaskAtom
import com.tencent.bk.devops.atom.task.api.DockerApi
import com.tencent.bk.devops.atom.task.pojo.Status
import com.tencent.bk.devops.atom.task.utils.DockerUtils.dockerBuild
import com.tencent.bk.devops.atom.task.utils.DockerUtils.dockerLogin
import com.tencent.bk.devops.atom.task.utils.DockerUtils.dockerLogout
import com.tencent.bk.devops.atom.task.utils.DockerUtils.dockerPush
import com.tencent.bk.devops.atom.task.utils.TicketUtils.getTicketUserAndPass
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.min

@AtomService(paramClass = DockerAtomParam::class)
class DockerTaskAtom : TaskAtom<DockerAtomParam> {

    private val logger = LoggerFactory.getLogger(DockerTaskAtom::class.java)

    private val kubernetesRunMode = System.getenv("kubernetes_run_mode") != null

    override fun execute(atomContext: AtomContext<DockerAtomParam>) {
        val property = System.getenv("devops_slave_model")

        logger.info("构建类型为$property")
        if ("docker" == property && !kubernetesRunMode) {
            executeCommon(atomContext)
        } else {
            executeThird(atomContext)
        }
        val result = atomContext.result
        result.data["BK_DOCKER_TARGE_IMAGE_NAME"] = StringData(atomContext.param.targetImage)
        result.data["BK_DOCKER_TARGE_IMAGE_TAG"] = StringData(atomContext.param.targetImageTag)

        result.data["repository"] = StringData(atomContext.param.targetImage)
        result.data["imageTag"] = StringData(atomContext.param.targetImageTag)
    }

    private fun executeCommon(atomContext: AtomContext<DockerAtomParam>) {
        with(atomContext.param) {
            val dockerBuildParamNew = getHttpDockerBuildParam(atomContext.param)
            DockerApi.startDockerBuildAndPush(atomContext.param, dockerBuildParamNew)

            var timeGap = 2000L
            while (true) {
                Thread.sleep(timeGap)
                timeGap = min(timeGap + 2000L, 60 * 1000)
                val status = DockerApi.getDockerBuildStatus(pipelineBuildId).data
                when (status?.first) {
                    Status.RUNNING -> {
                    }
                    Status.FAILURE -> {
                        throw RuntimeException("get docker build status fail: ${status?.second}")
                    }
                    Status.NO_EXISTS -> {
                        throw RuntimeException("get docker build status fail: docker image does not exist: $status")
                    }
                    Status.SUCCESS -> {
                        println("docker build image success!: $status")
                        return
                    }
                    else -> {
                        throw RuntimeException("get docker build status fail for unknown reason: $status")
                    }
                }
            }
        }

    }

    private fun executeThird(atomContext: AtomContext<DockerAtomParam>) {
        logger.info("使用命令行")
        with(atomContext.param) {
            val workspace = File(atomContext.param.bkWorkspace)

            val loginIps = mutableSetOf<String>()
            try {
                loginIps.addAll(doSourceLogin(this, workspace))

                logger.info("loginIp is : $loginIps")
                dockerBuild(this, workspace)
            } finally {
                loginIps.forEach {
                    dockerLogout(it, workspace)
                }
            }

            loginIps.clear()
            try {
                loginIps.addAll(doTargetLogin(this, workspace))

                dockerPush(this, workspace)
            } finally {
                loginIps.forEach {
                    dockerLogout(it, workspace)
                }
            }

        }
    }

    private fun doSourceLogin(dockerAtomParam: DockerAtomParam, workspace: File): Set<String> {
        logger.info("doSourceLogin。。。")
        val loginIps = mutableSetOf<String>()
        with(dockerAtomParam) {
            sourceMirrorTicketPair?.split("\n")
                    ?.filter { !it.isBlank() }
                    ?.map { it.split(Regex("\\s+")) }
                    ?.forEach {
                        val loginIp = it[0]
                        val ticketId = it.getOrNull(1)?.trim()
                        if (!ticketId.isNullOrBlank()) {
                            logger.info("登录docker")
                            dockerLogin(loginIp, ticketId, workspace)
                            logger.info("登录docker成功")
                            loginIps.add(loginIp)
                        }
                    }

            return loginIps
        }
    }

    private fun doTargetLogin(dockerAtomParam: DockerAtomParam, workspace: File): Set<String> {
        val loginIps = mutableSetOf<String>()
        with(dockerAtomParam) {
            // solve target ticket id
            if (!targetTicketId.isNullOrBlank()) {
                val loginIp = targetImage.removePrefix("/").removeSuffix("/").split("/").first()
                println("login for target host: $loginIp")
                dockerLogin(loginIp, targetTicketId, workspace)
                println("login successfully for host: $loginIp")
                loginIps.add(loginIp)
            }
            return loginIps
        }
    }

    private fun getHttpDockerBuildParam(param: DockerAtomParam): DockerBuildParamNew {
        with(param) {
            // get source tickect list
            val ticketList = sourceMirrorTicketPair?.split("\n")
                    ?.filter { !it.isBlank() }
                    ?.map { it.split(Regex("\\s+")) }
                    ?.map {
                        val loginIp = it[0]
                        val ticketId = it[1].trim()
                        val ticketPair = getTicketUserAndPass(ticketId)
                        val username = ticketPair.first
                        val password = ticketPair.second

                        return@map Triple(loginIp, username, password)
                    } ?: listOf()

            // get target ticket info
            val targetTicket = getTicketUserAndPass(targetTicketId)

            // get build args
            val paramArgs = dockerBuildArgs?.split("\n")?.filter { !it.isBlank() } ?: listOf()

            // get build hosts
            val paramHosts = dockerBuildHosts?.split("\n")?.filter { !it.isBlank() } ?: listOf()

            // get final param
            val targetImageRepo = targetImage.split("/").first()
            val targetImageName = targetImage.removePrefix(targetImageRepo).removeSuffix("/")
            val dockerBuildParamNew = DockerBuildParamNew(
                    ticket = ticketList,
                    imageName = targetImageName.removePrefix("/").removeSuffix("/"),
                    imageTag = targetImageTag,
                    buildDir = dockerBuildDir,
                    dockerFile = dockerFilePath,
                    repoAddr = targetImageRepo,
                    userName = targetTicket.first,
                    password = targetTicket.second,
                    args = paramArgs,
                    host = paramHosts,
                    poolNo = System.getenv("pool_no")
            )
            println("docker build http param is: $dockerBuildParamNew")
            return dockerBuildParamNew
        }
    }

}
