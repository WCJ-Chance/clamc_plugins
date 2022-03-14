package com.tencent.bk.devops.atom.task.model

import com.tencent.bk.devops.atom.task.enums.LogType

data class LogMessage(
        val message: String,
        val timestamp: Long,
        val tag: String = "",
        val logType: LogType = LogType.LOG,
        val executeCount: Int? = null
) {
    override fun toString(): String {
        return "LogMessage(tag='$tag', message='$message', timestamp=$timestamp), logType=$logType, executeCount=$executeCount)"
    }
}
