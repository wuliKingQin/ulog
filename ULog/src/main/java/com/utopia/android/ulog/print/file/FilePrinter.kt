package com.utopia.android.ulog.print.file

import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.extend.getSdCardAvailable
import com.utopia.android.ulog.print.Printer
import com.utopia.android.ulog.print.file.clean.Cleaner
import com.utopia.android.ulog.print.file.clean.DefaultCleaner
import com.utopia.android.ulog.print.file.increment.DefaultIncrementer
import com.utopia.android.ulog.print.file.increment.Incrementer
import com.utopia.android.ulog.print.file.writer.DefaultFileWriter
import com.utopia.android.ulog.print.file.writer.Writer
import com.utopia.android.ulog.print.filter.Filter
import com.utopia.android.ulog.print.format.DefaultOutputFormatter
import com.utopia.android.ulog.print.format.Formatter
import java.io.File
import java.lang.RuntimeException

/**
 * des: 将日志输出到文件的打印器
 * author 秦王
 * time 2021/12/24 9:05
 */
class FilePrinter constructor(): Printer{

    companion object {
        private const val ERROR_DES = "==============" +
                "The storage space is insufficient to write logs" +
                "================"
        private const val THROW_DES = "log cacheDir or newFileName is null"
    }

    // doc: 文件写入器
    private var mWriter: Writer? = null
    // doc: 文件新增器
    private var mIncrementer: Incrementer? = null
    // doc: 文件清理处理器
    private var mCleaner: Cleaner? = null
    // doc: 用于日志信息输出到文件的格式化器
    private var mOutputFormatter: Formatter<UMessage>? = null
    // doc: 日志过滤器
    private var mFilter: Filter? = null
    // doc: 用于判断是否有存够多的存储空间
    private var isEnoughStorage = true

    /**
     * des: 用于从Builder类中通过build函数使用构造函数
     * time: 2021/12/25 8:41
     */
    private constructor(builder: Builder): this() {
        mWriter = builder.writer
        mIncrementer = builder.incrementer
        mCleaner = builder.cleaner
        mOutputFormatter = builder.outputFormatter
        mFilter = builder.filter
    }

    override fun isFilter(message: UMessage): Boolean {
        return mFilter?.isFilter(message) ?: false
    }

    override fun print(message: UMessage) {
        if (message.type == UMessage.EVENT) {
            // doc: 排除事件消息
            return
        }
        val writer = getWriter(message.config)
        if (message.type == UMessage.EVENT_FLUSH) {
            writer.flush()
            return
        }
        val config = message.config ?: return
        val incrementer = getIncrementer()
        val lastFileName = writer.getOpenedFileName()
        val isWriterClosed = !writer.isOpened()
        if (incrementer.isCreateFile(config, writer)) {
            checkStorage(config)
            if (!isEnoughStorage) {
                if (!isWriterClosed) {
                    writer.append(ERROR_DES)
                }
                return
            }
            val cacheDir = config.cacheLogDir
            val newFileName = incrementer.generateFileName(config)
            if (cacheDir.isNullOrEmpty() || newFileName.isNullOrEmpty()) {
                throw RuntimeException(THROW_DES)
            }
            if (newFileName != lastFileName || isWriterClosed) {
                writer.close()
                checkCleanFileIfNeed(config)
                val newFile = File(cacheDir, newFileName)
                writer.recordLastLogFileName(newFile)
                if (!writer.open(newFile)) {
                    return
                }
            }
        }
        val result = getOutputFormatter().format(message)
        writer.append(result)
    }

    /**
     * des: 检测存储空间是否足够，足够则继续
     * time: 2021/11/18 15:05
     */
    private fun checkStorage(config: UConfig) {
        var freeDiskSize = config.getOnlineConfig()?.getRemainingDiskSize() ?: -1
        if (freeDiskSize <= 0) {
            freeDiskSize = config.remainingDiskSize
        }
        isEnoughStorage = freeDiskSize < getSdCardAvailable(1024)
    }

    /**
     * des: 检查是否需要清除文件
     * time: 2021/11/18 15:36
     */
    private fun checkCleanFileIfNeed(config: UConfig) {
        if (mCleaner == null) {
            mCleaner = DefaultCleaner()
        }
        mCleaner?.executeCleanFiles(config)
    }

    /**
     * des: 获取增量器的实力
     * time: 2021/11/19 9:11
     */
    private fun getIncrementer(): Incrementer {
        return if (mIncrementer == null) {
            mIncrementer = DefaultIncrementer()
            mIncrementer!!
        } else {
            mIncrementer!!
        }
    }

    /**
     * des: 获取文件写入器
     * time: 2021/11/19 9:14
     */
    private fun getWriter(config: UConfig?): Writer {
        return if (mWriter == null) {
            mWriter = DefaultFileWriter().apply {
                cacheDir = config?.cacheLogDir
            }
            mWriter!!
        } else {
            mWriter!!
        }
    }

    /**
     * des: 获取日志文件输出格式化器
     * time: 2021/11/19 9:28
     */
    private fun getOutputFormatter(): Formatter<UMessage> {
        return if (mOutputFormatter == null) {
            mOutputFormatter = DefaultOutputFormatter()
            mOutputFormatter!!
        } else {
            mOutputFormatter!!
        }
    }

    /**
     * des: 构建文件打印器
     * author 秦王
     * time 2021/12/25 8:37
     */
    class Builder {

        // doc: 文件写入器
        internal var writer: Writer? = null
        // doc: 文件新增器
        internal var incrementer: Incrementer? = null
        // doc: 文件清理处理器
        internal var cleaner: Cleaner? = null
        // doc: 用于日志信息输出到文件的格式化器
        internal var outputFormatter: Formatter<UMessage>? = null
        // doc: 用于实现自己的过滤器
        internal var filter: Filter? = null

        /**
         * des: 设置自己实现的或者默认实现的写入文件器
         * time: 2021/12/25 8:38
         */
        fun setFileWriter(writer: Writer): Builder {
            this.writer = writer
            return this
        }

        /**
         * des: 设置自己实现的日志文件新增的实现类
         * time: 2021/12/25 10:02
         */
        fun setFileIncrementer(incrementer: Incrementer): Builder{
            this.incrementer = incrementer
            return this
        }

        /**
         * des: 设置自己实现的日志文件清理器
         * time: 2021/12/25 10:04
         */
        fun setFileCleaner(cleaner: Cleaner): Builder {
            this.cleaner = cleaner
            return this
        }

        /**
         * des: 设置自己实现的日志输出到日志文件的格式化器
         * time: 2021/12/25 10:10
         */
        fun setOutputFormatter(formatter: Formatter<UMessage>): Builder {
            this.outputFormatter = formatter
            return this
        }

        /**
         * des: 设置自己的过滤器实现类
         * time: 2021/12/25 10:16
         */
        fun setFilter(filter: Filter): Builder {
            this.filter = filter
            return this
        }

        /**
         * des: 通过该方法构建出一个文件打印器出来
         * time: 2021/12/25 8:39
         */
        fun build(): FilePrinter {
            return FilePrinter(this)
        }
    }
}