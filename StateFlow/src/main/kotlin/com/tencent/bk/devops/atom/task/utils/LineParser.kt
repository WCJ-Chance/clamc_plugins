package com.tencent.bk.devops.atom.task.utils

interface LineParser {
    fun onParseLine(line: String): String
}
