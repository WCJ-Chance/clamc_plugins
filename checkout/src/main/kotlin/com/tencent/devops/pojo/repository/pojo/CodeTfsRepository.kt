package com.tencent.devops.repository.pojo

import com.tencent.devops.pojo.repository.pojo.Repository

/**
 * Team Foundation Server 版本控制
 * @see <a href="https://docs.microsoft.com/zh-cn/visualstudio/mac/tf-version-control">TFS</a>
 */
data class CodeTfsRepository(
    override val aliasName: String,
    override val url: String,
    override val credentialId: String,
    override val projectName: String,
    override var userName: String,
    override val projectId: String?,
    override val repoHashId: String?
) : Repository {
    companion object {
        const val classType = "tfs-tfvc"
    }
}
