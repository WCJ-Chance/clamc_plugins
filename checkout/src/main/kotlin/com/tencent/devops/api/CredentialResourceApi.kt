package com.tencent.devops.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.api.BaseApi
import com.tencent.bk.devops.atom.pojo.Result
import com.tencent.bk.devops.atom.utils.json.JsonUtil
import com.tencent.devops.pojo.utils.CredentialInfo

class CredentialResourceApi : BaseApi() {
    fun get(credentialId: String, publicKey: String): Result<CredentialInfo> {
        val path = "/ticket/api/build/credentials/$credentialId?publicKey=${encode(publicKey)}"
        val request = buildGet(path)
        val responseContent = request(request, "获取凭据失败")
        return JsonUtil.fromJson(responseContent, object : TypeReference<Result<CredentialInfo>>() {})
    }

    fun get(credentialId: String): Result<CredentialInfo> {
        val path = "/ticket/api/build/credentials/$credentialId/detail"
        val request = buildGet(path)
        val responseContent = request(request, "获取凭据失败")
        return JsonUtil.fromJson(responseContent, object : TypeReference<Result<CredentialInfo>>() {})
    }
}
