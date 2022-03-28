package com.tencent.bk.devops.atom.task.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.devops.atom.api.SdkEnv
import com.tencent.bk.devops.atom.pojo.Result
import com.tencent.bk.devops.atom.task.DockerAtomParam
import com.tencent.bk.devops.atom.task.DockerBuildParamNew
import com.tencent.bk.devops.atom.task.pojo.Status
import com.tencent.bk.devops.atom.utils.http.OkHttpUtils
import com.tencent.devops.common.api.util.JsonUtil

object DockerApi {

    private val dockerHostIp = System.getenv("docker_host_ip")
    private val dockerHostPort = System.getenv("docker_host_port")
    private var vmSeqId = SdkEnv.getVmSeqId()
    private val poolNo = System.getenv("pool_no")
    private val objectMapper = JsonUtil.getObjectMapper()
    private val gatewayUrl = System.getenv("devops_gateway")

    fun startDockerBuildAndPush(param: DockerAtomParam, dockerBuildParamNew: DockerBuildParamNew): String {
        with(param) {

            try {
                vmSeqId = if (poolNo.toInt() > 1) vmSeqId + "_$poolNo" else vmSeqId
                val url = "http://$dockerHostIp:$dockerHostPort/api/dockernew/build/$projectName/$pipelineId/$vmSeqId/$pipelineBuildId?elementId=${param.pipelineTaskId}"
                dockerBuildParamNew.poolNo = poolNo
                val request = objectMapper.writeValueAsString(dockerBuildParamNew)
                    val response = OkHttpUtils.doPost(url, request)
                println("start docker build and push url: dockernew/build/$projectName/$pipelineId/$vmSeqId/$pipelineBuildId")
                println("start docker build and push response: $response")
                return response
            } catch (e: Exception) {
                println("请求build出错" + e.message)
            }
            return ""
        }
    }

    fun getDockerBuildStatus(buildId: String): Result<Pair<Status, String>> {
        val url = "http://$dockerHostIp:$dockerHostPort/api/dockernew/build/$vmSeqId/$buildId"
        val responseBody = OkHttpUtils.doGet(url)
        return objectMapper.readValue(responseBody)
    }

}
