package com.tencent.bk.devops.atom.task.logger.meta

enum class AnsiColor(val value: Int) {
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7),
    DEFAULT(9);

    fun fg(): Int {
        return value + 30
    }

    fun bg(): Int {
        return value + 40
    }

    fun fgBright(): Int {
        return value + 90
    }

    fun bgBright(): Int {
        return value + 100
    }
}
