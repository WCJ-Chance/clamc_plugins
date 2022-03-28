package com.tencent.devops.pojo.repository.pojo

import com.tencent.devops.enums.RepoAuthType

data class CodeBitbucketRepository(
    override val aliasName: String,
    override val url: String,
    override val credentialId: String,
    override val projectName: String,
    override var userName: String,
    val authType: RepoAuthType? = RepoAuthType.SSH,
    override val projectId: String?,
    override val repoHashId: String?
) : Repository {
    companion object {
        const val classType = "bitbucket"
    }
}
