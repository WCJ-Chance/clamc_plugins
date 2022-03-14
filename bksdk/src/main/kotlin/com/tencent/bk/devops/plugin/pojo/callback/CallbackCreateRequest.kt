package com.tencent.bk.devops.plugin.pojo.callback

data class CallbackCreateRequest(
    val name: String,
    val uniqueKey: String,
    val taskId: String
)
