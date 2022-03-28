package com.tencent.devops.scm

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.slf4j.LoggerFactory

class CodeGitUsernameCredentialSetter constructor(
    private val username: String,
    private val password: String
) : GitCredentialSetter {
    override fun setGitCredential() {
    }

    override fun getCredentialUrl(url: String): String {
        try {
            val u = if (url.startsWith("http")) url.toHttpUrlOrNull()
            else "http://$url".toHttpUrlOrNull()
            if (u == null) {
                logger.warn("url is invalid: $url")
                return url
            }
            return u.newBuilder().username(username).password(password).build().toString()
        } catch (t: Throwable) {
            logger.warn("Fail to get the username and password credential url for $url", t)
        }
        return url
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitUsernameCredentialSetter::class.java)
    }
}
