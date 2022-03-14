package com.tencent.bk.devops.atom.task.utils

import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class OauthCredentialLineParser : LineParser {
    // http://oauth2:7cfa3bb1659598dd478d149ca3b10f58@git.code.oa.com/rdeng/maven-hello-world.git ->
    // http://git.code.oa.com//rdeng/maven-hello-world.git
    override fun onParseLine(line: String): String {
        if (line.contains("http://oauth2:")) {
            val pattern = Pattern.compile("oauth2:(\\w+)@")
            val matcher = pattern.matcher(line)
            val replace = matcher.replaceAll("")
            logger.info("Parse the line from $line to $replace")
            return replace
        }
        return line
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OauthCredentialLineParser::class.java)
    }
}

fun main(argv: Array<String>) {
    val pattern = Pattern.compile("oauth2:(\\w+)@")
    val matcher = pattern.matcher("http://oauth2:7cfa3bb1659598dd478d149ca3b10f58@git.code.oa.com/rdeng/maven-hello-world.git  http://oauth2:7cfa3bb1659598dd478d149ca3b10f58@git.code.oa.com/rdeng/maven-hello-world.git")
    println(matcher.replaceAll(""))
}
