package com.utopia.android.ulog.print.file.clean

import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.extend.trace
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
        trace("enter")
        val cacheDir = config.cacheLogDir ?: return
        trace("cacheDir=${cacheDir}")
        val cacheDirFile = File(cacheDir)
        var files = cacheDirFile.listFiles { file ->
            file.isFile && !file.name.endsWith(".zip")
        }
        val result = onCleanBefore(config, cacheDirFile)
        if (!result.isNullOrEmpty()) {
            files = result
        }
        if (!files.isNullOrEmpty()) {
            for (file in files) {
                trace("scan fileName=${file.name}")
                if (shouldClean(config, file)) {
                    trace("execute delete fileName=${file.name}")
                    file.delete()
                }
            }
        }
        onCleanAfter(cacheDirFile)
        trace("end")
    }

    /**
     * des: 执行清除缓存之前
     * time: 2021/11/19 14:45
     */
    fun onCleanBefore(config: UConfig, cacheDirFile: File): Array<File>? {
        return null
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