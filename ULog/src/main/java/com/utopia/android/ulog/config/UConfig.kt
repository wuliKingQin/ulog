package com.utopia.android.ulog.config

import com.utopia.android.ulog.config.online.ConfigModel
import com.utopia.android.ulog.config.online.ConfigUpdater
import com.utopia.android.ulog.core.impl.ThreadFactoryQueue
import com.utopia.android.ulog.print.Printer
import com.utopia.android.ulog.print.file.upload.Uploader
import com.utopia.android.ulog.print.interceptor.Interceptor
import java.util.concurrent.atomic.AtomicReference

/**
 * des: 日志库的配置类
 * author 秦王
 * time 2021/12/25 11:00
 */
data class UConfig @JvmOverloads constructor(
    // doc: 日志是否打印的开关
    internal var isDebug: Boolean = true,
    /// doc: 用于判断是否写入文件
    internal var isWriteFile: Boolean = true,
    // doc: 日志缓存的目录
    internal var cacheLogDir: String? = null,
    // doc: 日志文件前缀名，通常使用app的包名
    internal var logFilePrefixName: String? = null,
    // doc: 打印器
    internal var printers: List<Class<*>>? = null,
    // doc: 文件上传器
    internal var fileUploader: Uploader? = null,
    // doc: 日志有效期，超过有效期删除，单位:天
    internal var logValidTime: Int = 3,
    // doc: 单个日志文件的大小
    internal var maxLogFileSize: Int = 1,
    // doc: 日志文件夹≥20M，删除最早创建的日志文件
    internal var maxCacheDirSize: Int = 20,
    // doc: 磁盘可用空余≤10M，不再写入日志
    internal var remainingDiskSize: Int = 10,
    // doc: 添加已经new出来的打印器
    internal var workers: List<Printer>? = null,
    // doc: 提供使用接入方的线程工程来进行创建线程
    internal var threadFactoryQueue: ThreadFactoryQueue? = null,
    // doc: 线上配置自己实现的更新器
    internal var configUpdater: ConfigUpdater? = null,
    // doc: 日志拦截器
    internal var interceptor: Interceptor? = null
) {

    // doc: 获取在线配置文件对象，因为这个地方可能在其他线程进行修改
    internal var onlineConfig: AtomicReference<ConfigModel>? = null

    /**
     * des: 获取在线配置信息
     * time: 2021/12/26 9:06
     */
    fun getOnlineConfig(): ConfigModel? {
        if (onlineConfig?.get() == null) {
            configUpdater?.getLastConfigModel()?.apply {
                // doc: 如果有历史配置，则加载历史配置信息
                if (onlineConfig == null) {
                    onlineConfig = AtomicReference()
                }
                onlineConfig?.set(this)
            }
        }
        return onlineConfig?.get()
    }

    /**
     * des: 用于Builder进行实例化配置的构造器
     * time: 2021/12/25 11:25
     */
    private constructor(builder: Builder): this() {
        isDebug = builder.isDebug
        isWriteFile = builder.isWriteFile
        cacheLogDir = builder.cacheLogDir
        printers = builder.printers
        fileUploader = builder.fileUploader
        logValidTime = builder.logValidTime
        maxLogFileSize = builder.maxLogFileSize
        maxCacheDirSize = builder.maxCacheDirSize
        remainingDiskSize = builder.remainingDiskSize
        logFilePrefixName = builder.logFilePrefixName
        workers = builder.workers
        threadFactoryQueue = builder.threadFactoryQueue
        configUpdater = builder.configUpdater
        interceptor = builder.interceptor
    }

    /**
     * des: 配置文件的构建器
     * author 秦王
     * time 2021/12/25 11:01
     */
    class Builder {

        // doc: 日志是否打印的开关
        internal var isDebug: Boolean = true
        /// doc: 用于判断是否写入文件
        internal var isWriteFile: Boolean = true
        // doc: 日志缓存的目录
        internal var cacheLogDir: String? = null
        // doc: 日志文件前缀名，通常使用app的包名
        internal var logFilePrefixName: String? = null
        // doc: 打印器
        internal var printers: List<Class<*>>? = null
        // doc: 文件上传器
        internal var fileUploader: Uploader? = null
        // doc: 日志有效期，超过有效期删除，单位:天
        internal var logValidTime: Int = 3
        // doc: 单个日志文件的大小
        internal var maxLogFileSize: Int = 1
        // doc: 日志文件夹≥20M，删除最早创建的日志文件
        internal var maxCacheDirSize: Int = 20
        // doc: 磁盘可用空余≤10M，不再写入日志
        internal var remainingDiskSize: Int = 10
        // doc: 添加已经new出来的打印器
        internal var workers: List<Printer>? = null
        // doc: 提供使用接入方的线程工程来进行创建线程
        internal var threadFactoryQueue: ThreadFactoryQueue? = null
        // doc: 线上配置自己实现的更新器
        internal var configUpdater: ConfigUpdater? = null
        // doc: 日志拦截器
        internal var interceptor: Interceptor? = null

        /**
         * des: 设置debug开关
         * time: 2021/12/25 11:10
         */
        fun setDebug(isDebug: Boolean): Builder {
            this.isDebug = isDebug
            return this
        }

        /**
         * des: 设置是否需要将日志写入文件
         * time: 2021/12/25 11:10
         */
        fun setWriteFile(isWriteFile: Boolean): Builder {
            this.isWriteFile = isWriteFile
            return this
        }

        /**
         * des: 添加已经创建出来的打印器，它和printers不会产生冲突，
         * 如果有，只会留已经new出来的
         * time: 2021/12/26 12:01
         */
        fun addPrinter(printer: Printer): Builder {
            if (workers == null) {
                workers = ArrayList()
            }
            (workers as ArrayList).add(printer)
            return this
        }

        /**
         * des: 设置日志信息缓存到文件的目录
         * author 秦王
         * time 2021/12/25 11:11
         */
        fun setCacheLogDir(cacheLogDir: String): Builder {
            this.cacheLogDir = cacheLogDir
            return this
        }

        /**
         * des: 设置文件上传器
         * time: 2021/12/27 12:28
         */
        fun setFileUploader(fileUploader: Uploader): Builder {
            this.fileUploader = fileUploader
            return this
        }

        /**
         * des: 设置日志文件的前缀名，通常是包名
         * time: 2021/12/26 9:00
         */
        fun setLogFilePrefixName(logFilePrefixName: String): Builder {
            this.logFilePrefixName = logFilePrefixName
            return this
        }

        /**
         * des: 添加打印器
         * time: 2021/12/25 11:17
         */
        fun addPrinters(vararg printers: Class<*>): Builder {
            this.printers = printers.toList()
            return this
        }

        /**
         * des: 设置日志的有效缓存时间
         * time: 2021/12/25 11:19
         */
        fun setLogValidTime(logValidTime: Int): Builder {
            this.logValidTime = logValidTime
            return this
        }

        /**
         * des: 设置单个日志文件的最大值
         * time: 2021/12/25 11:21
         */
        fun setMaxLogFileSize(maxLogFileSize: Int): Builder {
            this.maxLogFileSize = maxLogFileSize
            return this
        }

        /**
         * des: 设置日志缓存的目录的最大值
         * author 秦王
         * time 2021/12/25 11:22
         */
        fun setMaxCacheDirSize(maxCacheDirSize: Int): Builder {
            this.maxCacheDirSize = maxCacheDirSize
            return this
        }

        /**
         * des: 设置剩余磁盘空间大小，表示不再将日志写入文件
         * time: 2021/12/25 11:22
         */
        fun setRemainingDiskSize(remainingDiskSize: Int): Builder {
            this.remainingDiskSize = remainingDiskSize
            return this
        }
        /**
         * des: 用于创建线程的线程工厂
         * time: 2021/12/27 9:36
         */
        fun setThreadFactory(threadFactoryQueue: ThreadFactoryQueue): Builder {
            this.threadFactoryQueue = threadFactoryQueue
            return this
        }

        /**
         * des: 设置线上配置自己实现的配置更新器
         * time: 2021/12/27 18:57
         */
        fun setConfigUpdater(configUpdater: ConfigUpdater): Builder {
            this.configUpdater = configUpdater
            return this
        }

        /**
         * des: 设置日志拦截器
         * time: 2021/12/29 15:32
         */
        fun setInterceptor(interceptor: Interceptor?): Builder {
            this.interceptor = interceptor
            return this
        }

        /**
         * des: 通过该方法构建出配置文件的实例
         * time: 2021/12/25 11:02
         */
        fun build(): UConfig {
            return UConfig(this)
        }
    }
}