package com.utopia.android.ulog

import android.app.Application
import android.content.Context
import android.util.Log
import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.config.online.ConfigModel
import com.utopia.android.ulog.config.online.ConfigJobService
import com.utopia.android.ulog.core.impl.PrintExecutor
import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.print.Printer
import com.utopia.android.ulog.print.file.FilePrinter
import com.utopia.android.ulog.print.file.upload.UploadHandler
import com.utopia.android.ulog.print.file.upload.UploadJobService
import com.utopia.android.ulog.print.file.upload.Uploader
import com.utopia.android.ulog.print.logcat.AndroidPrinter
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicReference

/**
 * des: 封装日志的封装类
 * author 秦王
 * time 2021/12/26 10:49
 */
internal class ULogger {

    // doc: 用于保存日志的配置文件
    var config: UConfig? = null

    // doc: 用于处理日志文件上传的逻辑
    private val mUploadHandler by lazy {
        UploadHandler()
    }

    // doc: 日志信息的分发执行器
    private val mPrintExecutor by lazy {
        PrintExecutor()
    }

    /**
     * des: 初始化日志库
     * time: 2021/12/27 9:08
     */
    fun init(context: Context, config: UConfig) {
        if (context is Application) {
            this.config = config
            ConfigJobService.addService(config.configUpdater)
            UploadJobService.addService(config.fileUploader)
            mPrintExecutor.threadFactoryQueue = config.threadFactoryQueue
            initPrinters(config.printers, config.workers)
            // doc: 清除配置里面的
            (config.printers as? ArrayList)?.clear()
            (config.workers as? ArrayList)?.clear()
        } else {
            throw RuntimeException("该方法必须在Application中进行调用")
        }
    }

    /**
     * des: 添加打引器
     * time: 2021/12/26 10:58
     */
    private fun addPrinter(printer: Printer) {
        mPrintExecutor.getWorkerPool()
            .addWorker(printer)
    }

    /**
     * des: 通过该方法添加的打印器必须要有
     * 一个默认无参数构造函数，否则会添加失败
     * time: 2021/12/26 10:58
     */
    private fun addPrinters(vararg printerClsList: Class<*>) {
        for (printerCls in printerClsList) {
            mPrintExecutor.getWorkerPool()
                .addWorker(printerCls)
        }
    }

    /**
     * des: 处理不同形式的添加打印器的方式，如果有一样的，只保留已经存在的
     * time: 2021/12/26 12:16
     */
    private fun initPrinters(printerCls: List<Class<*>>?, workerList: List<Printer>? = null) {
        val isHasPrinterCls = !printerCls.isNullOrEmpty()
        val isHasWorker = !workerList.isNullOrEmpty()
        when {
            isHasPrinterCls && !isHasWorker -> {
                addPrinters(*printerCls!!.toTypedArray())

            }
            !isHasPrinterCls && isHasWorker -> {
                for (printer in workerList!!) {
                    addPrinter(printer)
                }
            }
            isHasPrinterCls && isHasWorker -> {
                val workers = workerList as ArrayList
                for (worker in workers) {
                    val workerCls = worker.javaClass
                    if (printerCls!!.contains(workerCls)) {
                        (printerCls as ArrayList).remove(workerCls)
                        continue
                    }
                    addPrinter(worker)
                }
                addPrinters(*printerCls!!.toTypedArray())
            }
            else -> {
                // doc: 如果没有自己配置，则添加默认的日志打印器，
                // 包括文件输出打印器和Android控制台打印器
                addPrinters(
                    AndroidPrinter::class.java,
                    FilePrinter::class.java
                )
            }
        }
    }

    /**
     * des: 打印日志
     * time: 2021/11/16 18:14
     */
    fun println(level: Int, tag: String?, message: Any?, otherArgs: Map<String, Any>? = null) {
        try {
            mPrintExecutor.execute(createMessage(level, tag, message, otherArgs))
        } catch (e: Exception) {
        }
    }

    /**
     * des: 只打印log日志到控制台，不输入到文件
     * time: 2021/12/9 9:56
     */
    fun printlnToAndroid(level: Int, tag: String?, message: Any?) {
        try {
            mPrintExecutor.execute(createMessage(level, tag, message).apply {
                isWriteToFile = false
            })
        } catch (e: Exception) {
        }
    }

    /**
     * des: post一条异步消息到日志框架，这条消息可以优先进行处理
     * time: 2021/12/26 13:26
     */
    fun postAsync(message: Any, type: Int = UMessage.EVENT) {
        mPrintExecutor.execute(createMessage(
            Log.INFO,
            if (type == UMessage.EVENT_FLUSH) "event_flush" else "event",
            message
        ).apply {
            this.type = type
        })
    }

    /**
     * des: 获取日志记录对象
     * time: 2021/11/16 18:07
     */
    private fun createMessage(
        level: Int,
        tag: String?,
        message: Any?,
        otherArgs: Map<String, Any>? = null
    ): UMessage {
        val rawMessage = UMessage.obtain().apply {
            this.level = level
            this.tag = tag
            this.content = message
            this.otherArgs = otherArgs
            this.config = this@ULogger.config
            this.time = System.currentTimeMillis()
            this.thread = Thread.currentThread()
        }
        return config?.interceptor?.onIntercept(rawMessage) ?: rawMessage
    }

    /**
     * des: 将日志上传到服务器
     * time: 2021/12/27 12:36
     */
    fun uploadToServer(
        context: Context,
        uploadCondition: Uploader.Condition?,
        isSilentlyUpload: Boolean,
        isCheckUseInfo: Boolean
    ) {
        val configInfo = config ?: return
        mUploadHandler.startUpload(
            context,
            configInfo,
            uploadCondition,
            isSilentlyUpload,
            isCheckUseInfo
        )
    }

    /**
     * des: 设置在线配置实例
     * time: 2021/12/27 19:52
     */
    fun setOnlineConfig(onlineConfig: ConfigModel?) {
        if (config?.onlineConfig == null) {
            config?.onlineConfig = AtomicReference()
        }
        config?.onlineConfig?.set(onlineConfig)
    }
}