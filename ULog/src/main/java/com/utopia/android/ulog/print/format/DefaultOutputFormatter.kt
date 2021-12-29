package com.utopia.android.ulog.print.format

import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.tools.DateTool
import com.utopia.android.ulog.tools.UnmapTool
import java.lang.StringBuilder

/**
 * des: 默认实现的日志输入到文件的格式
 * author 秦王
 * time 2021/11/18 16:07
 */
class DefaultOutputFormatter: Formatter<UMessage> {

    companion object {
        private const val DATE_FORMAT = "yyyy/MM/dd HH:mm:ss SSS"
        private const val BRACKET_LEFT = '['
        private const val BRACKET_RIGHT = ']'
        private const val SEPARATOR = ": "
    }

    private val jsonFormatter by lazy {
        JsonFormatter()
    }

    // doc: 输入文本构建器
    private val outputBuilder by lazy {
        StringBuilder()
    }

    // doc: 异常格式化器
    private val throwableFormatter by lazy {
        ThrowableFormatter(true, "", "", "")
    }

    // doc: 日期格式化工具
    private val mDateTool by lazy {
        DateTool(DATE_FORMAT)
    }

    override fun format(data: UMessage): String {
        return try {
            val message = messageFormat(data.content)
            outputBuilder
                .append(BRACKET_LEFT)
                .append(mDateTool.format(data.time))
                .append(BRACKET_RIGHT)
                .append(BRACKET_LEFT)
                .append(data.tag)
                .append(BRACKET_RIGHT)
                .append(SEPARATOR)
                .append(message)
                .append(UnmapTool.getLineSeparator())
                .append(UnmapTool.getLineSeparator())
                .toString()
        } finally {
            outputBuilder.clear()
        }
    }

    /**
     * des: 消息格式化成字符串
     * time: 2021/11/18 16:29
     */
    private fun messageFormat(message: Any?): String {
        return when(message) {
            is String -> jsonFormatter.format(message)
            is Throwable -> throwableFormatter.format(message)
            else -> message.toString()
        }
    }
}