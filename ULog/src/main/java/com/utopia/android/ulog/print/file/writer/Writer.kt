package com.utopia.android.ulog.print.file.writer

import com.utopia.android.ulog.print.file.encrypt.Encryptor
import java.io.File

/**
 * des: 抽象出日志信息写入文件的接口，方便实现不同的文件写入方式，
 * 比如传统的io形式，mmap形式等
 * author 秦王
 * time 2021/12/10 19:30
 */
interface Writer {

    // doc: 日志信息写入文件的时候，
    // 将日志信息进行加密处理
    var encryptor: Encryptor?

    /**
     * des: 打开一个指定的文件
     * time: 2021/11/18 9:58
     */
    fun open(file: File): Boolean

    /**
     * des: 用于判断是否已经打开
     * time: 2021/11/18 9:58
     */
    fun isOpened(): Boolean

    /**
     * des: 获取已经打开的文件，没有打开返回null
     * time: 2021/11/18 9:59
     */
    fun getOpenedFile(): File?

    /**
     * des: 获取已经打开的文件名字，没有返回null
     * time: 2021/11/18 9:59
     */
    fun getOpenedFileName(): String?

    /**
     * des: 将日志内容通过该方法写入到文件中
     * time: 2021/11/18 10:00
     */
    fun append(message: String)

    /**
     * des: 关闭已经打开的文件
     * time: 2021/11/18 10:01
     */
    fun close(): Boolean

    /**
     * des: 在该方法中实现新创建的文件，写入文件头部基础信息等工作
     * time: 2021/11/18 10:30
     */
    fun onNewFileCreated(file: File)

    /**
     * des: 用于将数据刷新到文件
     * time: 2021/12/3 14:57
     */
    fun flush() {
    }

    /**
     * des: 获取上次应用退出后，最后一个记录的日子文件
     * time: 2021/12/26 9:36
     */
    fun getLastLogFileName(cacheDir: String?): String? = ""

    /**
     * des: 用于记录上次写入的日志文件名
     * time: 2021/12/26 9:43
     */
    fun recordLastLogFileName(file: File) {
    }

    /**
     * des: 当文件创建完毕后，需要在文件头写入内容，那么该接口就是实现该功能的
     * author 秦王
     * time 2021/11/22 16:04
     */
    interface FileReady {
        /**
         * des: 当文件创建完毕后会执行该方法，在方法中则可以进行文件头部内容的写入操作
         * time: 2021/11/22 16:05
         */
        fun onReady(writer: Writer)
    }
}