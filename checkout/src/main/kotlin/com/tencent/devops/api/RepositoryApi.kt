package com.tencent.devops.api

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.bk.devops.atom.api.BaseApi
import com.tencent.bk.devops.atom.exception.AtomException
import com.tencent.bk.devops.atom.pojo.Result
import com.tencent.bk.devops.plugin.utils.JsonUtil
import com.tencent.devops.enums.RepositoryConfig
import com.tencent.devops.pojo.repository.pojo.Repository
import org.slf4j.LoggerFactory

object RepositoryApi : BaseApi() {

    fun get(repositoryConfig: RepositoryConfig): Repository {
        val path = "/ms/repository/api/build/repositories?repositoryId=${repositoryConfig.getURLEncodeRepositoryId()}" +
            "&repositoryType=${repositoryConfig.repositoryType.name}"
        return try {
            val request = buildGet(path)
            val response = request(request, "获取仓库信息失败")
            val result = JsonUtil.to(response, jacksonTypeRef<Result<Repository>>())
            result.data ?: throw AtomException("获取仓库信息失败: ${result.message}")
        } catch (e: Exception) {
            LoggerFactory.getLogger(RepositoryApi::class.java.name).error("获取仓库信息失败", e)
            throw AtomException(e.message)
        }
    }
}
