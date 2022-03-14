package com.tencent.bk.devops.atom.task.api.log

//import com.tencent.bk.devops.atom.task.api.AbstractBuildResourceApi
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.devops.atom.api.BaseApi
import com.tencent.bk.devops.atom.pojo.Result
import com.tencent.bk.devops.atom.task.model.LogMessage
import com.tencent.bk.devops.atom.task.utils.JsonUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class LogResourceApi : BaseApi() {

    fun addLogMultiLine(logMessages: List<LogMessage>): Result<Boolean> {
        val path = "/log/api/build/logs/multi"
        val requestBody = JsonUtil.getObjectMapper()
            .writeValueAsString(logMessages).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = buildPost(path, requestBody, emptyMap())
        val responseContent = request(request, "上报日志失败")
        return JsonUtil.getObjectMapper().readValue(responseContent)
    }
}
