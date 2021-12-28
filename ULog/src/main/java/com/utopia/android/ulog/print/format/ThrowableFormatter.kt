package com.utopia.android.ulog.print.format

import com.utopia.android.ulog.tools.UnmapTool


/**
 * des: 对Throwable进行日志输出格式化
 * author 秦王
 * time 2021/11/30 14:11
 */
class ThrowableFormatter(
    // doc: 栈的间隔符号，默认是“\t
    private var symbolOne: String = DEFAULT_SYMBOL_ONE,
    private var symbolBefore: String = DEFAULT_SYMBOL_BEFORE,
    private var symbolAfter: String = DEFAULT_SYMBOL_AFTER
): Formatter<Throwable>{

    companion object {
        private const val DEFAULT_SYMBOL_ONE = "\t─ "
        private const val DEFAULT_SYMBOL_BEFORE = "\t├ "
        private const val DEFAULT_SYMBOL_AFTER = "\t└ "
    }

    override fun format(data: Throwable): String {
        val stackTrace = data.stackTrace
        val sb = StringBuilder(256)
        return when {
            stackTrace.isNullOrEmpty() -> ""
            stackTrace.size == 1 -> symbolOne + stackTrace[0].toString()
            else -> {
                var i = 0
                val stackDeep: Int = stackTrace.size
                while (i < stackDeep) {
                    if (i != stackDeep - 1) {
                        sb.append(symbolBefore)
                        sb.append(stackTrace[i].toString())
                        sb.append(UnmapTool.getLineSeparator())
                    } else {
                        sb.append(symbolAfter)
                        sb.append(stackTrace[i].toString())
                    }
                    i++
                }
                sb.toString()
            }
        }
    }
}