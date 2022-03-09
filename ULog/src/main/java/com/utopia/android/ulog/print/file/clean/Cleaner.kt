package com.utopia.android.ulog.print.file.clean

import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.config.online.ConfigModel
import java.io.File

/**
 * des: 用于抽象出日志文件清除需要实现的条件，默认实现可以@DefaultCleaner
 * author 秦王
 * time 2021/12/25 8:42
 */
interface Cleaner {

    /**
     * des: 检测是否有需要清除的文件
     * time: 2021/11/18 17:11
     */
    fun executeCleanFiles(config: UConfig) {
        val cacheDir = config.cacheLogDir ?: return
        val cacheDirFile = File(cacheDir)
        var files = cacheDirFile.listFiles { file ->
            file.isFile && !file.name.endsWith(".zip")
        }
        if (!checkCleanCondition(files, config)) {
            return
        }
        val result = onCleanBefore(config, cacheDirFile)
        if (!result.isNullOrEmpty()) {
            files = result
        }
        if (!files.isNullOrEmpty()) {
            for (file in files) {
                if (shouldClean(config, file)) {
                    file.delete()
                }
            }
        }
        onCleanAfter(cacheDirFile)
    }

    /**
     * des: 执行清除缓存之前
     * time: 2021/11/19 14:45
     */
    fun onCleanBefore(config: UConfig, cacheDirFile: File): Array<File>? {
        return null
    }

    /**
     * des: 检测是否满足清除条件
     * time: 2022/3/3 11:35
     */
    private fun checkCleanCondition(cacheFiles: Array<out File>?, config: UConfig): Boolean {
        val thrValue = config.getMaxLogFolderSize() / config.getMaxLogFileSize()
        return (cacheFiles?.size ?: 0) >= thrValue
    }

    private fun UConfig?.getMaxLogFolderSize(): Int {
        this ?: return 0
        return getOnlineConfig()?.getMaxCacheDirSize() ?: maxCacheDirSize
    }

    private fun UConfig?.getMaxLogFileSize(): Int {
        this ?: return 0
        return getOnlineConfig()?.getMaxLogFileSize() ?: maxLogFileSize
    }

    /**
     * des: 执行清除缓存后
     * time: 2021/11/19 14:46
     */
    fun onCleanAfter(cacheDirFile: File) {
    }

    /**
     * des: 检测目标文件是否需要被清除
     * time: 2021/11/18 17:11
     */
    fun shouldClean(config: UConfig, targetFile: File): Boolean
}