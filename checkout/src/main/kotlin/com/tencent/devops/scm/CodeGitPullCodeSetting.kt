package com.tencent.devops.scm

import com.tencent.bk.devops.atom.exception.AtomException
import com.tencent.devops.api.RepositoryApi
import com.tencent.devops.enums.AuthType
import com.tencent.devops.enums.CodePullStrategy
import com.tencent.devops.enums.GitPullModeType
import com.tencent.devops.enums.RepositoryConfig
import com.tencent.devops.enums.RepositoryType
import com.tencent.devops.enums.ticket.CredentialType
import com.tencent.devops.pojo.FROM
import com.tencent.devops.pojo.GitCodeAtomParam
import com.tencent.devops.utils.SSHAgentUtils
import org.slf4j.LoggerFactory

open class CodeGitPullCodeSetting(
    override val params: GitCodeAtomParam
) : IPullCodeSetting {

    override fun pullCode(): Map<String, String>? {
        if (FROM.DEVOPS == params.from) {
            if (params.repositoryHashId.isNullOrBlank()) {
                throw AtomException("repositoryHashId is null")
            }
            val repository = RepositoryApi.get(RepositoryConfig(params.repositoryHashId, null, RepositoryType.ID))
            params.ticketId = repository.credentialId
            params.repositoryUrl = repository.url
        }
        return when (params.authType) {
            AuthType.ACCESSTOKEN -> {
                doOauthPullCode(listOf(params.accessToken!!))
            }
            AuthType.TICKET -> {
                val credentialResult = getCredential(params.ticketId)

                when (credentialResult?.second) {
                    CredentialType.ACCESSTOKEN -> doOauthPullCode(credentialResult.first)
                    CredentialType.USERNAME_PASSWORD, CredentialType.USERNAME_ACCESSTOKEN -> {
                        doHttpPullCode(credentialResult.first)
                    }
                    CredentialType.SSH_PRIVATEKEY -> doPullCodeSSH(credentialResult.first)
                    else -> doPullCode()
                }
            }
            AuthType.USERNAME_PASSWORD -> {
                doHttpPullCode(listOf(params.username!!, params.password!!))
            }
        }
    }

    private fun doPullCodeSSH(credentials: List<String>): Map<String, String>? {
        if (credentials.size > 2) {
            logger.warn("The git credential($credentials) is illegal")
            throw RuntimeException("The git credential is illegal")
        }

        val privateKey = credentials[0]
        val passPhrase = if (credentials.size == 2) {
            val c = credentials[1]
            if (c.isEmpty()) {
                null
            } else {
                c
            }
        } else {
            null
        }
        SSHAgentUtils(privateKey, passPhrase).addIdentity()
        val credentialSetter = CodeGitSshCredentialSetter(privateKey, passPhrase)
        return performTask(credentialSetter)
    }

    private fun doOauthPullCode(credentials: List<String>): Map<String, String>? {
        val token = credentials.firstOrNull() ?: throw RuntimeException("cannot found oauth access token")
        val credentialSetter = CodeGitOauthCredentialSetter(token)
        return performTask(credentialSetter)
    }


    /**
     * chance 20220325
     */
    private fun doOauthPullCode(accessToken: String): Map<String, String>? {
        val credentialSetter = CodeGitOauthCredentialSetter(accessToken)
        return performTask(credentialSetter)
    }

    private fun doHttpPullCode(credentials: List<String>): Map<String, String>? {
        if (credentials.size != 2) {
            logger.warn("The git credential($credentials) is illegal")
            throw RuntimeException("git???????????????")
        }

        val username = credentials[0]
        val password = credentials[1]
        if (username.isEmpty() || password.isEmpty()) {
            logger.warn("The git credential username($username) or password($password) is empty")
            throw RuntimeException("git???????????????")
        }

        val credentialSetter = CodeGitUsernameCredentialSetter(username, password)
        return performTask(credentialSetter)
    }

    private fun doPullCode(): Map<String, String>? {
        return performTask(CodeGitBlankCredentialSetter())
    }

    private fun performTask(setter: GitCredentialSetter): MutableMap<String, String> {
        val task = when (params.strategy) {
            CodePullStrategy.FRESH_CHECKOUT -> FreshCheckoutTask(
                params,
                setter
            )
            CodePullStrategy.REVERT_UPDATE -> RevertCheckoutTask(
                params,
                setter
            )
            CodePullStrategy.INCREMENT_UPDATE -> GitUpdateTask(
                params,
                setter
            )
        }
        return performTask(task)
    }

    override fun getRemoteBranch(): String {
        with(params) {
            if (pullType == GitPullModeType.BRANCH) {
                return refName
            }
            return ""
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitPullCodeSetting::class.java)
    }
}
