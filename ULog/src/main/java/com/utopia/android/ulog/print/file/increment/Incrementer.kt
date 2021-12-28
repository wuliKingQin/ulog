package com.utopia.android.ulog.print.file.increment

import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.print.file.writer.Writer

/**
 * des: 抽象出文件新增的接口，用来可以自定义什么时候进行新建一个日志文件，
 * 默认实现@DefaultIncrementer
 * author 秦王
 * time 2021/12/25 8:48
 */
interface Incrementer {

    companion object {
        // doc: 文件后缀名
        const val FILE_SUFFIX_NAME = ".txt"
        // doc: 用于连接文件名的分割符号
        const val SEPARATOR = "_"
    }

    /**
     * des: 用来生成文件名
     * time: 2021/11/18 15:43
     */
    fun generateFileName(config: UConfig): String?

    /**
     * des: 用于判断是否需要创建一个新的文件
     * time: 2021/12/25 10:35
     */
    fun isCreateFile(config: UConfig, writer: Writer): Boolean

    /**
     * des: 用于是否创建新文件的单独接口
     * author 秦王
     * time 2021/12/25 10:36
     */
    interface Condition {
        /**
         * des: 用于判断是否需要创建一个新的文件
         * time: 2021/12/25 10:35
         */
        fun isCreateFile(config: UConfig, writer: Writer): Boolean
    }

    /**
     * des: 用于单独生成文件名的接口
     * author 秦王
     * time 2021/12/25 10:38
     */
    interface NameGenerator {
        /**
         * des: 用来生成文件名
         * time: 2021/11/18 15:43
         */
        fun generateFileName(config: UConfig): String?
    }
}