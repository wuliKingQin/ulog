package com.utopia.android.ulog.print.format

import com.utopia.android.ulog.tools.UnmapTool

/**
 * des: 实现日志信息加上边框的格式化器
 * author 秦王
 * time 2021/12/29 16:41
 */
class BorderFormatter: Formatter<Array<String?>>{

    // doc: 将处理后的结果组装起来
    private val mResultBuilder by lazy {
        StringBuilder()
    }

    // doc: 用于处理边框
    private val mBorderBuilder by lazy {
        StringBuilder()
    }

    companion object {
        private const val VERTICAL_BORDER_CHAR = '║'
        private const val TOP_HORIZONTAL_BORDER = "╔═════════════════════════════════════════════════" +
                "══════════════════════════════════════════════════"
        private const val DIVIDER_HORIZONTAL_BORDER = "╟─────────────────────────────────────────────────" +
                "──────────────────────────────────────────────────"
        private const val BOTTOM_HORIZONTAL_BORDER = "╚═════════════════════════════════════════════════" +
                "══════════════════════════════════════════════════"
    }

    override fun format(data: Array<String?>): String {
        try {
            if (data.isEmpty()) {
                return ""
            }
            val nonNullSegments = data.filter { !it.isNullOrEmpty() }
            val segmentSize = nonNullSegments.size
            if (segmentSize == 0) {
                return ""
            }
            return defaultHandle(nonNullSegments, segmentSize)
        } finally {
            mResultBuilder.clear()
        }
    }

    private fun defaultHandle(segments: List<String?>, segmentSize: Int): String {
        mResultBuilder
            .append("   ")
            .append(UnmapTool.getLineSeparator())
            .append(TOP_HORIZONTAL_BORDER)
            .append(UnmapTool.getLineSeparator())
        var segment: String?
        for (i in 0 until segmentSize) {
            segment = segments[i] ?: continue
            mResultBuilder.append(appendVerticalBorder(segment))
            if (i != segmentSize - 1) {
                mResultBuilder
                    .append(UnmapTool.getLineSeparator())
                    .append(DIVIDER_HORIZONTAL_BORDER)
                    .append(UnmapTool.getLineSeparator())
            } else {
                mResultBuilder.append(UnmapTool.getLineSeparator())
                    .append(BOTTOM_HORIZONTAL_BORDER)
            }
        }
        return mResultBuilder.toString()
    }

    private fun appendVerticalBorder(msg: String): String {
        try {
            val lines = msg.split(UnmapTool.getLineSeparator()).toTypedArray()
            var i = 0
            val lineSize = lines.size
            while (i < lineSize) {
                if (i != 0) {
                    mBorderBuilder.append(UnmapTool.getLineSeparator())
                }
                val line = lines[i]
                mBorderBuilder.append(VERTICAL_BORDER_CHAR)
                    .append(line)
                i++
            }
            return mBorderBuilder.toString()
        } finally {
            mBorderBuilder.clear()
        }
    }
}