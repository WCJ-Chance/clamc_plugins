package com.tencent.bk.devops.plugin.docker.exception

data class DockerPullException(
    val errorMsg: String,
    val errorCode: Int = 2198001
) : RuntimeException(errorMsg)
