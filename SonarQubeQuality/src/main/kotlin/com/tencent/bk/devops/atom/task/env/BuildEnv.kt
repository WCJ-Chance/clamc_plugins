package com.tencent.bk.devops.atom.task.env

import com.tencent.bk.devops.atom.task.BUILD_TYPE

object BuildEnv {
    private var buildType: String? = null

    fun getBuildType(): BuildType {
        if (buildType == null) {
            synchronized(this) {
                if (buildType == null) {
                    buildType = System.getProperty(BUILD_TYPE)
                }
            }
        }
        if (buildType == null || !BuildType.contains(buildType!!)) {
            return BuildType.DOCKER
        }
        return BuildType.valueOf(buildType!!)
    }

    fun isThirdParty() = getBuildType() == BuildType.AGENT
}

fun main(args: Array<String>) {
    println(BuildEnv.getBuildType())
}
