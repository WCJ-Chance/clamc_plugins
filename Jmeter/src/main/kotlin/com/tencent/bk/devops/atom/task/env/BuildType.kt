package com.tencent.bk.devops.atom.task.env

enum class BuildType {
    WORKER,
    AGENT,
    PLUGIN_AGENT,
    DOCKER,
    DOCKER_HOST,
    TSTACK_AGENT;

    companion object {
        fun contains(env: String): Boolean {
            BuildType.values().forEach {
                if (it.name == env) {
                    return true
                }
            }
            return false
        }
    }
}
