package com.tencent.devops.scm

import org.slf4j.LoggerFactory
import java.net.URL

class CodeGitOauthCredentialSetter constructor(private val token: String) : GitCredentialSetter {
    override fun setGitCredential() {
    }

    override fun getCredentialUrl(url: String): String {
        try {
            val u = URL(url)
            val host = if (u.host.endsWith("/")) {
                u.host.removeSuffix("/")
            } else {
                u.host
            }
            val path = if (u.path.startsWith("/")) {
                u.path.removePrefix("/")
            } else {
                u.path
            }
            val port = u.port
            if (port == -1) {
                return "${u.protocol}://oauth2:$token@$host/$path"
            }
            return "${u.protocol}://oauth2:$token@$host:$port/$path"
        } catch (t: Throwable) {
            logger.warn("Fail to get the oauth credential url for $url", t)
        }
        return url
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitOauthCredentialSetter::class.java)
    }
}
