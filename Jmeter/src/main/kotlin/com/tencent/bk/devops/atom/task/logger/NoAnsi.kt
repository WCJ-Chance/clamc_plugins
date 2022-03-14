package com.tencent.bk.devops.atom.task.logger

import com.tencent.bk.devops.atom.task.logger.meta.AnsiAttribute
import com.tencent.bk.devops.atom.task.logger.meta.AnsiColor
import com.tencent.bk.devops.atom.task.logger.meta.AnsiErase

class NoAnsi : Ansi {
    constructor(builder: StringBuilder) : super(builder)
    constructor(size: Int) : super(size)
    constructor() : super()

    override fun fg(color: AnsiColor): Ansi {
        return this
    }

    override fun bg(color: AnsiColor): Ansi {
        return this
    }

    override fun fgBright(color: AnsiColor): Ansi {
        return this
    }

    override fun bgBright(color: AnsiColor): Ansi {
        return this
    }

    override fun a(attribute: AnsiAttribute): Ansi {
        return this
    }

    override fun cursor(x: Int, y: Int): Ansi {
        return this
    }

    override fun cursorToColumn(x: Int): Ansi {
        return this
    }

    override fun cursorUp(y: Int): Ansi {
        return this
    }

    override fun cursorRight(x: Int): Ansi {
        return this
    }

    override fun cursorDown(y: Int): Ansi {
        return this
    }

    override fun cursorLeft(x: Int): Ansi {
        return this
    }

    override fun cursorDownLine(): Ansi {
        return this
    }

    override fun cursorDownLine(n: Int): Ansi {
        return this
    }

    override fun cursorUpLine(): Ansi {
        return this
    }

    override fun cursorUpLine(n: Int): Ansi {
        return this
    }

    override fun eraseScreen(): Ansi {
        return this
    }

    override fun eraseScreen(kind: AnsiErase): Ansi {
        return this
    }

    override fun eraseLine(): Ansi {
        return this
    }

    override fun eraseLine(kind: AnsiErase): Ansi {
        return this
    }

    override fun scrollUp(rows: Int): Ansi {
        return this
    }

    override fun scrollDown(rows: Int): Ansi {
        return this
    }

    override fun saveCursorPosition(): Ansi {
        return this
    }

    override fun restoreCursorPosition(): Ansi {
        return this
    }

    override fun reset(): Ansi {
        return this
    }
}
