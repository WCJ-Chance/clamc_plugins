package com.tencent.bk.devops.atom.task.exception

class PropertyNotExistException(val key: String, message: String) : RuntimeException(message)
