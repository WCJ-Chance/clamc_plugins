package com.tencent.devops.repository.pojo

import com.tencent.devops.enums.RepoAuthType
import com.tencent.devops.pojo.repository.pojo.Repository

class CodeGiteeRepository(
    override val aliasName: String,
    override val url: String,
    override val credentialId: String,
    override val projectName: String,
    override var userName: String,
    override val projectId: String?,
    override val repoHashId: String?,
    val authType: RepoAuthType? = RepoAuthType.HTTPS
) : Repository {
    companion object {
        const val classType = "gitee"
    }
}
