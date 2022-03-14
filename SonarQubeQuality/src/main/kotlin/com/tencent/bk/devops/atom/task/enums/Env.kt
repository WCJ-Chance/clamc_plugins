package com.tencent.bk.devops.atom.task.enums

import org.slf4j.LoggerFactory

enum class Env {
    PROD,
    TEST,
    DEV,
    DEFAULT;

    companion object {
        fun parse(env: String): Env {
            return when {
                env.equals(PROD.name, true) -> PROD
                env.equals(TEST.name, true) -> TEST
                env.equals(DEV.name, true) -> DEV
                env.equals(DEFAULT.name, true) -> DEFAULT
                else -> {
                    logger.warn("Unknown the env $env, use prod as default")
                    PROD
                }
            }
        }
        private val logger = LoggerFactory.getLogger(Env::class.java)
    }
}
