package com.tencent.devops.utils.shell

import com.tencent.devops.api.CredentialResourceApi
import com.tencent.devops.enums.ticket.CredentialType
import com.tencent.devops.exception.ClientException
import com.tencent.devops.utils.DHUtil
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * This util is to get the credential from core
 * It use DH encrypt and decrypt
 */
object CredentialUtils {

    fun getCredentialWithType(credentialId: String, showErrorLog: Boolean = true): Pair<List<String>, CredentialType> {
        if (credentialId.trim().isEmpty()) {
            throw RuntimeException("The credential Id is empty")
        }
        try {
            logger.info("Start to get the credential($credentialId)")
            val result = CredentialResourceApi().get(credentialId)

            if (result.status != 0 || result.data == null) {
                logger.error("Fail to get the credential($credentialId) because of ${result.message}")
                throw ClientException(result.message!!)
            }

            val credential = result.data!!
            val list = ArrayList<String>()

            list.add(credential.v1)
            if (!credential.v2.isNullOrEmpty()) {
                list.add(credential.v2)
                if (!credential.v3.isNullOrEmpty()) {
                    list.add(credential.v3)
                    if (!credential.v4.isNullOrEmpty()) {
                        list.add(credential.v4)
                    }
                }
            }
            return Pair(list, credential.credentialType)
        } catch (e: Exception) {
            logger.warn("Fail to get the credential($credentialId)", e)
            if (showErrorLog) {
                logger.error("获取凭证（$credentialId）失败， 原因：${e.message}")
            }
            throw e
        }
    }

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }

    private val logger = LoggerFactory.getLogger(CredentialUtils::class.java)
}
