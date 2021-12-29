package com.utopia.android.ulog.print.format

import com.utopia.android.ulog.tools.UnmapTool


/**
 * des: 对Throwable进行日志输出格式化
 * author 秦王
 * time 2021/11/30 14:11
 */
class ThrowableFormatter(
    // doc: 用于判断首行是否换行，默认是false
    private var isFirstNewline: Boolean = true,
    // doc: 栈的间隔符号，默认是“\t
    private var symbolOne: String = DEFAULT_SYMBOL_ONE,
    private var symbolStart: String = DEFAULT_SYMBOL_START,
    private var symbolBefore: String = DEFAULT_SYMBOL_BEFORE,
    private var symbolAfter: String = DEFAULT_SYMBOL_AFTER
): Formatter<Throwable> {

    private val mResultBuilder by lazy {
        StringBuilder()
    }

    companion object {
        private const val DEFAULT_SYMBOL_ONE = "\t─ "
        private const val DEFAULT_SYMBOL_START = "\t┌☹"
        private const val DEFAULT_SYMBOL_BEFORE = "\t├  "
        private const val DEFAULT_SYMBOL_AFTER = "\t└☺"
    }

    override fun format(data: Throwable): String {
        try {
            return defaultFormat(data)
        } finally {
            mResultBuilder.clear()
        }
    }

    private fun defaultFormat(data: Throwable): String {
        val stackTrace = data.stackTrace
        return when {
            stackTrace.isNullOrEmpty() -> {
                addExceptionInfo(data, symbolOne)
                mResultBuilder.toString()
            }
            else -> {
                addExceptionInfo(data, symbolStart)
                mResultBuilder.append(UnmapTool.getLineSeparator())
                var i = 0
                val stackDeep = stackTrace.size
                while (i < stackDeep) {
                    if (i != stackDeep - 1) {
                        mResultBuilder.append(symbolBefore)
                        mResultBuilder.append(stackTrace[i].toString())
                        mResultBuilder.append(UnmapTool.getLineSeparator())
                    } else {
                        mResultBuilder.append(symbolAfter)
                        mResultBuilder.append(stackTrace[i].toString())
                    }
                    i++
                }
                mResultBuilder.toString()
            }
        }
    }

    private fun addExceptionInfo(data: Throwable, symbol: String) {
        if (isFirstNewline) {
            mResultBuilder.append(UnmapTool.getLineSeparator())
        }
        var i = 0
        mResultBuilder.append(symbol)
        mResultBuilder.append("${data::class.java.name}: ${data.message}")
    }
}