package com.tencent.bk.devops.atom.task.utils

import com.tencent.bk.devops.atom.task.enums.OSType
import com.tencent.bk.devops.atom.task.env.AgentEnv.getOS
import com.tencent.bk.devops.atom.task.logger.LoggerService
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory
import java.io.File

object CommandLineUtils {

    private val logger = LoggerFactory.getLogger(CommandLineUtils::class.java)

    private val lineParser = listOf(OauthCredentialLineParser())

    @JvmStatic fun execute(command: String, workspace: File?, print2Logger: Boolean, prefix: String = ""): String {

        val result = StringBuilder()

        val cmdLine = CommandLine.parse(command)
        val executor = CommandLineExecutor()
        if (workspace != null) {
            executor.workingDirectory = workspace
        }

        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null)
                    return

                var tmpLine: String = prefix + line

                lineParser.forEach {
                    tmpLine = it.onParseLine(tmpLine)
                }
                if (print2Logger) {
                    LoggerService.addNormalLine(tmpLine)
                } else {
                    result.append(tmpLine).append("\n")
                }
            }
        }

        val errorStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }

                var tmpLine: String = prefix + line

                lineParser.forEach {
                    tmpLine = it.onParseLine(tmpLine)
                }
                if (print2Logger) {
                    LoggerService.addRedLine(tmpLine)
                } else {
                    result.append(tmpLine).append("\n")
                }
            }
        }
        executor.streamHandler = PumpStreamHandler(outputStream, errorStream)
        try {
            val exitCode = executor.execute(cmdLine)
            if (exitCode != 0) {
                throw RuntimeException("$prefix Script command execution failed with exit code($exitCode)")
            }
        } catch (t: Throwable) {
            logger.warn("Fail to execute the command($command)", t)
            if (print2Logger) {
                LoggerService.addRedLine("$prefix Fail to execute the command($command)")
            }
            throw t
        }
        return result.toString()
    }

    fun execute(file: File, workspace: File?, print2Logger: Boolean, prefix: String = ""): String {
        if (!file.exists()) {
            logger.warn("The file(${file.absolutePath}) is not exist")
            throw RuntimeException("The file(${file.absolutePath}) is not exist")
        }
        val command = if (getOS() == OSType.WINDOWS) {
            file.name
        } else {
            execute("chmod +x ${file.name}", workspace, false)
            "./${file.name}"
        }
        logger.info("Executing command($command) in workspace($workspace)")
        return execute(command, workspace, print2Logger, prefix)
    }
}
