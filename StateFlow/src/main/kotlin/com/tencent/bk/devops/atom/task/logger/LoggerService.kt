package com.tencent.bk.devops.atom.task.logger

import com.tencent.bk.devops.atom.task.api.log.LogResourceApi
import com.tencent.bk.devops.atom.task.enums.LogType
import com.tencent.bk.devops.atom.task.model.LogMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

object LoggerService {

    private val logResourceApi = LogResourceApi()

    private val executorService = Executors.newSingleThreadExecutor()
    private val flushExecutor = Executors.newSingleThreadExecutor()
    private val queue = LinkedBlockingQueue<LogMessage>(2000)
    private val logger = LoggerFactory.getLogger(LoggerService::class.java)
    private val running = AtomicBoolean(true)
    private var future: Future<Boolean>? = null
    // 当前执行的原子id
    var elementId = ""
    var executeCount = 1

    private val lock = ReentrantLock()

    private val logMessages = ArrayList<LogMessage>()

    private val loggerThread = Callable<Boolean> {
        try {
            var lastSaveTime: Long = 0
            while (running.get()) {
                val logMessage = try {
                    queue.poll(3, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    logger.warn("Logger service poll thread interrupted", e)
                    null
                }
                lock.lock()
                try {
                    if (logMessage != null) {
                        logMessages.add(logMessage)
                    }
                } finally {
                    lock.unlock()
                }

                val size = logMessages.size
                val now = System.currentTimeMillis()
                // 缓冲大于200条或上次保存时间超过3秒
                if (size >= 200 || (size > 0 && (now - lastSaveTime > 3 * 1000))) {
                    flush()
                    lastSaveTime = now
                }
            }
            if (!logMessages.isEmpty()) {
                flush()
            }
        } catch (t: Throwable) {
            logger.warn("Fail to send the logger", t)
        }
        logger.info("Finish the sending thread - (${queue.size})")
        true
    }

    private class FlushThread : Callable<Int> {
        override fun call(): Int {
            logger.info("Start to flush the logger")
            lock.lock()
            val size = logMessages.size
            try {
                if (size > 0) {
                    sendMultiLog(logMessages)
                    logMessages.clear()
                }
            } finally {
                logger.info("Finish flush the log - $size")
                lock.unlock()
            }
            return size
        }
    }

    fun start() {
        logger.info("Start the log service")
        future = executorService.submit(loggerThread)
    }

    fun flush(): Int {
        logger.info("Start to flush the log service")
        val future = flushExecutor.submit(FlushThread())
        return future.get()
    }

    fun stop() {
        try {
            logger.info("Start to stop the log service")
            if (this.running.get()) {
                this.running.set(false)
                if (future != null) {
                    future!!.get()
                }
                // 把没完成的日志打完
                while (queue.size != 0) {
                    queue.drainTo(logMessages)
                    if (!logMessages.isEmpty()) {
                        flush()
                    }
                }
            }
            logger.info("Finish stopping the log service")
        } catch (e: Exception) {
            logger.error("Fail to stop log service for build", e)
        }
    }

    @JvmStatic fun addNormalLine(message: String) {
        val logMessage = LogMessage(
            message,
            System.currentTimeMillis(),
            elementId,
            LogType.LOG,
            executeCount
        )
        logger.info(logMessage.toString())
        try {
            this.queue.put(logMessage)
        } catch (e: InterruptedException) {
            logger.error("写入普通日志行失败：", e)
        }
    }

    fun addYellowLine(message: String) =
        addNormalLine(Ansi().fgYellow().a(message).reset().toString())

    fun addRedLine(message: String) =
        addNormalLine(Ansi().fgRed().a(message).reset().toString())

    fun addFoldStartLine(tagName: String) {
        val logMessage = LogMessage(
            "soda_fold:start:$tagName",
            System.currentTimeMillis(),
            elementId,
            LogType.START,
            executeCount
        )
        addLog(logMessage)
    }

    fun addFoldEndLine(tagName: String) {
        val logMessage = LogMessage(
            "soda_fold:end:$tagName",
            System.currentTimeMillis(),
            elementId,
            LogType.END,
            executeCount
        )
        addLog(logMessage)
    }

    private fun addLog(message: LogMessage) = queue.put(message)

    private fun sendMultiLog(logMessages: List<LogMessage>) {
        try {
            logger.info("Start to send the log - ${logMessages.size}")
            val result = logResourceApi.addLogMultiLine(logMessages)
            if (result.status != 0) {
                logger.error("发送构建日志失败：${result.message}")
            }
        } catch (e: Exception) {
            logger.warn("Fail to send the logs(${logMessages.size})", e)
        }
    }
}
