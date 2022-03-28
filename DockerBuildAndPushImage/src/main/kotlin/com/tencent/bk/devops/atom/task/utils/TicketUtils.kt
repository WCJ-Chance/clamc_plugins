package com.tencent.bk.devops.atom.task.utils

import com.tencent.bk.devops.atom.task.api.CredentialApi
import org.slf4j.LoggerFactory
import java.util.*

object TicketUtils {

    private val credentialApi = CredentialApi()

//    fun getTicketUserAndPass(ticketId: String?): Pair<String, String> {
//        if (ticketId.isNullOrBlank()) {
//            println("ticket id is null or blank: $ticketId")
//            return Pair("", "")
//        }
//        val ticket: Map<String, String>?
//        try {
//            val pair = DHUtil.initKey()
//            val encoder = Base64.getEncoder()
//            val decoder = Base64.getDecoder()
//            ticket = credentialApi.getCredential(ticketId, encoder.encodeToString(pair.publicKey)).data
//            val username = String(DHUtil.decrypt(
//                    decoder.decode(ticket["v1"]),
//                    decoder.decode(ticket["publicKey"]),
//                    pair.privateKey))
//            val password = String(DHUtil.decrypt(
//                    decoder.decode(ticket["v2"]),
//                    decoder.decode(ticket["publicKey"]),
//                    pair.privateKey))
//            if (ticket.isNullOrEmpty()) throw RuntimeException("the ticketId is error, please check your input......")
//            if (username.isNullOrEmpty()) throw RuntimeException("the username is error, please check your input......")
//            if (password.isNullOrEmpty()) throw RuntimeException("the password is error, please check your input......")
//            return Pair(username, password)
//        } catch (e: Exception) {
//            throw RuntimeException("获取凭证 $ticketId 失败", e)
//        }
//    }

    fun getTicketUserAndPass(ticketId: String?): Pair<String, String> {
        if (ticketId.isNullOrBlank()) {
            println("ticket id is null or blank: $ticketId")
            return Pair("", "")
        }
        val ticket: Map<String, String>?
        ticket = credentialApi.getCredential(ticketId).data
        val username = ticket.get("username").toString()
        val password = ticket.get("password").toString()
        return Pair(username, password)
    }
}