package com.tencent.bk.devops.plugin.api.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.api.BaseApi
import com.tencent.bk.devops.atom.pojo.Result
import com.tencent.bk.devops.atom.utils.json.JsonUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.slf4j.LoggerFactory
import java.io.File

class PluginDataFileApi : BaseApi() {

    companion object {
        private val logger = LoggerFactory.getLogger(PluginDataFileApi::class.java)
        private val MultipartFormData = "multipart/form-data".toMediaTypeOrNull()
    }

    /**
     * 文件上传
     * @param userId 用户id
     * @param projectCode 项目code
     * @param pipelineId 流水线id
     * @param buildId 构建id
     * @param taskId 插件id
     * @param file 上传文件
     */
    fun uploadFile(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        file: File
    ): Result<Boolean> {
        logger.info("upload file ..........")
        val path = "/ms/process/api/service/pipelines/cw/file_upload/$userId/$projectCode/$pipelineId/$buildId/$taskId"
        val fileBody = file.asRequestBody(MultipartFormData)
        val fileName = file.name
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .build()
        val request = buildPost(path, requestBody, mutableMapOf())
        logger.info("request ${request.url}")
        val responseContent = request(request, "upload file:$fileName fail")
        logger.info("responseContent $responseContent")
        return JsonUtil.fromJson(responseContent, object : TypeReference<Result<Boolean>>() {})
    }
}
