package com.tencent.bk.devops.plugin.api.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.devops.atom.api.BaseApi
import com.tencent.bk.devops.atom.pojo.Result
import com.tencent.bk.devops.atom.utils.json.JsonUtil
import com.tencent.bk.devops.plugin.pojo.callback.CallbackCreateRequest
import com.tencent.bk.devops.plugin.pojo.callback.CallbackData
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class CallBackApi : BaseApi() {

    private val mapper = jacksonObjectMapper()

    fun createCallback(name: String, uniqueKey: String, taskId: String): Boolean {
        val requestBody = CallbackCreateRequest(name, uniqueKey, taskId)
        val path = "/process/api/build/callback/task/buildCreateCallback"
        val request = buildPost(
            path,
            RequestBody.create(JSON_CONTENT_TYPE, JsonUtil.toJson(requestBody)),
            hashMapOf()
        )
        var responseContent: String? = null
        try {
            responseContent = super.request(request, "创建回调失败")
        } catch (e: Exception) {
            logger.error("create callback throw Exception", e)
        }
        return if (null != responseContent) {
            mapper.readValue<Result<Boolean>>(
                responseContent
            ).data ?: false
        } else false
    }

    fun claimCallbackRequestBody(
        uniqueKey: String,
        taskId: String
    ): CallbackData {
        val param = "uniqueKey=${
            URLEncoder.encode(
                uniqueKey,
                "UTF-8"
            )
        }&taskId=${URLEncoder.encode(taskId, "UTF-8")}"
        val path = "/process/api/build/callback/task/buildClaimCallback?$param"
        val request = buildGet(path)
        var responseContent: String? = null
        try {
            responseContent = super.request(request, "查询回调状态失败")
        } catch (e: Exception) {
            logger.error("create callback throw Exception", e)
        }
        return if (null != responseContent) {
            mapper.readValue<Result<CallbackData>>(
                responseContent
            ).data ?: CallbackData()
        } else CallbackData()
    }

    private val logger = LoggerFactory.getLogger(this::class.qualifiedName)
}
