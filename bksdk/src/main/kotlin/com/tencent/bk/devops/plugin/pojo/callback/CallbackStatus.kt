package com.tencent.bk.devops.plugin.pojo.callback

enum class CallbackStatus {
    SUCCESS,
    WAITING;

    companion object {
        private val map = values().associateBy(CallbackStatus::name)
        fun parse(name: String) = map[name] ?: WAITING
    }
}