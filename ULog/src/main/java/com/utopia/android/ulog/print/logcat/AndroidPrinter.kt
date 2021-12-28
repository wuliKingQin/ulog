package com.utopia.android.ulog.print.logcat

import android.util.Log
import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.print.Printer
import com.utopia.android.ulog.print.format.JsonFormatter

/**
 * des: 实现在Android的logcat里面打印的日志器
 * author 秦王
 * time 2021/12/10 19:11
 */
class AndroidPrinter: Printer {

    companion object {
        private const val MAX_SHOW_SIZE = 4000
    }

    private val mJsonFormatter by lazy {
        JsonFormatter()
    }

    override fun print(message: UMessage) {
        val tag = getTag(message)
        var content = messageFormat(message.content)
        content = if (message.isWriteToFile) {
            "[${message.tag}] $content"
        } else {
            content
        }
        doPrint(message.level, tag, content)
    }

    /**
     * des: 获取tag
     * time: 2021/12/10 19:27
     */
    private fun getTag(message: UMessage): String {
        return message.tag ?: if (message.type == UMessage.EVENT_FLUSH) {
            "event_flush"
        } else {
            "event"
        }
    }

    private fun messageFormat(message: Any?): String {
        return when(message) {
            is String -> mJsonFormatter.format(message as? String ?: "")
            else -> message.toString()
        }
    }

    private fun doPrint(level: Int, tag: String, message: String) {
        val length = message.length
        var start = 0
        var end = 0
        while (start < length) {
            if (message[start] == '\n') {
                start ++
                continue
            }
            end = (start + MAX_SHOW_SIZE).coerceAtMost(length)
            end = adjustEnd(message, start, end)
            println(level, tag, message.substring(start, end))
            start = end
        }
    }

    private fun adjustEnd(message: String, start: Int, originEnd: Int): Int {
        if (originEnd == message.length) {
            return originEnd
        }
        if (message[originEnd] == '\n') {
            return originEnd
        }
        var last = originEnd - 1
        while (start < last) {
            if (message[last] == '\n') {
                return last
            }
            last--
        }
        return originEnd
    }

    private fun println(level: Int, tag: String, message: String) {
        Log.println(level, tag, message)
    }
}