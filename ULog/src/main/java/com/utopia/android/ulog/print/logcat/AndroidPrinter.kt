package com.utopia.android.ulog.print.logcat

import android.util.Log
import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.print.Printer
import com.utopia.android.ulog.print.filter.Filter
import com.utopia.android.ulog.print.format.*

/**
 * des: 实现在Android的logcat里面打印的日志器
 * author 秦王
 * time 2021/12/10 19:11
 */
class AndroidPrinter @JvmOverloads constructor(
    // doc: 日志过滤器
    private var filter: Filter? = null,
    // doc: 输出到Logcat的日志格式器
    private var outputFormatter: Formatter<UMessage>? = null
): Printer {

    companion object {
        private const val MAX_SHOW_SIZE = 4000
    }

    override fun isFilter(message: UMessage): Boolean {
        return filter?.isFilter(message) ?: !(message.config?.isDebug ?: true)
    }

    override fun print(message: UMessage) {
        val tag = getTag(message)
        var content = messageFormat(message)
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

    private fun messageFormat(message: UMessage): String {
        if (outputFormatter == null) {
            outputFormatter = DefaultLogcatOutputFormatter()
        }
        return outputFormatter?.format(message) ?: message.content.toString()
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

    /**
     * des: 输出到Logcat的日志格式器的实现类
     * author 秦王
     * time 2021/12/29 15:48
     */
    class DefaultLogcatOutputFormatter: Formatter<UMessage> {

        // doc: 对json字符串进行的格式化
        private val mJsonFormatter by lazy {
            JsonFormatter()
        }

        // doc: 对异常信息的格式化器
        private val mThrowableFormatter by lazy {
            ThrowableFormatter(false)
        }

        // doc: 线程格式化器
        private val mThreadFormatter by lazy {
            ThreadFormatter()
        }

        // doc: 日志消息添加边框格式化器
        private val mBorderFormatter by lazy {
            BorderFormatter()
        }

        override fun format(data: UMessage): String {
            return when(val content = data.content) {
                is String -> mJsonFormatter.format(content as? String ?: "")
                is Throwable -> {
                    val threadInfo = mThreadFormatter.format(data.thread)
                    val stackTraceInfo = mThrowableFormatter.format(content)
                    mBorderFormatter.format(arrayOf(threadInfo, stackTraceInfo))
                }
                else -> content.toString()
            }
        }
    }
}