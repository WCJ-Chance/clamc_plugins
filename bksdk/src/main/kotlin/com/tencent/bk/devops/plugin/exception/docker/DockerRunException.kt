package com.tencent.bk.devops.plugin.exception.docker

data class DockerRunException(
    val errorMsg: String,
    val errorCode: Int = 2198002
) : RuntimeException(errorMsg)