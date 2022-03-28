package com.tencent.bk.devops.atom.task


//("DockerBuild")
data class DockerBuildParamNew(
        //("基础镜像凭证", required = true)
        val ticket: List<Triple<String,String,String>>,
        //("镜像名称", required = true)
        val imageName: String,
        //("镜像TAG", required = true)
        val imageTag: String,
        //("构建目录", required = false)
        val buildDir: String? = ".",
        //("Dockerfile", required = false)
        val dockerFile: String? = "Dockerfile",
        //("repoAddr", required = true)
        val repoAddr: String,
        //("userName", required = true)
        val userName: String,
        //("password", required = true)
        val password: String,
        //("构建的参数", required = true)
        val args: List<String>,
        //("host配置", required = true)
        val host: List<String>,
        // 并发数
        var poolNo: String

) {
    override fun toString(): String {
        return "ticketList: ${ticket.map { Triple(it.first, it.second, "***") }}\n" +
                "imageName: $imageName, buildDir: $buildDir, dockerFile: $dockerFile, " +
                "repoAddr: $repoAddr, userName: $userName, password: ***, args: $args, host: $host, poolNo: $poolNo"
    }
}