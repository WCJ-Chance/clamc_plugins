package com.tencent.bk.devops.plugin.pojo.callback

data class CallbackData(
    val uniqueKey: String? = null,
    val data: String? = null,
    val status: CallbackStatus = CallbackStatus.WAITING
)
