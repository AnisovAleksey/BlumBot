package com.blum.bot

import org.slf4j.LoggerFactory

class Logger(private val clientName: String) {
    private val internalLogger = LoggerFactory.getLogger(this.javaClass)

    fun log(message: String)
        = internalLogger.info("[$clientName] $message")

    fun error(message: String, e: Exception)
        = internalLogger.error("[$clientName] $message", e)

    companion object {
        var threadLocal: ThreadLocal<Logger> = ThreadLocal()

        fun log(message: String)
            = threadLocal.get().log(message)

        fun error(message: String, e: Exception)
            = threadLocal.get().error(message, e)
    }
}