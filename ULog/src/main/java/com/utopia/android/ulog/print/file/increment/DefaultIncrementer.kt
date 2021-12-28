package com.utopia.android.ulog.print.file.increment

import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.extend.getFileSize
import com.utopia.android.ulog.extend.splitFileNameInDate
import com.utopia.android.ulog.extend.trace
import com.utopia.android.ulog.print.file.writer.Writer
import com.utopia.android.ulog.tools.DateTool
import java.io.File
import java.lang.StringBuilder

/**
 * des: 默认实现的文件新增器的实现类
 * author 秦王
 * time 2021/12/25 10:40
 */
class DefaultIncrementer @JvmOverloads constructor(
    // doc: 文件名生成器
    private var nameGenerator: Incrementer.NameGenerator? = null,
    // doc: 用于判断是否新增文件，单独自己的实现类
    private var condition: Incrementer.Condition? = null
): Incrementer {

    // doc: 用于处理日期的类
    private val mDateTool by lazy(LazyThreadSafetyMode.NONE) {
        DateTool()
    }
    // doc: 文件名构建器
    private val mFileNameBuilder by lazy(LazyThreadSafetyMode.NONE) {
        StringBuilder()
    }

    override fun generateFileName(config: UConfig): String {
        return nameGenerator?.generateFileName(config) ?: generateNewFileName(config)
    }

    /**
     * des: 生成日志的文件名的默认实现
     * time: 2021/12/26 8:50
     */
    private fun generateNewFileName(config: UConfig): String {
        trace("enter")
        try {
            val prefixName = config.logFilePrefixName ?: "utopia"
            mFileNameBuilder.clear()
            val newFileName = mFileNameBuilder.append(prefixName)
                .append(Incrementer.SEPARATOR)
                .append(mDateTool.format(System.currentTimeMillis()))
                .append(Incrementer.FILE_SUFFIX_NAME)
                .toString()
            trace("newFileName=${newFileName}")
            return newFileName
        } finally {
            trace("end")
        }
    }

    override fun isCreateFile(config: UConfig, writer: Writer): Boolean {
        return condition?.isCreateFile(config, writer) ?: false
                || isCreateNewFile(config, writer)
    }

    /**
     * des: 是否创建的新的文件的默认实现
     * time: 2021/12/26 8:50
     */
    private fun isCreateNewFile(config: UConfig, writer: Writer): Boolean {
        trace("end")
        return try {
            val fileName = writer.getOpenedFileName()
            val isWriteClosed = !writer.isOpened()
            var maxLogFileSize = config.getOnlineConfig()?.getMaxLogFileSize() ?: -1
            if (maxLogFileSize <= 0) {
                maxLogFileSize = config.maxLogFileSize
            }
            when {
                fileName.isNullOrEmpty() || isWriteClosed -> {
                    checkLastFileSpace(config.cacheLogDir, maxLogFileSize, writer)
                }
                else -> {
                    val dateStr = writer.getOpenedFile().splitFileNameInDate()
                    val isSameDay = mDateTool.calculateTwoDateSameDay(dateStr)
                    trace("dateStr=${dateStr} isSameDay=$isSameDay")
                    if (isSameDay) {
                        val targetFileSize = writer.getOpenedFile().getFileSize()
                        trace("maxLogFileSize=${maxLogFileSize} targetFileSize=$targetFileSize")
                        targetFileSize >= maxLogFileSize
                    } else {
                        true
                    }
                }
            }
        } finally {
            trace("end")
        }
    }

    /**
     * des: 检查上次写入的文件是否已经满了，没有满的话，会继续写入
     * time: 2021/12/26 9:16
     */
    private fun checkLastFileSpace(
        cacheDir: String?,
        maxLogFileSize: Int,
        writer: Writer
    ): Boolean {
        trace("enter")
        val lastFileName = writer.getLastLogFileName(cacheDir)
        trace("lastFileName=${lastFileName}")
        return lastFileName?.let { lastFilePath ->
            val lastFile = File(lastFilePath)
            if (!lastFile.exists()) {
                trace("lastFileName=${lastFile.path} is not exists")
                true
            } else {
                val lastFileSize = lastFile.getFileSize()
                trace("lastFileSize=${lastFileSize} maxLogFileSize=${maxLogFileSize}")
                var isNewCreateFile = lastFileSize >= maxLogFileSize
                if (!isNewCreateFile) {
                    val dateStr = lastFile.splitFileNameInDate()
                    val isSameDay = mDateTool.calculateTwoDateSameDay(dateStr)
                    trace("dateStr=$dateStr isSameDay=${isSameDay}")
                    isNewCreateFile = if (isSameDay) {
                        !writer.open(lastFile)
                    } else {
                        !isSameDay
                    }
                }
                isNewCreateFile
            }
        } ?: true
    }
}