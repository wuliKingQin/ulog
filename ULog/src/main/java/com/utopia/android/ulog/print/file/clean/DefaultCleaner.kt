package com.utopia.android.ulog.print.file.clean

import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.extend.getFileSize
import com.utopia.android.ulog.extend.splitFileNameInDate
import com.utopia.android.ulog.extend.trace
import com.utopia.android.ulog.tools.DateTool
import java.io.File
import java.lang.Exception
import java.util.*

/**
 * des: 实现默认清除文件的类
 * time: 2021/12/26 10:14
 */
class DefaultCleaner: Cleaner {

    // doc: 日期工具类
    private val mDateTool by lazy(LazyThreadSafetyMode.NONE) {
        DateTool()
    }

    override fun onCleanBefore(config: UConfig, cacheDirFile: File): Array<File>? {
        trace("enter")
        var maxLogFolderSize = config.getOnlineConfig()?.getMaxCacheDirSize() ?: -1
        if (maxLogFolderSize <= 0) {
            maxLogFolderSize = config.maxCacheDirSize
        }
        val isCleanLongestFile = cacheDirFile.getFileSize() >= maxLogFolderSize
        trace("isCleanLongestFile=$isCleanLongestFile")
        try {
            return if (isCleanLongestFile) {
                val fileList = cacheDirFile.listFiles()
                if (!fileList.isNullOrEmpty()) {
                    try {
                        sortAndDelete(fileList, cacheDirFile, maxLogFolderSize.toDouble())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        trace("sortOrDelete exception:${e.message}")
                    }
                }
                cacheDirFile.listFiles()
            } else {
                null
            }
        } finally {
            trace("end")
        }
    }

    /**
     * des: 排序并进行删除操作
     * time: 2021/11/19 15:42
     */
    private fun sortAndDelete(fileList: Array<File>, cacheDirFile: File, maxLogFolderSize: Double) {
        trace("enter fileList=${fileList}")
        Arrays.sort(fileList) { file1, file2 ->
            var compare = mDateTool.compareTowDate(
                file1.splitFileNameInDate(),
                file2.splitFileNameInDate()
            )
            if (compare == -2) {
                compare = file1.lastModified().compareTo(file2.lastModified())
            }
            compare
        }
        trace("fileList=${fileList}")
        for (file in fileList) {
            file.delete()
            trace("delete file=${file.name}")
            if (cacheDirFile.getFileSize() < maxLogFolderSize) {
                break
            }
        }
        trace("end")
    }

    override fun shouldClean(config: UConfig, targetFile: File): Boolean {
        var logValidTime = config.getOnlineConfig()?.getLogValidTime() ?: -1
        if (logValidTime <= 0) {
            logValidTime = config.logValidTime
        }
        trace("enter")
        return try {
            val dateStr = targetFile.splitFileNameInDate()
            val diffDay = mDateTool.calculateFewDay(dateStr)
            trace("dateStr=${dateStr}" +
                    " diffDay=${diffDay} logValidTime=${logValidTime}")
            if (diffDay < 0) {
                true
            } else {
                diffDay >= logValidTime
            }
        } finally {
            trace("end")
        }
    }
}